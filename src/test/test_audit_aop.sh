#!/usr/bin/env bash
# ============================================================
# AOP 감사 로깅 + MDC + Actuator 메트릭 통합 검증 스크립트
#
# 사용법:
#   ./test_audit_aop.sh                  # audit.log 경로 자동 탐색
#   ./test_audit_aop.sh /path/to/audit.log   # 경로 직접 지정
# ============================================================

BASE_URL="http://localhost:8080"
COOKIE=$(mktemp)
PASS_COUNT=0
FAIL_COUNT=0

command -v jq >/dev/null 2>&1 || { echo "jq가 필요합니다."; exit 1; }
command -v awk >/dev/null 2>&1 || { echo "awk가 필요합니다 (대부분 환경에 기본 설치되어 있습니다)."; exit 1; }

# ---------- audit.log 경로 결정 ----------
if [ -n "$1" ]; then
  AUDIT_LOG="$1"
else
  CANDIDATES=(
    "logs/audit.log"
    "../logs/audit.log"
    "./target/logs/audit.log"
    "./build/logs/audit.log"
  )
  AUDIT_LOG=""
  for c in "${CANDIDATES[@]}"; do
    if [ -f "$c" ]; then
      AUDIT_LOG="$c"
      break
    fi
  done

  if [ -z "$AUDIT_LOG" ]; then
    echo "audit.log를 자동으로 찾지 못했습니다. 아래 중 하나로 해결하세요:"
    echo "  1) 서버를 기동한 프로젝트 루트에서 이 스크립트를 실행"
    echo "  2) 경로를 직접 지정: ./test_audit_aop.sh /path/to/audit.log"
    echo ""
    echo "참고 - 서버 프로세스가 실제로 로그를 쓰고 있는 위치 찾는 법:"
    echo "  find / -name 'audit.log' 2>/dev/null"
    exit 1
  fi
fi

echo "사용할 audit.log 경로: $AUDIT_LOG"

assert_true() {
  local desc="$1" cond="$2"
  if [ "$cond" == "true" ]; then
    echo "  [PASS] $desc"
    PASS_COUNT=$((PASS_COUNT+1))
  else
    echo "  [FAIL] $desc"
    FAIL_COUNT=$((FAIL_COUNT+1))
  fi
}

assert_gte() {
  # bc + (( )) 조합은 bash 버전/로케일에 따라 부동소수점 비교가 어긋날 수 있어
  # awk로 비교한다 (거의 모든 환경에 기본 설치되어 있고 부동소수점 처리가 안정적).
  local desc="$1" actual="$2" min="$3"
  if [ -z "$actual" ] || [ "$actual" == "null" ]; then
    echo "  [FAIL] $desc (expected >= $min, actual=$actual)"
    FAIL_COUNT=$((FAIL_COUNT+1))
    return
  fi
  local ok
  ok=$(awk -v a="$actual" -v m="$min" 'BEGIN{print (a>=m)?1:0}')
  if [ "$ok" == "1" ]; then
    echo "  [PASS] $desc (value=$actual)"
    PASS_COUNT=$((PASS_COUNT+1))
  else
    echo "  [FAIL] $desc (expected >= $min, actual=$actual)"
    FAIL_COUNT=$((FAIL_COUNT+1))
  fi
}

echo "============================================================"
echo " AOP 감사 로깅 + MDC + Actuator 메트릭 통합 검증 시작"
echo "============================================================"

before_lines=0
[ -f "$AUDIT_LOG" ] && before_lines=$(wc -l < "$AUDIT_LOG")

# ---------- 사전 준비: 로그인 ----------
echo ""
echo "[준비] skala01 로그인"
curl -s -c "$COOKIE" -X POST "$BASE_URL/api/customers/login" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"skala01","customerPassword":"pw1234"}' > /dev/null

# ---------- 1. 정상 주문(1회차) -> SUCCESS 로그 + reqId 확보 ----------
echo ""
echo "[1] 정상 주문(1회차) - SUCCESS 로그 생성 확인"
curl -s -b "$COOKIE" -X POST "$BASE_URL/api/customers/order" \
  -H "Content-Type: application/json" \
  -d '{"productId":5,"quantity":1}' > /dev/null
sleep 1

found=$(grep -c "method=order.*status=SUCCESS" "$AUDIT_LOG" 2>/dev/null || echo 0)
[ "$found" -ge 1 ] && assert_true "audit.log에 order SUCCESS 라인 존재" "true" || assert_true "audit.log에 order SUCCESS 라인 존재" "false"

reqid_1=$(grep "method=order.*status=SUCCESS" "$AUDIT_LOG" | tail -1 | grep -oE 'reqId=[a-f0-9]+' | cut -d= -f2)

# ---------- 2. 정상 취소 -> SUCCESS 로그 확인 ----------
echo ""
echo "[2] 정상 취소 - SUCCESS 로그 생성 확인"
curl -s -b "$COOKIE" -X POST "$BASE_URL/api/customers/cancel" \
  -H "Content-Type: application/json" \
  -d '{"productId":5,"quantity":1}' > /dev/null
sleep 1

found=$(grep -c "method=cancel.*status=SUCCESS" "$AUDIT_LOG" 2>/dev/null || echo 0)
[ "$found" -ge 1 ] && assert_true "audit.log에 cancel SUCCESS 라인 존재" "true" || assert_true "audit.log에 cancel SUCCESS 라인 존재" "false"

