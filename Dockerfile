FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime com JDK 21 leve
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080

ENV SERVER_PORT=8080
ENV SPRING_APPLICATION_NAME=voting-system-api-gateway
ENV EUREKA_CLIENT_REGISTERWITHFUREKA=true
ENV EUREKA_CLIENT_FETCHREGISTRY=true
ENV EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://voting-system-discovery.onrender.com:8761/eureka
ENV EUREKA_INSTANCE_PREFERIPADDRESS=true

ENTRYPOINT ["java", "-jar", "app.jar"]