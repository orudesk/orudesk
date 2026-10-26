# One image per edition, built from source so a self-hoster needs only Docker, not a JDK.
#   docker build -t orudesk-standard api/                       (from the repo root)
#   docker build --build-arg APP=orudesk-standard -t ... api/   (APP picks the edition under apps/)

# ---- Stage 1: build the jar with the JDK ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
# The Gradle wrapper and build scripts first: they change rarely, so Docker reuses this layer.
COPY gradlew settings.gradle gradle.properties ./
COPY gradle gradle
COPY build-logic build-logic
RUN ./gradlew --version
COPY . .
ARG APP=orudesk-standard
# The cache mount keeps downloaded dependencies between builds on this machine, without putting them in the image.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :apps:${APP}:bootJar -x test \
 && find apps/${APP}/build/libs -name '*.jar' ! -name '*-plain.jar' -exec cp {} /src/app.jar \; \
 && java -Djarmode=tools -jar /src/app.jar extract --layers --launcher --destination /src/extracted

# ---- Stage 2: run it with only the JRE ----
FROM eclipse-temurin:25-jre
# Never run as root: a bug in the app then can't write to the image or bind privileged ports.
RUN useradd --system --uid 10001 --no-create-home orudesk
WORKDIR /app
# Layers in order of how often they change: libraries (rarely) first, our own classes (every build) last.
# A new release then re-downloads a few hundred KB, not the 80 MB of dependencies.
COPY --from=build /src/extracted/dependencies/ ./
COPY --from=build /src/extracted/spring-boot-loader/ ./
COPY --from=build /src/extracted/snapshot-dependencies/ ./
COPY --from=build /src/extracted/application/ ./
USER orudesk
EXPOSE 8080
# The image has no curl, so bash's /dev/tcp asks the readiness probe. Docker marks the container healthy when it says UP.
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=3 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf "GET /actuator/health/readiness HTTP/1.0\r\n\r\n" >&3 && grep -q "\"UP\"" <&3'
# MaxRAMPercentage: size the heap from the container's memory limit, not the host's.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
