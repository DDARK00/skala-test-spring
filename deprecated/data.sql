-- ============================================
-- SKALA-SHOP 초기 데이터 (소규모, 시나리오 검증용)
-- 순서: Supplier/Category → Product → Customer → CustomerHolding → OrderLog
-- ============================================

-- 1. Supplier (매입처)
INSERT INTO supplier (name) VALUES ('삼성전자');
INSERT INTO supplier (name) VALUES ('로지텍코리아');
-- 3번째 상품은 supplier_id를 NULL로 남겨 nullable 케이스 검증

-- 2. Category
INSERT INTO category (name) VALUES ('컴퓨터주변기기');
INSERT INTO category (name) VALUES ('생활가전');

-- 3. Product (supplier_id, category_id 일부 NULL 허용)
INSERT INTO product (name, price, cost_price, stock, supplier_id, category_id)
VALUES ('무선마우스', 15000, 9000, 100, 2, 1);

INSERT INTO product (name, price, cost_price, stock, supplier_id, category_id)
VALUES ('블루투스키보드', 29000, 18000, 80, 2, 1);

INSERT INTO product (name, price, cost_price, stock, supplier_id, category_id)
VALUES ('USB허브', 39000, NULL, 50, NULL, NULL);

INSERT INTO product (name, price, cost_price, stock, supplier_id, category_id)
VALUES ('공기청정기', 129000, 95000, 30, 1, 2);

INSERT INTO product (name, price, cost_price, stock, supplier_id, category_id)
VALUES ('가습기', 45000, 28000, 60, 1, 2);

-- 4. Customer (customer_password는 'pw1234'의 BCrypt 해시)
INSERT INTO customer (customer_id, customer_password, customer_name, point)
VALUES ('skala01', '$2b$10$Ic/54XDX30EEMHAVr0UtleCXzYvu9.LvlhWOQGQxGB.UFlUgWttxC', '테스트고객', 1000000);

INSERT INTO customer (customer_id, customer_password, customer_name, point)
VALUES ('skala02', '$2b$10$Ic/54XDX30EEMHAVr0UtleCXzYvu9.LvlhWOQGQxGB.UFlUgWttxC', '홍길동', 50000);

INSERT INTO customer (customer_id, customer_password, customer_name, point)
VALUES ('skala03', '$2b$10$Ic/54XDX30EEMHAVr0UtleCXzYvu9.LvlhWOQGQxGB.UFlUgWttxC', '김철수', 10000);

-- 5. CustomerHolding (현재 보유 수량 - 재주문 누적 상태 미리 반영)
INSERT INTO customer_holding (customer_id, product_id, quantity, ordered_at)
VALUES (1, 1, 2, '2026-08-01 10:00:00');

INSERT INTO customer_holding (customer_id, product_id, quantity, ordered_at)
VALUES (1, 2, 1, '2026-08-02 11:30:00');

INSERT INTO customer_holding (customer_id, product_id, quantity, ordered_at)
VALUES (2, 3, 3, '2026-08-03 09:15:00');

-- 6. OrderLog (위 CustomerHolding과 앞뒤가 맞는 최소 이력)
INSERT INTO order_log (customer_id, product_id, type, quantity, price_at_order, created_at)
VALUES (1, 1, 'ORDER', 2, 15000, '2026-08-01 10:00:00');

INSERT INTO order_log (customer_id, product_id, type, quantity, price_at_order, created_at)
VALUES (1, 2, 'ORDER', 1, 29000, '2026-08-02 11:30:00');

INSERT INTO order_log (customer_id, product_id, type, quantity, price_at_order, created_at)
VALUES (2, 3, 'ORDER', 3, 39000, '2026-08-03 09:15:00');
