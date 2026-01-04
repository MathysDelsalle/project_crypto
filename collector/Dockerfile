FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 1) Copier seulement le pom d'abord (cache deps)
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# 2) Copier le code ensuite
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
