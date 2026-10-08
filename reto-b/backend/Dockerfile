# Etapa base: dependencias descargadas y código fuente. La usa el servicio de pruebas.
FROM maven:3.9-eclipse-temurin-21 AS test
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
CMD ["mvn", "-B", "test"]

# Etapa de construcción del jar ejecutable (las pruebas corren en su propio servicio).
FROM test AS build
RUN mvn -B -q package -DskipTests

# Imagen final: solo JRE y el jar, con usuario sin privilegios.
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/target/reto-b-*.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
