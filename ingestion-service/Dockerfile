# syntax=docker/dockerfile:1
#
# The jar is built on the host / in CI, not inside Docker (avoids corporate TLS-proxy issues
# and keeps the image build fast):
#   ./gradlew :ingestion-service:bootJar
#   docker build -t ingestion-service:0.0.1-SNAPSHOT ingestion-service

# ---- Stage 1: split the jar into layers -------------------------------------------------
# Dependencies change rarely, application code changes often: separate layers mean a code
# change only rebuilds/pushes the small "application" layer.
FROM eclipse-temurin:25-jre AS extract
WORKDIR /workspace
COPY build/libs/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---- Stage 2: runtime image -------------------------------------------------------------
FROM eclipse-temurin:25-jre

# Fixed numeric UID/GID so Kubernetes "runAsNonRoot" can verify the user.
RUN groupadd --system --gid 1001 app \
 && useradd --system --uid 1001 --gid app --no-create-home --shell /usr/sbin/nologin app

WORKDIR /application
COPY --from=extract /workspace/extracted/dependencies/ ./
COPY --from=extract /workspace/extracted/spring-boot-loader/ ./
COPY --from=extract /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extract /workspace/extracted/application/ ./

USER 1001:1001

# Size the heap from the container memory limit instead of the host's memory,
# and fail fast on OOM so Kubernetes restarts the pod.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "application.jar"]
