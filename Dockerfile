FROM maven:3.9.6-eclipse-temurin-21 AS builder 
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Render define a porta via $PORT
ENV PORT=${PORT}

ENV SERVER_PORT=${PORT}
ENV SPRING_APPLICATION_NAME=voting-system-api-gateway
ENV EUREKA_CLIENT_REGISTER_WITH_EUREKA=true
ENV EUREKA_CLIENT_FETCH_REGISTRY=true
ENV EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=https://voting-system-discovery.onrender.com/eureka
ENV EUREKA_INSTANCE_PREFER_IP_ADDRESS=true

ENTRYPOINT ["java", "-jar", "app.jar"]
