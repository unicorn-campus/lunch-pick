# payment-service Dockerfile
FROM amazoncorretto:21-alpine3.21-jdk

WORKDIR /app

# JAR 복사
COPY build/libs/*.jar app.jar

# JVM 옵션 (메모리 최적화)
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport"

# 포트 노출
EXPOSE 8083

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT:-8083}/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
