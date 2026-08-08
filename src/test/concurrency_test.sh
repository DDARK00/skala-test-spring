#!/usr/bin/env bash
# concurrency_test.sh
# 동시 주문 시 포인트/재고 정합성 검증

BASE_URL="http://localhost:8080"
COOKIE="/tmp/concurrency_cookie.txt"

curl -s -c "$COOKIE" -X POST "$BASE_URL/api/customers/login" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"skala01","customerPassword":"pw1234"}' > /dev/null

# product 5(가습기) 기준 - 주문 전 상태 확인
before_stock=$(curl -s "$BASE_URL/api/products/5" | jq -r '.stock')
before_point=$(curl -s -b "$COOKIE" "$BASE_URL/api/customers/skala01" | jq -r '.point')

echo "주문 전 - 재고: $before_stock, 포인트: $before_point"

# 동시에 5개 요청 발사 (각 1개씩, 총 5개 소모되어야 함)
for i in $(seq 1 5); do
  curl -s -b "$COOKIE" -X POST "$BASE_URL/api/customers/order" \
    -H "Content-Type: application/json" \
    -d '{"productId":5,"quantity":1}' &
done
wait

after_stock=$(curl -s "$BASE_URL/api/products/5" | jq -r '.stock')
after_point=$(curl -s -b "$COOKIE" "$BASE_URL/api/customers/skala01" | jq -r '.point')

expected_stock=$((before_stock - 5))
product_price=45000
expected_point=$((before_point - product_price * 5))

echo "주문 후 - 재고: $after_stock (기대값: $expected_stock)"
echo "주문 후 - 포인트: $after_point (기대값: $expected_point)"

if [ "$after_stock" == "$expected_stock" ] && [ "$after_point" == "$expected_point" ]; then
  echo "[PASS] 동시 요청 5건 모두 정확히 반영됨 (lost update 없음)"
else
  echo "[FAIL] 동시성 문제 발생 - 일부 갱신이 유실됨"
fi