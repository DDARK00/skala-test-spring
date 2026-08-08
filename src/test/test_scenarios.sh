#!/usr/bin/env bash
# ============================================================
# SKALA-SHOP API 통합 테스트 스크립트 v2
# 실행 전제: data.sql이 반영된 상태로 서버가 기동 중 (localhost:8080)
# 사용법: ./test_scenarios.sh
# v2 변경점: 재고 차감/복구/부족 시나리오(13~16) 추가
# ============================================================

BASE_URL="http://localhost:8080"
COOKIE_DIR=$(mktemp -d)
PASS_COUNT=0
FAIL_COUNT=0

command -v jq >/dev/null 2>&1 || { echo "jq가 필요합니다. brew install jq 또는 apt install jq로 설치하세요."; exit 1; }

# ---------- 유틸 함수 ----------

assert_status() {
  local desc="$1" actual="$2" expected="$3"
  if [ "$actual" == "$expected" ]; then
    echo "  [PASS] $desc (status=$actual)"
    PASS_COUNT=$((PASS_COUNT+1))
  else
    echo "  [FAIL] $desc (expected status=$expected, actual=$actual)"
    FAIL_COUNT=$((FAIL_COUNT+1))
  fi
}

assert_eq() {
  local desc="$1" actual="$2" expected="$3"
  if [ "$actual" == "$expected" ]; then
    echo "  [PASS] $desc (value=$actual)"
    PASS_COUNT=$((PASS_COUNT+1))
  else
    echo "  [FAIL] $desc (expected=$expected, actual=$actual)"
    FAIL_COUNT=$((FAIL_COUNT+1))
  fi
}

request() {
  local method="$1" path="$2" cookie="$3" body="$4"
  local cookie_opt=""
  [ "$cookie" != "-" ] && cookie_opt="-b $cookie -c $cookie"

  if [ "$body" != "-" ]; then
    resp=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$path" \
      -H "Content-Type: application/json" $cookie_opt -d "$body")
  else
    resp=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$path" $cookie_opt)
  fi

  status=$(echo "$resp" | tail -n1)
  respBody=$(echo "$resp" | sed '$d')
  echo "$status"
  echo "$respBody"
}

echo "============================================================"
echo " SKALA-SHOP 통합 테스트 시작"
echo "============================================================"

# ---------- 1. 신규 회원가입 (point 미지정 -> 기본 정책값) ----------
echo ""
echo "[1] 신규 회원가입 - 기본 포인트 정책 적용"
out=$(request POST /api/customers - '{"customerId":"skala99","customerPassword":"pw1234","customerName":"신규고객"}')
status=$(echo "$out" | sed -n 1p); body=$(echo "$out" | sed -n 2p)
assert_status "회원가입 200 OK" "$status" "200"
point=$(echo "$body" | jq -r '.point')
assert_eq "기본 정책 포인트(10000) 적용" "$point" "10000"

# ---------- 2. skala01 로그인 ----------
echo ""
echo "[2] skala01 로그인"
cookie1="$COOKIE_DIR/skala01.txt"
out=$(request POST /api/customers/login "$cookie1" '{"customerId":"skala01","customerPassword":"pw1234"}')
status=$(echo "$out" | sed -n 1p)
assert_status "로그인 200 OK" "$status" "200"
if [ -s "$cookie1" ] && grep -q "bff-access" "$cookie1"; then
  echo "  [PASS] Cookie(bff-access) 발급 확인"
  PASS_COUNT=$((PASS_COUNT+1))
else
  echo "  [FAIL] Cookie(bff-access) 발급 안 됨"
  FAIL_COUNT=$((FAIL_COUNT+1))
fi

# ---------- 3. 상품 목록 조회 (인증 없이) ----------
echo ""
echo "[3] 상품 목록 조회 - 인증 불필요 확인"
out=$(request GET /api/products - -)
status=$(echo "$out" | sed -n 1p)
assert_status "인증 없이 상품 목록 조회 200 OK" "$status" "200"

# ---------- 4. 인증 없이 주문 시도 -> 401 ----------
echo ""
echo "[4] 인증 없이 주문 시도"
out=$(request POST /api/customers/order - '{"productId":3,"quantity":1}')
status=$(echo "$out" | sed -n 1p)
assert_status "미인증 주문 401" "$status" "401"

