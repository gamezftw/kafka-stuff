#!/usr/bin/env zsh
# export POD_NAME=$(kubectl get pods --namespace kafka -l "app.kubernetes.io/name=kafka-ui,app.kubernetes.io/instance=kafbat-ui" -o jsonpath="{.items[0].metadata.name}")
# echo "Visit http://127.0.0.1:8080 to use your application"
# kubectl --namespace kafka port-forward $POD_NAME 8080:8080
kubectl --namespace kafka port-forward svc/kafbat-ui-kafka-ui 8080:80


