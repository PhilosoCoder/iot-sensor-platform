# iot-sensor-platform
IoT Sensor Monitoring Platform — A real-time data ingestion and processing system for IoT sensor telemetry. Ingests high-volume sensor readings via Kafka, processes and aggregates data using Kafka Streams, detects anomalies, and streams live updates to clients. Built with Java 25, Spring Boot, and deployed on Kubernetes/AKS.

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
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
strimzi-cluster-operator-xxx                1/1     Running   0          ...
```

### 4. Build the ingestion-service image and load it into minikube

The jar is built on the host, the Dockerfile only packages it (layered, non-root, container-aware JVM).

```bash
./gradlew :ingestion-service:bootJar
docker build -t ingestion-service:0.0.1-SNAPSHOT ingestion-service
minikube image load ingestion-service:0.0.1-SNAPSHOT
```

Verify the image is available inside minikube:

```bash
minikube image ls | grep ingestion-service
```

> Use a fixed tag (not `latest`): with `latest`, Kubernetes defaults to `imagePullPolicy: Always` and tries to pull from Docker Hub instead of using the loaded image.

### 5. Deploy the ingestion-service

```bash
kubectl apply -f k8s/apps/ingestion-service/
kubectl rollout status deployment/ingestion-service -n iot-sensor-platform
```

After rebuilding and re-loading the image under the same tag, restart the pods so they pick it up:

```bash
kubectl rollout restart deployment/ingestion-service -n iot-sensor-platform
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

curl localhost:8080/v1/readings/sensor-1
```

> Storage is still in-memory (H2) at this stage: readings are lost when the pod restarts.