# ---------- 5. skala01 재주문 (product 1, 기존 보유 2개 -> +2 = 4개) ----------
echo ""
echo "[5] skala01 재주문 - 수량 누적 확인"
out=$(request POST /api/customers/order "$cookie1" '{"productId":1,"quantity":2}')
status=$(echo "$out" | sed -n 1p); body=$(echo "$out" | sed -n 2p)
assert_status "주문 200 OK" "$status" "200"
remain=$(echo "$body" | jq -r '.remainingPoint')
assert_eq "포인트 차감 (1000000-30000=970000)" "$remain" "970000"

# ---------- 6. 내 정보 상세 조회 (누적 수량 확인) ----------
echo ""
echo "[6] 내 정보 조회 - 누적 수량 4개 확인"
out=$(request GET /api/customers/skala01 "$cookie1" -)
status=$(echo "$out" | sed -n 1p); body=$(echo "$out" | sed -n 2p)
assert_status "상세 조회 200 OK" "$status" "200"
qty=$(echo "$body" | jq -r '.products[] | select(.productId==1) | .quantity')
assert_eq "product 1 수량 누적(4)" "$qty" "4"

# ---------- 7. 신규 상품 주문 (product 4, 처음 주문) ----------
echo ""
echo "[7] 신규 상품 주문 (공기청정기)"
out=$(request POST /api/customers/order "$cookie1" '{"productId":4,"quantity":1}')
status=$(echo "$out" | sed -n 1p); body=$(echo "$out" | sed -n 2p)
assert_status "주문 200 OK" "$status" "200"
remain=$(echo "$body" | jq -r '.remainingPoint')
assert_eq "포인트 차감 (970000-129000=841000)" "$remain" "841000"

# ---------- 8. 일부 취소 (product 1, 4개 중 1개 취소 -> 3개) ----------
echo ""
echo "[8] 일부 취소 - product 1"
out=$(request POST /api/customers/cancel "$cookie1" '{"productId":1,"quantity":1}')
status=$(echo "$out" | sed -n 1p); body=$(echo "$out" | sed -n 2p)
assert_status "취소 200 OK" "$status" "200"
remain=$(echo "$body" | jq -r '.remainingPoint')
assert_eq "포인트 환급 (841000+15000=856000)" "$remain" "856000"

out=$(request GET /api/customers/skala01 "$cookie1" -)
body=$(echo "$out" | sed -n 2p)
qty=$(echo "$body" | jq -r '.products[] | select(.productId==1) | .quantity')
assert_eq "product 1 수량 차감 확인(3)" "$qty" "3"

# ---------- 9. 전량 취소 (product 4, 1개 전부 취소 -> row 삭제) ----------
echo ""
echo "[9] 전량 취소 - product 4 (row 삭제 확인)"
out=$(request POST /api/customers/cancel "$cookie1" '{"productId":4,"quantity":1}')
status=$(echo "$out" | sed -n 1p); body=$(echo "$out" | sed -n 2p)
assert_status "취소 200 OK" "$status" "200"
remain=$(echo "$body" | jq -r '.remainingPoint')
assert_eq "포인트 환급 (856000+129000=985000)" "$remain" "985000"

out=$(request GET /api/customers/skala01 "$cookie1" -)
body=$(echo "$out" | sed -n 2p)
exists=$(echo "$body" | jq -r '.products[] | select(.productId==4)')
if [ -z "$exists" ]; then
  echo "  [PASS] product 4 보유 목록에서 삭제 확인"
  PASS_COUNT=$((PASS_COUNT+1))
else
  echo "  [FAIL] product 4가 여전히 남아있음"
  FAIL_COUNT=$((FAIL_COUNT+1))
fi

# ---------- 10. 포인트 부족 (skala03, 보유 10000) ----------
echo ""
echo "[10] 포인트 부족 예외 - skala03"
cookie3="$COOKIE_DIR/skala03.txt"
request POST /api/customers/login "$cookie3" '{"customerId":"skala03","customerPassword":"pw1234"}' > /dev/null

out=$(request POST /api/customers/order "$cookie3" '{"productId":4,"quantity":1}')
status=$(echo "$out" | sed -n 1p); body=$(echo "$out" | sed -n 2p)
assert_status "포인트 부족 409 Conflict" "$status" "409"
errorCode=$(echo "$body" | jq -r '.errorCode')
assert_eq "errorCode = INSUFFICIENT_FUNDS" "$errorCode" "INSUFFICIENT_FUNDS"

