# Multi-stage build for the banking-app monolith.
# Produces the same image tag as Jib (openfinova/banking-app:local) so `docker compose up --build`
# works without a separate `./mvnw package` step. Jib remains available for faster local iteration
# when Docker layer caching is not needed (`./mvnw -pl banking-app -am package`).

FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace

COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
COPY . .

RUN chmod +x mvnw \
    && ./mvnw -pl banking-app -am package -DskipTests -Djib.skip=true -B -ntp

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S openfinova && adduser -S openfinova -G openfinova \
    && mkdir -p /var/lib/openfinova/tan \
    && chown -R openfinova:openfinova /var/lib/openfinova

COPY docker/banking-app/docker-entrypoint.sh /docker-entrypoint.sh
RUN sed -i 's/\r$//' /docker-entrypoint.sh && chmod +x /docker-entrypoint.sh

COPY --from=build /workspace/banking-app/target/banking-app-*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["/docker-entrypoint.sh"]
