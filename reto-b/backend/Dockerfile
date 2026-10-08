# ---- Build stage: compila y empaqueta con el Maven Wrapper sobre JDK 21 ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copiar primero el wrapper y el pom para cachear la descarga de dependencias.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Copiar el código y construir el jar (sin tests: ya corren en CI / build local).
COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests

# ---- Runtime stage: solo el JRE y el jar ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Usuario no root por seguridad.
RUN useradd --system --uid 1001 spring
USER spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
