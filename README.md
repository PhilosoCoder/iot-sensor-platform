# iot-sensor-platform
IoT Sensor Monitoring Platform — A real-time data ingestion and processing system for IoT sensor telemetry. Ingests high-volume sensor readings via Kafka, processes and aggregates data using Kafka Streams, detects anomalies, and streams live updates to clients. Built with Java 25, Spring Boot, and deployed on Kubernetes/AKS.

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (also needed for `./gradlew test`/`integrationTest`/`build` and `bootTestRun`, see below)
- [minikube](https://minikube.sigs.k8s.io/docs/start/) 
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- [helm](https://helm.sh/docs/intro/install/)

## Local Development

### 1. Start minikube

```bash
minikube start --cpus=4 --memory=7800
```

### 2. Run bootstrap script

> The script is idempotent — safe to run multiple times.

```bash
./k8s/scripts/bootstrap.sh
```

It installs the Strimzi operator in the version pinned by `STRIMZI_VERSION` in the script, then PostgreSQL, Redis
and the Kafka cluster.

> **Behind a TLS-intercepting proxy** (Strimzi pods stuck in `ErrImagePull`/`ErrImageNeverPull`, events show
> `x509: certificate signed by unknown authority` for `quay.io`): minikube doesn't trust the proxy's certificate, but
> the host Docker does. Pull the Strimzi images on the host, load them into minikube, then wait for the pods (or
> re-run the script):
>
> ```bash
> docker pull quay.io/strimzi/operator:1.3.1
> docker pull quay.io/strimzi/kafka:1.3.1-kafka-4.3.2
> minikube image load quay.io/strimzi/operator:1.3.1 quay.io/strimzi/kafka:1.3.1-kafka-4.3.2
> ```
>
> The tags are `operator:<STRIMZI_VERSION>` and `kafka:<STRIMZI_VERSION>-kafka-<spec.kafka.version in kafka.yaml>`.

> **Upgrading Strimzi or Kafka:** bump `STRIMZI_VERSION` (in `bootstrap.sh`) and/or `spec.kafka.version` (in
> `k8s/infrastructure/kafka/kafka.yaml`) and re-run the script. It upgrades the operator before applying the Kafka
> resource, which matters because a Kafka version is only accepted by operators that support it (see the Strimzi
> release notes). The operator then rolls the Kafka pods to the new version.

### 3. Verify everything is running

```bash
kubectl get pods -n iot-sensor-platform
kubectl get pods -n kafka
```

Expected output:
```
# iot-sensor-platform namespace
NAME           READY   STATUS    RESTARTS   AGE
postgresql-0   1/1     Running   0          ...
redis-0        1/1     Running   0          ...

# kafka namespace
NAME                                        READY   STATUS    RESTARTS   AGE
iot-kafka-dual-role-0                       1/1     Running   0          ...
iot-kafka-entity-operator-xxx               2/2     Running   0          ...
strimzi-cluster-operator-xxx                1/1     Running   0          ...
```

### 4. Build the ingestion-service image inside minikube

The jar is built on the host, the Dockerfile only packages it (layered, non-root, container-aware JVM). The image is
built directly in minikube's Docker daemon, so there is nothing to load or push:

```bash
./gradlew :ingestion-service:bootJar
minikube image build -t ingestion-service:0.0.1-SNAPSHOT -f Dockerfile ingestion-service
```

Verify the image inside minikube is the one you just built (the age should be seconds or minutes, not days):

```bash
minikube ssh -- "docker images --format '{{.Repository}}:{{.Tag}}  {{.ID}}  {{.CreatedSince}}' ingestion-service"
```

> Why not `docker build` + `minikube image load`? When a running pod uses the tag, `minikube image load` (even with
> `--overwrite`) silently keeps the old image, so a rollout restart starts the old code again.
>
> `-f Dockerfile` is required: `.dockerignore` excludes everything except the jar, and without `-f` the Dockerfile
> itself is left out of the context `minikube image build` sends.
>
> Avoid `eval $(minikube docker-env)` in your main shell: the variables stay set, and `./gradlew integrationTest` would then
> start its Testcontainers inside minikube.

> Use a fixed tag (not `latest`): with `latest`, Kubernetes defaults to `imagePullPolicy: Always` and tries to pull from Docker Hub instead of using the image built inside minikube.

### 5. Deploy the ingestion-service

```bash
kubectl apply -f k8s/apps/ingestion-service/
kubectl rollout status deployment/ingestion-service -n iot-sensor-platform
```

After a code change, rebuild the jar and the image (step 4) under the same tag, then restart the pods so they pick it
up. `kubectl apply` alone does nothing here, because the Deployment spec hasn't changed:

```bash
./gradlew :ingestion-service:bootJar
minikube image build -t ingestion-service:0.0.1-SNAPSHOT -f Dockerfile ingestion-service
kubectl rollout restart deployment/ingestion-service -n iot-sensor-platform
kubectl rollout status deployment/ingestion-service -n iot-sensor-platform
```

The same restart is needed after changing `configmap.yaml` (or `postgresql-secret`): the values are injected as
environment variables, which are only read when a container starts. `kubectl apply` updates the ConfigMap, but the
running pods keep the old values:

```bash
kubectl apply -f k8s/apps/ingestion-service/configmap.yaml
kubectl rollout restart deployment/ingestion-service -n iot-sensor-platform
kubectl rollout status deployment/ingestion-service -n iot-sensor-platform
```

### 6. Try it out

```bash
kubectl port-forward svc/ingestion-service 8080:8080 -n iot-sensor-platform
```

In another terminal:

```bash
curl localhost:8080/actuator/health/readiness

curl -X POST localhost:8080/v1/readings \
  -H 'Content-Type: application/json' \
  -d '{"sensorId":"sensor-1","value":21.5}'

curl localhost:8080/v1/sensors/sensor-1/readings

# Optional time range (from inclusive, to exclusive, ISO-8601 instants) and paging
curl 'localhost:8080/v1/sensors/sensor-1/readings?from=2026-11-16T10:00:00Z&to=2026-11-16T11:00:00Z&page=0&size=100'

curl localhost:8080/v1/sensors
```

The API is also browsable in Swagger UI at http://localhost:8080/swagger-ui.html (OpenAPI spec at
http://localhost:8080/v3/api-docs).

List endpoints are paginated:

```json
{
  "content": [
    {"id": "…", "sensorId": "sensor-1", "value": 21.5, "measuredAt": "2026-11-16T10:15:30.123456Z"}
  ],
  "page": {"size": 50, "number": 0, "totalElements": 1, "totalPages": 1}
}
```

- `page` is 0-based. `size` defaults to 50; larger values than 500 are capped to 500.
- The order is fixed and `sort` is ignored: readings are newest first (id as tie-breaker, so pages are stable),
  sensors are ordered by id.

Invalid requests return `400` with an RFC 9457 `application/problem+json` body that lists every invalid field:

```bash
curl -X POST localhost:8080/v1/readings \
  -H 'Content-Type: application/json' \
  -d '{"sensorId":"bad id"}'
```

```json
{
  "detail": "Invalid request content.",
  "instance": "/v1/readings",
  "status": 400,
  "title": "Bad Request",
  "errors": [
    {"field": "sensorId", "message": "may only contain letters, digits, '.', '_' and '-'"},
    {"field": "value", "message": "must not be null"}
  ]
}
```

Validation rules:
- `sensorId`: required, at most 64 characters, only letters, digits, `.`, `_`, `-` (in the body and in the path).
- `value`: required.
- `measuredAt`: optional (defaults to now), must not be in the future. A small amount of device clock drift is
  tolerated (`ingestion.validation.clock-drift-tolerance`, default `5m`).
- `from` must be before `to`.

> Readings are stored in PostgreSQL (schema managed by Flyway), so they survive pod restarts.
> The readiness probe includes the DB health check: if PostgreSQL is unreachable, pods are taken out of the Service but not restarted.

## Running the ingestion-service without Kubernetes

For a quick edit-run loop, start the service directly from Gradle. Only Docker needs to be running; minikube is not
needed:

```bash
./gradlew :ingestion-service:bootTestRun
```

This starts a throwaway PostgreSQL 18 container via Testcontainers (see `TestcontainersConfiguration`), points the
service at it, lets Flyway create the schema, and serves the API on http://localhost:8080. The curl examples and
the Swagger UI above work unchanged. The container and its data are removed when you stop the service (Ctrl+C).

## Tests

```bash
./gradlew unitTest          # unit tests and @WebMvcTest controller tests, no Docker needed
./gradlew integrationTest   # *IT classes, needs Docker
./gradlew test              # all tests, without building the jar
./gradlew build             # all tests (via check) + the jar
```

Integration tests are the classes named `*IT` (in `src/test`, next to the unit tests). They are `@SpringBootTest`s
that start a PostgreSQL 18 container via Testcontainers, so migrations and queries are tested against the same
database as in Kubernetes. Without Docker running, they fail at startup.
