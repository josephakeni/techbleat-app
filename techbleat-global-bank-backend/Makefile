REGISTRY      ?= jakeni
TAG           ?= latest
NETWORK        = techbleat-net
HOST_IP       ?= 192.168.0.10
FRONTEND      ?= http://$(HOST_IP):3000
FRONTEND_DIR  ?= ../techbleat-global-bank-frontend

# ── Images ────────────────────────────────────────────────────────────────────
IMAGE_USER     = $(REGISTRY)/techbleat-user-service:$(TAG)
IMAGE_TX       = $(REGISTRY)/techbleat-transaction-service:$(TAG)
IMAGE_ACT      = $(REGISTRY)/techbleat-activity-service:$(TAG)
IMAGE_FRONTEND = $(REGISTRY)/techbleat-frontend:$(TAG)

# ── Network ───────────────────────────────────────────────────────────────────
.PHONY: network
network:
	docker network inspect $(NETWORK) >/dev/null 2>&1 || \
	  docker network create $(NETWORK)

# ── Infrastructure (Postgres / Redis / Kafka) ─────────────────────────────────
.PHONY: postgres
postgres: network
	docker run -d \
	  --name postgres \
	  --network $(NETWORK) \
	  -e POSTGRES_DB=bankingdb \
	  -e POSTGRES_USER=bankuser \
	  -e POSTGRES_PASSWORD=bankpass \
	  -v postgres_data:/var/lib/postgresql/data \
	  -v $(PWD)/db-init/init.sql:/docker-entrypoint-initdb.d/init.sql \
	  -p 5432:5432 \
	  postgres:15

.PHONY: redis
redis: network
	docker run -d \
	  --name redis \
	  --network $(NETWORK) \
	  -p 6379:6379 \
	  redis:7

.PHONY: kafka
kafka: network
	docker run -d \
	  --name kafka \
	  --hostname kafka \
	  --network $(NETWORK) \
	  -e KAFKA_NODE_ID=1 \
	  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT \
	  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092 \
	  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
	  -e KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0 \
	  -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 \
	  -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
	  -e KAFKA_PROCESS_ROLES=broker,controller \
	  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@kafka:29093 \
	  -e KAFKA_LISTENERS=PLAINTEXT://kafka:29092,CONTROLLER://kafka:29093,PLAINTEXT_HOST://0.0.0.0:9092 \
	  -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT \
	  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
	  -e KAFKA_LOG_DIRS=/tmp/kraft-combined-logs \
	  -e CLUSTER_ID=MkU3OEVBNTcwNTJENDM2Qk \
	  -p 9092:9092 \
	  confluentinc/cp-kafka:7.4.6

.PHONY: infra
infra: postgres redis kafka

# ── Build ─────────────────────────────────────────────────────────────────────
.PHONY: build-user
build-user:
	docker build -t $(IMAGE_USER) ./user-service

.PHONY: build-transaction
build-transaction:
	docker build -t $(IMAGE_TX) ./transaction-service

.PHONY: build-activity
build-activity:
	docker build -t $(IMAGE_ACT) ./activity-service

.PHONY: build
build: build-user build-transaction build-activity

# ── Run ───────────────────────────────────────────────────────────────────────
.PHONY: run-user
run-user: network
	docker run -d \
	  --name user-service \
	  --network $(NETWORK) \
	  -e DATABASE_URL=postgresql://bankuser:bankpass@postgres:5432/bankingdb \
	  -e FRONTEND_ORIGIN=$(FRONTEND) \
	  -p 8000:8000 \
	  $(IMAGE_USER)

.PHONY: run-transaction
run-transaction: network
	docker run -d \
	  --name transaction-service \
	  --network $(NETWORK) \
	  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/bankingdb \
	  -e SPRING_DATASOURCE_USERNAME=bankuser \
	  -e SPRING_DATASOURCE_PASSWORD=bankpass \
	  -e SPRING_JPA_HIBERNATE_DDL_AUTO=update \
	  -e KAFKA_BOOTSTRAP_SERVERS=kafka:29092 \
	  -e REDIS_HOST=redis \
	  -e REDIS_PORT=6379 \
	  -e SERVER_PORT=8080 \
	  -e FRONTEND_ORIGIN=$(FRONTEND) \
	  -p 8080:8080 \
	  $(IMAGE_TX)

