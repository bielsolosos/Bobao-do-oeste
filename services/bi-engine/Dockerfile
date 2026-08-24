# Multi-stage build para o serviço Java (Spring Boot)
# Estágio 1: Build
FROM docker.io/library/eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copia arquivos do Maven Wrapper e POM para cache de dependências
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B || true

# Copia o código fonte e compila o JAR da aplicação
COPY src/ ./src/
RUN ./mvnw clean package -DskipTests

# Estágio 2: Runtime leve
FROM docker.io/library/eclipse-temurin:21-jre-alpine

WORKDIR /app

# Usuário não-root por segurança
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
