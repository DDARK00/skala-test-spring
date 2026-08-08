# ============================================================
# Stage 1: Build
# ============================================================
FROM gradle:8.8-jdk21 AS build

WORKDIR /app

# 의존성 캐시를 위해 build.gradle/settings.gradle 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 소스 복사 후 빌드 (테스트는 배포 이미지 빌드 시 생략 - CI에서 별도로 돌리는 것을 전제)
COPY src ./src
RUN gradle bootJar -x test --no-daemon

# ============================================================
# Stage 2: Run
# ============================================================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# non-root 사용자로 실행 (보안 관례)
RUN addgroup --system spring && adduser --system --ingroup spring spring
# logs 디렉토리를 미리 만들고 spring 사용자에게 쓰기 권한 부여
RUN mkdir -p /app/logs && chown -R spring:spring /app

USER spring:spring

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

# 컨테이너 메모리 제한을 인지하도록 JVM 옵션 부여 (무료 호스팅의 낮은 메모리 한도 대응)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
