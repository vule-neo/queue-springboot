# ---------- faza 1: build ----------
# Maven image umjesto mvnw: mvnw sa Windowsa zna doci sa CRLF i bez
# execute bita, pa puca u Linux kontejneru. mvn iz image-a nema taj problem.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Prvo samo pom.xml + zavisnosti: ovaj sloj se kesira dok se pom ne
# promijeni, pa promjena u src ne skida ponovo pola Maven Centrala.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
# Testovi trce u CI-ju (GitHub Actions) sa Testcontainers-om; u Docker
# buildu nema Dockera pa bi i onako pali. Zato -DskipTests ovdje.
RUN mvn -q -B -DskipTests package

# ---------- faza 2: runtime ----------
# Samo JRE, bez Mavena i izvornog koda: image je ~4x manji, a i manja
# povrsina za napad (nema kompajlera u produkciji).
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Ne trci kao root. Ako neko probije aplikaciju, nema root u kontejneru.
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