# ---------- 11. 없는 상품 조회 ----------
echo ""
echo "[11] 없는 상품 조회"
out=$(request GET /api/products/9999 - -)
status=$(echo "$out" | sed -n 1p); body=$(echo "$out" | sed -n 2p)
assert_status "없는 상품 404 Not Found" "$status" "404"
errorCode=$(echo "$body" | jq -r '.errorCode')
assert_eq "errorCode = DATA_NOT_FOUND" "$errorCode" "DATA_NOT_FOUND"

# ---------- 12. 필수값 누락 (Validation) ----------
echo ""
echo "[12] 필수값 누락 - productId 없이 주문"
out=$(request POST /api/customers/order "$cookie1" '{"quantity":1}')
status=$(echo "$out" | sed -n 1p); body=$(echo "$out" | sed -n 2p)
assert_status "필수값 누락 400 Bad Request" "$status" "400"
errorCode=$(echo "$body" | jq -r '.errorCode')
assert_eq "errorCode = PARAMETER_EXCEPTION" "$errorCode" "PARAMETER_EXCEPTION"

# ============================================================
# 13~16: 재고 반영 시나리오 (product 5 - 가습기, 초기 재고 60, 다른 시나리오 미접촉)
# ============================================================

# ---------- 13. 주문 전 재고 확인 ----------
echo ""
echo "[13] 주문 전 재고 확인 - product 5"
out=$(request GET /api/products/5 - -)
status=$(echo "$out" | sed -n 1p); body=$(echo "$out" | sed -n 2p)
assert_status "상품 조회 200 OK" "$status" "200"
stock=$(echo "$body" | jq -r '.stock')
assert_eq "초기 재고 60" "$stock" "60"

# ---------- 14. 주문 시 재고 차감 확인 ----------
echo ""
echo "[14] 주문 시 재고 차감 - product 5, 10개 주문"
out=$(request POST /api/customers/order "$cookie1" '{"productId":5,"quantity":10}')
status=$(echo "$out" | sed -n 1p)
assert_status "주문 200 OK" "$status" "200"

out=$(request GET /api/products/5 - -)
body=$(echo "$out" | sed -n 2p)
stock=$(echo "$body" | jq -r '.stock')
assert_eq "재고 차감 확인 (60-10=50)" "$stock" "50"

# ---------- 15. 취소 시 재고 복구 확인 ----------
echo ""
echo "[15] 취소 시 재고 복구 - product 5, 4개 취소"
out=$(request POST /api/customers/cancel "$cookie1" '{"productId":5,"quantity":4}')
status=$(echo "$out" | sed -n 1p)
assert_status "취소 200 OK" "$status" "200"

out=$(request GET /api/products/5 - -)
body=$(echo "$out" | sed -n 2p)
stock=$(echo "$body" | jq -r '.stock')
assert_eq "재고 복구 확인 (50+4=54)" "$stock" "54"

out=$(request GET /api/customers/skala01 "$cookie1" -)
body=$(echo "$out" | sed -n 2p)
qty=$(echo "$body" | jq -r '.products[] | select(.productId==5) | .quantity')
assert_eq "보유 수량 차감 확인 (10-4=6)" "$qty" "6"

# ---------- 16. 재고 부족 예외 ----------
echo ""
echo "[16] 재고 부족 예외 - product 5, 남은 재고(54)보다 많은 수량 주문"
out=$(request POST /api/customers/order "$cookie1" '{"productId":5,"quantity":9999}')
status=$(echo "$out" | sed -n 1p); body=$(echo "$out" | sed -n 2p)
assert_status "재고 부족 409 Conflict" "$status" "409"
errorCode=$(echo "$body" | jq -r '.errorCode')
assert_eq "errorCode = OUT_OF_STOCK" "$errorCode" "OUT_OF_STOCK"

# ---------- 결과 요약 ----------
echo ""
echo "============================================================"
echo " 결과: PASS $PASS_COUNT / FAIL $FAIL_COUNT"
echo "============================================================"

rm -rf "$COOKIE_DIR"

if [ "$FAIL_COUNT" -gt 0 ]; then
  exit 1
fi
exit 0
