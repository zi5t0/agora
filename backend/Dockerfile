# syntax=docker/dockerfile:1

FROM eclipse-temurin:25.0.5_8-jdk-noble@sha256:80f67fb3d6f76e2d817d98a4cc2a9cf05f054d4e6adc5e2144bde671d722e63f AS build
WORKDIR /build

COPY .mvn/ .mvn/
COPY --chmod=0755 mvnw ./mvnw
COPY pom.xml .
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw -B clean verify

FROM eclipse-temurin:25.0.5_8-jre-noble@sha256:e3b6f7887a50f20d8f07abf279d8a81a5953fb398a9555bd9edd56b9bd15968c AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 agora \
    && useradd --uid 10001 --gid agora --no-create-home --shell /usr/sbin/nologin agora
WORKDIR /app

COPY --from=build /build/target/agora-backend-0.0.1-SNAPSHOT.jar /app/agora.jar

USER agora
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/agora.jar"]
