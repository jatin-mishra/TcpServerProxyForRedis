# Dockerize TcpServerProxyForRedis — Plan

## Context

The project is a Java TCP proxy server (port `9736`) with a Redis-backed rate limiter.
Goal: add a simple, production-ready multi-stage `Dockerfile` (no compose).

Current state: no Docker files exist.

---

## Project Facts

| Property | Value |
|---|---|
| Java version | 17 |
| Gradle version | 9.2.0 (wrapper) |
| TCP port | `9736` |
| Entry point | `org.example.Main` |
| Artifact | `build/libs/BrowserStackMachineCodding-1.0-SNAPSHOT.jar` |
| Runtime deps | `lettuce-core`, `commons-pool2` |

---

## Problem: Thin JAR

Default `gradle build` produces a **thin jar** — dependencies are not bundled.
`java -jar` on a thin jar fails at runtime. The fat-jar (shadow) plugin resolves this.

---

## Changes Required

### 1. `build.gradle` — add Shadow plugin

```diff
 plugins {
     id 'java'
+    id 'com.github.johnrengelman.shadow' version '8.1.1'
 }

 ...

+shadowJar {
+    archiveClassifier.set('')          // keeps artifact name clean (no "-all" suffix)
+    manifest {
+        attributes 'Main-Class': 'org.example.Main'
+    }
+}
```

`./gradlew shadowJar` → self-runnable fat jar at `build/libs/BrowserStackMachineCodding-1.0-SNAPSHOT.jar`.

---

### 2. `Dockerfile` (new file)

Multi-stage build: JDK image to compile, JRE-Alpine image to run (smaller final image).

```dockerfile
# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Copy Gradle wrapper and config first → Docker layer caches dependency downloads
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Copy source and produce fat jar
COPY src/ src/
RUN ./gradlew shadowJar --no-daemon

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /app/build/libs/BrowserStackMachineCodding-1.0-SNAPSHOT.jar app.jar

EXPOSE 9736

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

### 3. `.dockerignore` (new file)

Prevents unnecessary files from being sent to the Docker build context:

```
.gradle/
build/
.idea/
*.md
.git/
```

---

## Environment Variables

All have defaults — none are required to start the container.

| Variable | Default | Purpose |
|---|---|---|
| `REDIS_HOST` | `localhost` | Redis server hostname |
| `REDIS_PORT` | `6379` | Redis server port |
| `REDIS_POOL_MAX` | `8` | Max pool connections |
| `REDIS_POOL_MAX_IDLE` | `4` | Max idle connections |
| `REDIS_POOL_MIN_IDLE` | `1` | Min idle connections |
| `RL_ALGORITHM` | `sliding_window` | Rate-limit algorithm |

---

## Build & Run

```bash
# Build image
docker build -t tcp-proxy-redis .

# Run — connect to Redis on the host machine (Mac/Windows)
docker run -p 9736:9736 \
  -e REDIS_HOST=host.docker.internal \
  tcp-proxy-redis

# Run — connect to a named Redis container on the same Docker network
docker run -p 9736:9736 \
  -e REDIS_HOST=my-redis \
  -e REDIS_PORT=6379 \
  tcp-proxy-redis
```

---

## Verification Steps

1. `docker build -t tcp-proxy-redis .` → build must complete without errors
2. `docker run -p 9736:9736 -e REDIS_HOST=host.docker.internal tcp-proxy-redis` → server starts and listens
3. `telnet localhost 9736` then send `PING` → expect `+PONG`
4. Final image size should be ~180–220 MB (JRE Alpine base)
