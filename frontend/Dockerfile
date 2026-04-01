# Build stage — compiles Kotlin/Wasm and produces the browser distribution
FROM gradle:8.8-jdk17 AS build

WORKDIR /app

# Copy Gradle wrapper and config first to leverage layer caching
COPY gradlew gradlew.bat settings.gradle.kts gradle.properties ./
COPY gradle/ gradle/
COPY frontend/ frontend/

RUN chmod +x gradlew && \
    ./gradlew :frontend:wasmJsBrowserDistribution --no-daemon

# Serve stage — lightweight nginx to serve the static assets
FROM nginx:1.27-alpine AS serve

COPY --from=build \
    /app/frontend/build/dist/wasmJs/productionExecutable \
    /usr/share/nginx/html

# Override default nginx config to handle single-page app routing
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
