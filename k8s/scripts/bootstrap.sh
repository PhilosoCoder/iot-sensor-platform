#!/bin/bash
set -e

echo "Creating namespaces..."
kubectl create namespace iot-sensor-platform --dry-run=client -o yaml | kubectl apply -f -
kubectl create namespace kafka --dry-run=client -o yaml | kubectl apply -f -

echo "Creating secrets..."
kubectl create secret generic postgresql-secret \
  --namespace iot-sensor-platform \
  --from-literal=POSTGRES_PASSWORD=postgres123 \
  --from-literal=POSTGRES_USER=postgres \
  --from-literal=POSTGRES_DB=iot_sensor_platform \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic redis-secret \
  --namespace iot-sensor-platform \
  --from-literal=REDIS_PASSWORD=redis123 \
  --dry-run=client -o yaml | kubectl apply -f -

STRIMZI_VERSION=1.3.1
echo "Installing Strimzi operator ${STRIMZI_VERSION}..."
# strimzi.io/install only serves the latest release. To pin the version, use the release manifest and replace its
# placeholder namespace (myproject) in the RoleBindings, which is what strimzi.io/install does.
STRIMZI_DIR=$(mktemp -d)
trap 'rm -rf "$STRIMZI_DIR"' EXIT
curl -sSfL -o "$STRIMZI_DIR/release.yaml" \
  "https://github.com/strimzi/strimzi-kafka-operator/releases/download/${STRIMZI_VERSION}/strimzi-cluster-operator-${STRIMZI_VERSION}.yaml"
sed 's/namespace: myproject/namespace: kafka/' "$STRIMZI_DIR/release.yaml" > "$STRIMZI_DIR/strimzi.yaml"
kubectl create -f "$STRIMZI_DIR/strimzi.yaml" -n kafka --dry-run=client -o yaml | kubectl apply -n kafka -f - 2>/dev/null || \
kubectl apply -f "$STRIMZI_DIR/strimzi.yaml" -n kafka

echo "Deploying infrastructure..."
kubectl apply -f k8s/infrastructure/postgresql/
kubectl apply -f k8s/infrastructure/redis/
kubectl apply -f k8s/infrastructure/kafka/

echo "Done! Run 'kubectl get pods -n iot-sensor-platform' and 'kubectl get pods -n kafka' to verify."
