FROM maven:3.9.9-eclipse-temurin-17 AS build
    WORKDIR /workspace
    COPY . .
    RUN mvn -q -pl clientes-service -am -DskipTests clean package
    FROM eclipse-temurin:17-jre
    WORKDIR /app
    COPY --from=build /workspace/clientes-service/target/clientes-service-3.0.0.jar app.jar
    EXPOSE 8087
    ENTRYPOINT ["java","-jar","/app/app.jar"]