# ---------- 3. 정상 주문(2회차) -> reqId 재확보 (1회차와 다른지 비교용) ----------
echo ""
echo "[3] 정상 주문(2회차) - reqId 고유성 비교용 재주문"
curl -s -b "$COOKIE" -X POST "$BASE_URL/api/customers/order" \
  -H "Content-Type: application/json" \
  -d '{"productId":5,"quantity":1}' > /dev/null
sleep 1

reqid_2=$(grep "method=order.*status=SUCCESS" "$AUDIT_LOG" | tail -1 | grep -oE 'reqId=[a-f0-9]+' | cut -d= -f2)
if [ -n "$reqid_1" ] && [ -n "$reqid_2" ] && [ "$reqid_1" != "$reqid_2" ]; then
  assert_true "1회차/2회차 요청의 reqId가 서로 다름 (고유성) [$reqid_1 vs $reqid_2]" "true"
else
  assert_true "1회차/2회차 요청의 reqId가 서로 다름 (고유성) [$reqid_1 vs $reqid_2]" "false"
fi

# ---------- 4. 포인트 부족 주문 -> REJECTED 로그 확인 ----------
echo ""
echo "[4] 포인트 부족 주문 - REJECTED 로그 생성 확인"
cookie3=$(mktemp)
curl -s -c "$cookie3" -X POST "$BASE_URL/api/customers/login" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"skala03","customerPassword":"pw1234"}' > /dev/null

curl -s -b "$cookie3" -X POST "$BASE_URL/api/customers/order" \
  -H "Content-Type: application/json" \
  -d '{"productId":4,"quantity":1}' > /dev/null
sleep 1

found=$(grep -c "method=order.*status=REJECTED.*errorCode=INSUFFICIENT_FUNDS" "$AUDIT_LOG" 2>/dev/null || echo 0)
[ "$found" -ge 1 ] && assert_true "audit.log에 REJECTED(INSUFFICIENT_FUNDS) 라인 존재" "true" || assert_true "audit.log에 REJECTED(INSUFFICIENT_FUNDS) 라인 존재" "false"

rejected_reqid=$(grep "method=order.*status=REJECTED" "$AUDIT_LOG" | tail -1 | grep -oE 'reqId=[a-f0-9]+' | cut -d= -f2)
if [ -n "$rejected_reqid" ]; then
  assert_true "REJECTED 라인에도 유효한 reqId 존재 [$rejected_reqid]" "true"
else
  assert_true "REJECTED 라인에도 유효한 reqId 존재" "false"
fi
rm -f "$cookie3"

# ---------- 5. audit.log 파일 갱신 여부 ----------
echo ""
echo "[5] audit.log 파일이 이번 실행으로 갱신되었는지 확인"
after_lines=$(wc -l < "$AUDIT_LOG" 2>/dev/null || echo 0)
if [ "$after_lines" -gt "$before_lines" ]; then
  assert_true "실행 전($before_lines줄) 대비 실행 후($after_lines줄) 증가" "true"
else
  assert_true "실행 전($before_lines줄) 대비 실행 후($after_lines줄) 증가" "false"
fi

# ---------- 6. Actuator 메트릭 - order.service.duration 노출 확인 ----------
echo ""
echo "[6] Actuator 메트릭 조회 - order.service.duration"
metric=$(curl -s "$BASE_URL/actuator/metrics/order.service.duration")
count=$(echo "$metric" | jq -r '.measurements[] | select(.statistic=="COUNT") | .value' 2>/dev/null)
assert_gte "COUNT 통계 존재 및 1 이상" "$count" "1"

max=$(echo "$metric" | jq -r '.measurements[] | select(.statistic=="MAX") | .value' 2>/dev/null)
assert_gte "MAX 통계 존재 (실행시간 0 이상)" "$max" "0"

# ---------- 7. Actuator 메트릭 - method/result 태그 필터링 확인 ----------
echo ""
echo "[7] Actuator 메트릭 - method=order, result=SUCCESS 태그 필터"
tagged=$(curl -s "$BASE_URL/actuator/metrics/order.service.duration?tag=method:order&tag=result:SUCCESS")
tagged_count=$(echo "$tagged" | jq -r '.measurements[] | select(.statistic=="COUNT") | .value' 2>/dev/null)
assert_gte "method=order,result=SUCCESS 태그로 필터링된 COUNT 존재" "$tagged_count" "1"

echo ""
echo "[8] Actuator 메트릭 - method=order, result=REJECTED 태그 필터"
tagged_rejected=$(curl -s "$BASE_URL/actuator/metrics/order.service.duration?tag=method:order&tag=result:REJECTED")
tagged_rejected_count=$(echo "$tagged_rejected" | jq -r '.measurements[] | select(.statistic=="COUNT") | .value' 2>/dev/null)
assert_gte "method=order,result=REJECTED 태그로 필터링된 COUNT 존재" "$tagged_rejected_count" "1"

# ---------- 결과 요약 ----------
echo ""
echo "============================================================"
echo " 결과: PASS $PASS_COUNT / FAIL $FAIL_COUNT"
echo "============================================================"
echo ""
echo "참고: audit.log 최근 6줄"
tail -6 "$AUDIT_LOG" 2>/dev/null

rm -f "$COOKIE"

if [ "$FAIL_COUNT" -gt 0 ]; then
  exit 1
fi
exit 0