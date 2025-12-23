#!/usr/bin/env zsh

k3d cluster delete mycluster
k3d cluster create --config $DEVBOX_CONFIG_DIR/k3d/config.yaml
kubectl create namespace kafka
kubectl config set-context --current --namespace=kafka
kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
$DEVBOX_PROJECT_ROOT/helm/linux-amd64/helm repo add kafbat-ui https://kafbat.github.io/helm-charts
$DEVBOX_PROJECT_ROOT/helm/linux-amd64/helm install kafbat-ui kafbat-ui/kafka-ui -f $DEVBOX_PROJECT_ROOT/infra/helm/values.yml
kubectl apply -f $DEVBOX_PROJECT_ROOT/infra/kafka-single-node.yaml
kubectl apply -f $DEVBOX_PROJECT_ROOT/infra/helm/ingress.yml

