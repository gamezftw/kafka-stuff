#!/usr/bin/env zsh

k3d cluster delete mycluster
k3d cluster create --config $DEVBOX_CONFIG_DIR/k3d/config.yaml
kubectl create namespace kafka
kubectl config set-context --current --namespace=kafka
kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
helm repo add kafbat-ui https://kafbat.github.io/helm-charts
helm install kafbat-ui kafbat-ui/kafka-ui -f $DEVBOX_PROJECT_ROOT/infra/helm/values.yml
kubectl apply -f $DEVBOX_PROJECT_ROOT/infra/kafka-single-node.yaml
kubectl apply -f $DEVBOX_PROJECT_ROOT/infra/helm/ingress.yml

kubectl wait deployment/strimzi-cluster-operator --for=condition=available --timeout=300s
kubectl wait svc/my-cluster-kafka-ext-bootstrap --for=jsonpath='{.spec.ports[0].nodePort}'
export KAFKA_PORT=$(kubectl get svc my-cluster-kafka-ext-bootstrap -o jsonpath="{.spec.ports[0].nodePort}")
echo $KAFKA_PORT
k3d node edit k3d-mycluster-serverlb --port-add 39094:$KAFKA_PORT
# yq ".contexts.default.brokers[0] = \"$KAFKA_PORT\"" $DEVBOX_CONFIG_DIR/kafkactl/config.yml -i -y
