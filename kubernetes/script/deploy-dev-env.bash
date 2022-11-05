#!/usr/bin/env bash

# Print commands to the terminal before execution and stop the script if any error occurs
# https://frankler.tistory.com/59
set -ex # -e 명령어는 스크립트에서 수행하는 명령어가 실패하면, 더이상 다음 스크립트를 수행하지 않는다,
        # -x 는 -e와 함꼐 줄 경우, 어디에서 종료되었는 지 알기 쉽게 디버깅 가능

# 마이크로서비스 별로 컨피그 맵을 생성
kubectl create configmap config-repo-auth-server       --from-file=config-repo/application.yml --from-file=config-repo/auth-server.yml --save-config
kubectl create configmap config-repo-gateway           --from-file=config-repo/application.yml --from-file=config-repo/gateway.yml --save-config
kubectl create configmap config-repo-product-composite --from-file=config-repo/application.yml --from-file=config-repo/product-composite.yml --save-config
kubectl create configmap config-repo-product           --from-file=config-repo/application.yml --from-file=config-repo/product.yml --save-config
kubectl create configmap config-repo-recommendation    --from-file=config-repo/application.yml --from-file=config-repo/recommendation.yml --save-config
kubectl create configmap config-repo-review            --from-file=config-repo/application.yml --from-file=config-repo/review.yml --save-config

# resource manager를 위한 시크릿
kubectl create secret generic rabbitmq-server-credentials \
    --from-literal=RABBITMQ_DEFAULT_USER=rabbit-user-dev \
    --from-literal=RABBITMQ_DEFAULT_PASS=rabbit-pwd-dev \
    --save-config

kubectl create secret generic rabbitmq-credentials \
    --from-literal=SPRING_RABBITMQ_USERNAME=rabbit-user-dev \
    --from-literal=SPRING_RABBITMQ_PASSWORD=rabbit-pwd-dev \
    --save-config

kubectl create secret generic rabbitmq-zipkin-credentials \
    --from-literal=RABBIT_USER=rabbit-user-dev \
    --from-literal=RABBIT_PASSWORD=rabbit-pwd-dev \
    --save-config

kubectl create secret generic mongodb-server-credentials \
    --from-literal=MONGO_INITDB_ROOT_USERNAME=mongodb-user-dev \
    --from-literal=MONGO_INITDB_ROOT_PASSWORD=mongodb-pwd-dev \
    --save-config

kubectl create secret generic mongodb-credentials \
    --from-literal=SPRING_DATA_MONGODB_AUTHENTICATION_DATABASE=admin \
    --from-literal=SPRING_DATA_MONGODB_USERNAME=mongodb-user-dev \
    --from-literal=SPRING_DATA_MONGODB_PASSWORD=mongodb-pwd-dev \
    --save-config

kubectl create secret generic mysql-server-credentials \
    --from-literal=MYSQL_ROOT_PASSWORD=rootpwd \
    --from-literal=MYSQL_DATABASE=review-db \
    --from-literal=MYSQL_USER=mysql-user-dev \
    --from-literal=MYSQL_PASSWORD=mysql-pwd-dev \
    --save-config

kubectl create secret generic mysql-credentials \
    --from-literal=SPRING_DATASOURCE_USERNAME=mysql-user-dev \
    --from-literal=SPRING_DATASOURCE_PASSWORD=mysql-pwd-dev \
    --save-config

kubectl create secret tls tls-certificate --key kubernetes/cert/tls.key --cert kubernetes/cert/tls.crt

# First deploy the resource managers and wait for their pods to become ready
kubectl apply -f kubernetes/services/overlays/dev/rabbitmq-dev.yml
kubectl apply -f kubernetes/services/overlays/dev/mongodb-dev.yml
kubectl apply -f kubernetes/services/overlays/dev/mysql-dev.yml
kubectl wait --timeout=600s --for=condition=ready pod --all

# Next deploy the microservices and wait for their pods to become ready
kubectl apply -k kubernetes/services/overlays/dev
kubectl wait --timeout=600s --for=condition=ready pod --all

set +ex