# Stage 1: Build source code
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY . .
# Thêm cấu hình encoding để tránh lỗi tiếng Việt
RUN mvn clean package -DskipTests -Dproject.build.sourceEncoding=UTF-8

# Stage 2: Chạy ứng dụng (Bản Runtime nhẹ)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

# CẤU HÌNH QUAN TRỌNG NHẤT:
# 1. -Xms256m -Xmx350m: Ép Java dùng tối đa 350MB RAM (chừa lại ~150MB cho OS và tiến trình khác của Container)
# 2. -XX:+UseSerialGC: Dùng thuật toán dọn rác đơn giản nhất, tiết kiệm RAM nhất (phù hợp 1 vCPU)
# 3. -Dserver.port=8080: Ép chạy port 8080
ENTRYPOINT ["java", "-Xms256m", "-Xmx350m", "-XX:+UseSerialGC", "-Dserver.port=8080", "-jar", "app.jar"]