.PHONY: run-activity
run-activity: network
	docker run -d \
	  --name activity-service \
	  --network $(NETWORK) \
	  -e DATABASE_URL=postgresql://bankuser:bankpass@postgres:5432/bankingdb \
	  -e KAFKA_BOOTSTRAP_SERVERS=kafka:29092 \
	  -e FRONTEND_ORIGIN=$(FRONTEND) \
	  -p 8001:8001 \
	  $(IMAGE_ACT)

.PHONY: run
run: run-user run-transaction run-activity

# ── Frontend ──────────────────────────────────────────────────────────────────
.PHONY: build-frontend
build-frontend:
	docker build \
	  --build-arg VITE_USER_API=http://$(HOST_IP):8000 \
	  --build-arg VITE_TX_API=http://$(HOST_IP):8080 \
	  --build-arg VITE_ACTIVITY_API=http://$(HOST_IP):8001 \
	  -t $(IMAGE_FRONTEND) $(FRONTEND_DIR)

.PHONY: run-frontend
run-frontend: network
	docker run -d \
	  --name frontend \
	  --network $(NETWORK) \
	  -p 3000:80 \
	  $(IMAGE_FRONTEND)

# ── Full stack ────────────────────────────────────────────────────────────────
.PHONY: build-all
build-all: build build-frontend

.PHONY: deploy
deploy: infra run run-frontend

.PHONY: stop-frontend
stop-frontend:
	docker rm -f frontend 2>/dev/null || true

.PHONY: stop-all
stop-all: stop stop-frontend stop-infra
	docker network rm $(NETWORK) 2>/dev/null || true

# ── Kubernetes ────────────────────────────────────────────────────────────────
# Build frontend image with NodePort-based API URLs for Kubernetes
# VITE_* vars are baked in at build time — must match NodePorts in k8s/apps/*.yaml
.PHONY: build-frontend-k8s
build-frontend-k8s:
	docker build \
	  --build-arg VITE_USER_API=http://$(HOST_IP):30800 \
	  --build-arg VITE_TX_API=http://$(HOST_IP):30808 \
	  --build-arg VITE_ACTIVITY_API=http://$(HOST_IP):30801 \
	  -t $(IMAGE_FRONTEND) $(FRONTEND_DIR)

.PHONY: k8s-deploy
k8s-deploy:
	kubectl apply -f k8s/namespace.yaml
	kubectl apply -f k8s/secret.yaml
	kubectl apply -f k8s/configmap.yaml
	kubectl apply -f k8s/infra/postgres.yaml
	kubectl apply -f k8s/infra/redis.yaml
	kubectl apply -f k8s/infra/kafka.yaml
	kubectl apply -f k8s/apps/user-service.yaml
	kubectl apply -f k8s/apps/transaction-service.yaml
	kubectl apply -f k8s/apps/activity-service.yaml
	kubectl apply -f k8s/apps/frontend.yaml

.PHONY: k8s-status
k8s-status:
	kubectl get pods,svc -n techbleat

.PHONY: k8s-delete
k8s-delete:
	kubectl delete namespace techbleat

# ── Push to registry ──────────────────────────────────────────────────────────
.PHONY: push
push:
	docker push $(IMAGE_USER)
	docker push $(IMAGE_TX)
	docker push $(IMAGE_ACT)

# ── Logs ──────────────────────────────────────────────────────────────────────
.PHONY: logs-user logs-transaction logs-activity
logs-user:
	docker logs -f user-service

logs-transaction:
	docker logs -f transaction-service

logs-activity:
	docker logs -f activity-service

# ── Stop & clean ──────────────────────────────────────────────────────────────
.PHONY: stop
stop:
	docker rm -f user-service transaction-service activity-service 2>/dev/null || true

.PHONY: stop-infra
stop-infra:
	docker rm -f postgres redis kafka 2>/dev/null || true

.PHONY: stop-all
stop-all: stop stop-infra
	docker network rm $(NETWORK) 2>/dev/null || true

.PHONY: restart
restart: stop run
