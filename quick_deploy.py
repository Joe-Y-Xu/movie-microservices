#!/bin/bash
set -e

echo "Building Docker images..."
docker build -t movieinfo-service:v1 ./movieinfo-service/
docker build -t artifact-catalog-service:v1 ./artifact-catalog-service/
docker build -t ratingdata-service:v1 ./ratingdata-service/

echo "Deploying to Kubernetes..."
kubectl apply -f ./movieinfo-service/deployment.yaml
kubectl apply -f ./movieinfo-service/service.yaml
kubectl apply -f ./artifact-catalog-service/deployment.yaml
kubectl apply -f ./artifact-catalog-service/service.yaml
kubectl apply -f ./ratingdata-service/deployment.yaml
kubectl apply -f ./ratingdata-service/service.yaml

echo "All services deployed!"
kubectl get pods
kubectl get services
