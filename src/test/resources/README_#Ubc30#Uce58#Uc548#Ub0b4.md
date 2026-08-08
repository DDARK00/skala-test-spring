# JUnit 테스트 코드 배치 안내

## 파일 위치

```
src/test/java/com/skala/shop/repository/CustomerRepositoryTest.java
src/test/java/com/skala/shop/repository/ProductRepositoryTest.java
src/test/java/com/skala/shop/repository/CustomerHoldingRepositoryTest.java
src/test/java/com/skala/shop/data/entity/CustomerTest.java
src/test/java/com/skala/shop/data/entity/ProductTest.java
src/test/java/com/skala/shop/data/entity/CustomerHoldingTest.java
src/test/java/com/skala/shop/service/OrderServiceImplTest.java

src/test/resources/application-test.yml
```

## 실행 방법

```bash
./gradlew test
```

## 필요한 의존성 확인

`spring-boot-starter-test`에 JUnit5, Mockito, AssertJ가 전부 포함되어 있어 build.gradle에 이미 있는 것으로 충분합니다. 별도 추가 불필요.

## AssertJ import를 처음 쓰신다면

`assertThat`, `assertThatThrownBy` 등은 `spring-boot-starter-test`에 포함된 AssertJ에서 제공됩니다. 별도 설정 없이 바로 동작합니다.

## 실행 시 유의사항

- `@DataJpaTest`는 기본적으로 트랜잭션을 걸고 테스트 종료 시 자동 롤백합니다 — 테스트끼리 데이터가 안 섞입니다.
- `application-test.yml`을 안 넣으면 운영용 `data.sql`(대량 생성 데이터)이 매 테스트마다 실행되어 느려집니다. 반드시 함께 배치하세요.


- 상세 리포트는 ```build/reports/tests/test/index.html```