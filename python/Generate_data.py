#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SKALA-SHOP 초기 데이터(data.sql) 생성기

설계 원칙
---------
1. Product.stock == StockHistory 합산, Customer.point == PointHistory 합산이
   항상 성립하도록 "이력을 먼저 만들고, 최종 상태는 이력을 합산해서 도출"하는 순서로 생성한다.
2. CustomerHolding.quantity 는 OrderLog(ORDER=+, CANCEL=-)의 순계산 값과 일치해야 하며,
   순계산이 0 이하인 (customer, product) 쌍은 row를 만들지 않는다 (엔티티의 "0이면 삭제" 규칙 반영).
3. 시뮬레이션 도중 재고/포인트가 부족해지는 이벤트는 스킵한다 (음수 상태가 생기지 않게).
4. 생성(generate)과 검증(verify)의 계산 경로를 분리한다.
   검증은 생성 시 사용한 누적 변수를 그대로 믿지 않고, 최종 이력 리스트에서 처음부터 다시 합산해서 비교한다.

사용법
------
    pip install bcrypt --break-system-packages
    python3 generate_data.py

CONFIG 딕셔너리의 값만 조절하면 데이터 규모/시드를 바꿀 수 있다.
출력물: data.sql (프로젝트의 src/main/resources/data.sql 로 그대로 사용 가능)
"""

import random
from datetime import datetime, timedelta

import bcrypt

# ============================================================
# 설정 - 여기 값만 바꾸면 데이터 규모가 바뀝니다
# ============================================================
CONFIG = {
    "seed": 42,                # 재현 가능한 랜덤 시드. None이면 매번 다른 데이터
    "num_suppliers": 5,
    "num_categories": 6,
    "num_products": 30,
    "num_customers": 15,
    "num_events": 200,         # 주문/취소 시뮬레이션 이벤트 총 횟수 (일부는 조건 미충족으로 스킵될 수 있음)
    "cancel_probability": 0.3, # 이벤트마다 취소를 시도할 확률 (나머지는 주문 시도)
    "default_password": "pw1234",
    "output_path": "data.sql",
}

# ============================================================
# 참조 데이터 풀
# ============================================================
SUPPLIER_NAMES = ["삼성전자", "로지텍코리아", "LG생활건강", "다이슨코리아", "샤오미코리아",
                   "필립스코리아", "쿠쿠전자", "테팔코리아"]

CATEGORY_NAMES = ["컴퓨터주변기기", "생활가전", "주방용품", "문구/오피스", "스포츠/레저", "뷰티/헬스"]

PRODUCT_POOL = [
    ("무선마우스", 15000, 9000), ("블루투스키보드", 29000, 18000), ("USB허브", 39000, 24000),
    ("공기청정기", 129000, 95000), ("가습기", 45000, 28000), ("전기포트", 32000, 19000),
    ("커피머신", 89000, 60000), ("에어프라이어", 79000, 52000), ("믹서기", 42000, 27000),
    ("노트북거치대", 25000, 14000), ("웹캠", 55000, 34000), ("헤드셋", 68000, 41000),
    ("모니터암", 47000, 29000), ("무선충전기", 22000, 13000), ("보조배터리", 31000, 18000),
    ("볼펜세트", 8000, 4000), ("노트", 5000, 2200), ("다이어리", 12000, 6500),
    ("요가매트", 27000, 15000), ("덤벨세트", 55000, 33000), ("폼롤러", 18000, 9500),
    ("전동칫솔", 62000, 38000), ("헤어드라이어", 71000, 44000), ("면도기", 58000, 35000),
    ("가스레인지청소기", 15000, 8000), ("식기건조대", 34000, 20000), ("밀폐용기세트", 21000, 11000),
    ("무선청소기", 189000, 130000), ("로봇청소기", 259000, 180000), ("전기밥솥", 149000, 102000),
]

CUSTOMER_NAMES = ["김민준", "이서연", "박도윤", "최지우", "정하은", "강시우", "조유나", "윤은우",
                   "장서준", "임하윤", "한지호", "오다인", "서예준", "신소율", "권태윤", "황수아",
                   "안지훈", "송민서", "류하람", "홍길동"]


def hash_password(pw: str) -> str:
    return bcrypt.hashpw(pw.encode(), bcrypt.gensalt(rounds=10)).decode()


# ============================================================
# 1단계: 마스터 데이터 생성 (Supplier, Category, Product, Customer)
# ============================================================

def generate_suppliers(n):
    names = random.sample(SUPPLIER_NAMES, min(n, len(SUPPLIER_NAMES)))
    return [{"id": i + 1, "name": name} for i, name in enumerate(names)]


def generate_categories(n):
    names = random.sample(CATEGORY_NAMES, min(n, len(CATEGORY_NAMES)))
    return [{"id": i + 1, "name": name} for i, name in enumerate(names)]


def generate_products(n, suppliers, categories):
    """초기 재고(target_stock)만 정해두고, 실제 stock/StockHistory는 2단계에서 채운다."""
    pool = PRODUCT_POOL.copy()
    random.shuffle(pool)
    products = []
    for i in range(n):
        name, price, cost_price = pool[i % len(pool)]
        # 이름 중복 방지 (풀보다 많이 요청하면 접미어 부여)
        if i >= len(pool):
            name = f"{name}-{i // len(pool) + 1}"

        supplier = random.choice(suppliers) if suppliers and random.random() > 0.1 else None
        category = random.choice(categories) if categories and random.random() > 0.1 else None
        cost_price = cost_price if random.random() > 0.15 else None  # 가끔 원가 정보 없음(nullable 검증)

        products.append({
            "id": i + 1,
            "name": name,
            "price": price,
            "cost_price": cost_price,
            "target_stock": random.randint(50, 300),  # 최초 입고량 (StockHistory로 반영될 값)
            "supplier_id": supplier["id"] if supplier else None,
            "category_id": category["id"] if category else None,
        })
    return products


def generate_customers(n):
    names = (CUSTOMER_NAMES * ((n // len(CUSTOMER_NAMES)) + 1))[:n]
    random.shuffle(names)
    pw_hash = hash_password(CONFIG["default_password"])
    customers = []
    for i in range(n):
        signup_point = random.choice([200000, 500000, 800000, 1200000, 2000000])
        customers.append({
            "id": i + 1,
            "customer_id": f"skala{i + 1:02d}",
            "customer_password": pw_hash,
            "customer_name": names[i],
            "signup_point": signup_point,  # PointHistory의 SIGNUP_BONUS 값
        })
    return customers


# ============================================================
# 2단계: 이력(History) 생성 - 여기서 실제 상태(stock/point)가 결정된다
# ============================================================

def generate_history(products, customers, num_events, cancel_probability):
    """
    반환값: dict(
        stock_history=[...], point_history=[...], order_log=[...],
        final_stock={product_id: int}, final_point={customer_id: int},
        holdings={(customer_id, product_id): {"quantity": int, "last_order_at": datetime}}
    )
    """
    base_time = datetime(2026, 7, 1, 9, 0, 0)
    cursor = base_time

    stock_history = []
    point_history = []
    order_log = []

    current_stock = {}
    current_point = {}
    holdings = {}  # (customer_id, product_id) -> {"quantity": int, "last_order_at": datetime}

    # --- 2-1. 최초 입고 (PURCHASE_IN) ---
    for p in products:
        current_stock[p["id"]] = p["target_stock"]
        stock_history.append({
            "product_id": p["id"],
            "amount": p["target_stock"],
            "unit_cost": p["cost_price"],
            "reason": "PURCHASE_IN",
            "created_at": cursor,
        })
    cursor += timedelta(hours=1)

    # --- 2-2. 가입 축하 포인트 (SIGNUP_BONUS) ---
    for c in customers:
        current_point[c["id"]] = c["signup_point"]
        point_history.append({
            "customer_id": c["id"],
            "amount": c["signup_point"],
            "reason": "SIGNUP_BONUS",
            "created_at": cursor,
        })
    cursor += timedelta(hours=1)

    # --- 2-3. 주문/취소 이벤트 시뮬레이션 ---
    skipped = 0
    for _ in range(num_events):
        cursor += timedelta(minutes=random.randint(5, 240))
        customer = random.choice(customers)
        product = random.choice(products)
        key = (customer["id"], product["id"])

        try_cancel = random.random() < cancel_probability and holdings.get(key, {}).get("quantity", 0) > 0

        if try_cancel:
            held = holdings[key]["quantity"]
            qty = random.randint(1, held)
            unit_price = product["price"]
            refund = unit_price * qty

            # 상태 갱신
            current_stock[product["id"]] += qty
            current_point[customer["id"]] += refund
            holdings[key]["quantity"] -= qty
            if holdings[key]["quantity"] <= 0:
                del holdings[key]

            stock_history.append({
                "product_id": product["id"], "amount": qty, "unit_cost": None,
                "reason": "CANCEL_RETURN", "created_at": cursor,
            })
            point_history.append({
                "customer_id": customer["id"], "amount": refund,
                "reason": "CANCEL", "created_at": cursor,
            })
            order_log.append({
                "customer_id": customer["id"], "product_id": product["id"],
                "type": "CANCEL", "quantity": qty, "price_at_order": unit_price,
                "created_at": cursor,
            })
        else:
            unit_price = product["price"]
            max_by_point = current_point[customer["id"]] // unit_price
            max_by_stock = current_stock[product["id"]]
            max_qty = min(5, max_by_point, max_by_stock)

            if max_qty < 1:
                skipped += 1
                continue

            qty = random.randint(1, max_qty)
            total = unit_price * qty

            current_stock[product["id"]] -= qty
            current_point[customer["id"]] -= total
            if key not in holdings:
                holdings[key] = {"quantity": 0, "last_order_at": cursor}
            holdings[key]["quantity"] += qty
            holdings[key]["last_order_at"] = cursor

            stock_history.append({
                "product_id": product["id"], "amount": -qty, "unit_cost": None,
                "reason": "SALE_OUT", "created_at": cursor,
            })
            point_history.append({
                "customer_id": customer["id"], "amount": -total,
                "reason": "ORDER", "created_at": cursor,
            })
            order_log.append({
                "customer_id": customer["id"], "product_id": product["id"],
                "type": "ORDER", "quantity": qty, "price_at_order": unit_price,
                "created_at": cursor,
            })

    print(f"[생성] 이벤트 {num_events}건 중 {skipped}건 스킵(재고/포인트 부족) "
          f"-> 실제 반영 {num_events - skipped}건")

    return {
        "stock_history": stock_history,
        "point_history": point_history,
        "order_log": order_log,
        "final_stock": current_stock,
        "final_point": current_point,
        "holdings": holdings,
    }


# ============================================================
# 3단계: SQL 파일 출력
# ============================================================

def sql_str(value):
    if value is None:
        return "NULL"
    if isinstance(value, str):
        escaped = value.replace("'", "''")
        return f"'{escaped}'"
    if isinstance(value, datetime):
        return f"'{value.strftime('%Y-%m-%d %H:%M:%S')}'"
    return str(value)


def export_sql(path, suppliers, categories, products, customers, history):
    lines = []
    w = lines.append

    w("-- ============================================================")
    w("-- SKALA-SHOP 초기 데이터 (자동 생성)")
    w(f"-- 생성 설정: {CONFIG}")
    w("-- 원칙: Product.stock / Customer.point 는 각각 StockHistory / PointHistory 합산과 일치")
    w("--       CustomerHolding.quantity 는 OrderLog(ORDER=+, CANCEL=-) 순계산과 일치")
    w("-- ============================================================")
    w("")

    w("-- 1. Supplier")
    for s in suppliers:
        w(f"INSERT INTO supplier (name) VALUES ({sql_str(s['name'])});")
    w("")

    w("-- 2. Category")
    for c in categories:
        w(f"INSERT INTO category (name) VALUES ({sql_str(c['name'])});")
    w("")

    w("-- 3. Product")
    for p in products:
        final_stock = history["final_stock"][p["id"]]
        w(f"INSERT INTO product (name, price, cost_price, stock, supplier_id, category_id) "
          f"VALUES ({sql_str(p['name'])}, {p['price']}, {sql_str(p['cost_price'])}, "
          f"{final_stock}, {sql_str(p['supplier_id'])}, {sql_str(p['category_id'])});")
    w("")

    w("-- 4. Customer")
    for c in customers:
        final_point = history["final_point"][c["id"]]
        w(f"INSERT INTO customer (customer_id, customer_password, customer_name, point) "
          f"VALUES ({sql_str(c['customer_id'])}, {sql_str(c['customer_password'])}, "
          f"{sql_str(c['customer_name'])}, {final_point});")
    w("")

    w("-- 5. CustomerHolding (최종 순보유 수량, 0 이하인 쌍은 제외)")
    for (cust_id, prod_id), info in sorted(history["holdings"].items()):
        w(f"INSERT INTO customer_holding (customer_id, product_id, quantity, ordered_at) "
          f"VALUES ({cust_id}, {prod_id}, {info['quantity']}, {sql_str(info['last_order_at'])});")
    w("")

    w("-- 6. OrderLog")
    for o in history["order_log"]:
        w(f"INSERT INTO order_log (customer_id, product_id, type, quantity, price_at_order, created_at) "
          f"VALUES ({o['customer_id']}, {o['product_id']}, {sql_str(o['type'])}, "
          f"{o['quantity']}, {o['price_at_order']}, {sql_str(o['created_at'])});")
    w("")

    w("-- 7. StockHistory")
    for s in history["stock_history"]:
        w(f"INSERT INTO stock_history (product_id, amount, unit_cost, reason, created_at) "
          f"VALUES ({s['product_id']}, {s['amount']}, {sql_str(s['unit_cost'])}, "
          f"{sql_str(s['reason'])}, {sql_str(s['created_at'])});")
    w("")

    w("-- 8. PointHistory")
    for p in history["point_history"]:
        w(f"INSERT INTO point_history (customer_id, amount, reason, created_at) "
          f"VALUES ({p['customer_id']}, {p['amount']}, {sql_str(p['reason'])}, {sql_str(p['created_at'])});")
    w("")

    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    print(f"[출력] {path} 생성 완료 ({len(lines)}줄)")


# ============================================================
# 4단계: 독립 검증 - 생성 시 누적 변수 대신 이력 리스트에서 처음부터 다시 합산
# ============================================================

def verify(suppliers, categories, products, customers, history):
    errors = []

    supplier_ids = {s["id"] for s in suppliers}
    category_ids = {c["id"] for c in categories}
    product_ids = {p["id"] for p in products}
    customer_ids = {c["id"] for c in customers}

    # 4-1. 참조 무결성
    for p in products:
        if p["supplier_id"] is not None and p["supplier_id"] not in supplier_ids:
            errors.append(f"Product {p['id']}: 존재하지 않는 supplier_id={p['supplier_id']}")
        if p["category_id"] is not None and p["category_id"] not in category_ids:
            errors.append(f"Product {p['id']}: 존재하지 않는 category_id={p['category_id']}")

    for o in history["order_log"]:
        if o["customer_id"] not in customer_ids:
            errors.append(f"OrderLog: 존재하지 않는 customer_id={o['customer_id']}")
        if o["product_id"] not in product_ids:
            errors.append(f"OrderLog: 존재하지 않는 product_id={o['product_id']}")

    # 4-2. Product.stock == StockHistory 재합산 (독립 재계산)
    recomputed_stock = {p["id"]: 0 for p in products}
    for s in history["stock_history"]:
        recomputed_stock[s["product_id"]] += s["amount"]

    for p in products:
        final_stock = history["final_stock"][p["id"]]
        if recomputed_stock[p["id"]] != final_stock:
            errors.append(
                f"Product {p['id']} 재고 불일치: export값={final_stock}, "
                f"StockHistory 재합산={recomputed_stock[p['id']]}"
            )
        if recomputed_stock[p["id"]] < 0:
            errors.append(f"Product {p['id']} 재고가 음수: {recomputed_stock[p['id']]}")

    # 4-3. Customer.point == PointHistory 재합산 (독립 재계산)
    recomputed_point = {c["id"]: 0 for c in customers}
    for ph in history["point_history"]:
        recomputed_point[ph["customer_id"]] += ph["amount"]

    for c in customers:
        final_point = history["final_point"][c["id"]]
        if recomputed_point[c["id"]] != final_point:
            errors.append(
                f"Customer {c['id']} 포인트 불일치: export값={final_point}, "
                f"PointHistory 재합산={recomputed_point[c['id']]}"
            )
        if recomputed_point[c["id"]] < 0:
            errors.append(f"Customer {c['id']} 포인트가 음수: {recomputed_point[c['id']]}")

    # 4-4. CustomerHolding.quantity == OrderLog 순계산 (독립 재계산)
    recomputed_holdings = {}
    for o in history["order_log"]:
        key = (o["customer_id"], o["product_id"])
        delta = o["quantity"] if o["type"] == "ORDER" else -o["quantity"]
        recomputed_holdings[key] = recomputed_holdings.get(key, 0) + delta

    # 0 이하는 holding에 없어야 함 / 0 초과는 정확히 일치해야 함
    exported_keys = set(history["holdings"].keys())
    for key, qty in recomputed_holdings.items():
        if qty > 0:
            if key not in exported_keys:
                errors.append(f"CustomerHolding 누락: {key} 순계산={qty}인데 export에 없음")
            elif history["holdings"][key]["quantity"] != qty:
                errors.append(
                    f"CustomerHolding {key} 수량 불일치: export="
                    f"{history['holdings'][key]['quantity']}, 재계산={qty}"
                )
        else:
            if key in exported_keys:
                errors.append(f"CustomerHolding 오export: {key} 순계산={qty}(<=0)인데 row가 존재함")

    for key in exported_keys:
        if key not in recomputed_holdings or recomputed_holdings[key] <= 0:
            errors.append(f"CustomerHolding {key}: export 되었으나 재계산 결과와 불일치")

    # --- 결과 출력 ---
    print("\n" + "=" * 60)
    print(" 데이터 정합성 검증 결과")
    print("=" * 60)
    if not errors:
        print(f"[PASS] 전부 통과")
        print(f"  - Supplier {len(suppliers)}건, Category {len(categories)}건")
        print(f"  - Product {len(products)}건, Customer {len(customers)}건")
        print(f"  - OrderLog {len(history['order_log'])}건")
        print(f"  - StockHistory {len(history['stock_history'])}건")
        print(f"  - PointHistory {len(history['point_history'])}건")
        print(f"  - CustomerHolding {len(history['holdings'])}건")
    else:
        print(f"[FAIL] {len(errors)}건의 불일치 발견:")
        for e in errors:
            print(f"  - {e}")
    print("=" * 60)

    return len(errors) == 0


# ============================================================
# 메인
# ============================================================

def main():
    if CONFIG["seed"] is not None:
        random.seed(CONFIG["seed"])

    suppliers = generate_suppliers(CONFIG["num_suppliers"])
    categories = generate_categories(CONFIG["num_categories"])
    products = generate_products(CONFIG["num_products"], suppliers, categories)
    customers = generate_customers(CONFIG["num_customers"])

    history = generate_history(
        products, customers,
        CONFIG["num_events"], CONFIG["cancel_probability"],
    )

    ok = verify(suppliers, categories, products, customers, history)

    export_sql(CONFIG["output_path"], suppliers, categories, products, customers, history)

    if not ok:
        raise SystemExit("검증 실패 - data.sql은 생성되었지만 내용을 확인하세요.")


if __name__ == "__main__":
    main()