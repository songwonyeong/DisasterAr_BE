# 1) Java 17 런타임 이미지 사용
FROM eclipse-temurin:17-jdk

# 2) 컨테이너 내부 작업 디렉토리
WORKDIR /app

# 3) 빌드된 JAR 복사 (미리 ./gradlew build 수행)
COPY build/libs/disaster-ar.jar app.jar

# 4) 도커에서는 docker 프로필 사용
ENV SPRING_PROFILES_ACTIVE=docker

# 5) 컨테이너 내부 포트
EXPOSE 8080

# 6) 스프링부트 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
