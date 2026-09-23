# syntax=docker/dockerfile:1

# ---------- Stage 1: build the jar ----------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

# Cache dependencies in their own layer: this layer only invalidates when
# pom.xml changes, not on every source edit, so rebuilds during development
# skip re-downloading the whole dependency tree.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Stage 2: minimal runtime ----------
# JRE-only image (no compiler, no build tooling) — smaller attack surface
# and a smaller pulled image than shipping the JDK to production.
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

# Run as a dedicated, non-root, unprivileged user.
RUN addgroup -S taskflow && adduser -S taskflow -G taskflow

COPY --from=build /build/target/taskflow.jar app.jar
RUN chown taskflow:taskflow app.jar
USER taskflow

ENV JAVA_OPTS=""
EXPOSE 8080

# Alpine's wget (from busybox) is used here so the healthcheck needs no
# extra package installed. Matches the /actuator/health endpoint exposed
# permit-all in SecurityConfig.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://127.0.0.1:${PORT:-8080}/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
