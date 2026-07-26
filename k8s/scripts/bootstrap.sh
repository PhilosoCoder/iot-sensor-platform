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

echo "Installing Strimzi operator..."
kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka --dry-run=client -o yaml | kubectl apply -f - 2>/dev/null || \
kubectl apply -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka

echo "Deploying infrastructure..."
kubectl apply -f k8s/infrastructure/postgresql/
kubectl apply -f k8s/infrastructure/redis/
kubectl apply -f k8s/infrastructure/kafka/

echo "Done! Run 'kubectl get pods -n iot-sensor-platform' and 'kubectl get pods -n kafka' to verify."
