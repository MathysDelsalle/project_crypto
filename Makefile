.PHONY: test test-api test-collector \
        up down logs \
        perf perf-clean perf-wait perf-steps perf-soak perf-prune \
        start stop status logs-api logs-front logs-collector \
        mk-docker-env build-k8s deploy restart wait images reset

# --------------------------
# Perf config
# --------------------------
PERF_DIR := perf
PERF_RESULTS_DIR := perf-results
STAMP = $(shell date +%Y%m%d_%H%M%S)

START_RPS ?= 50
END_RPS ?= 400
STEP_RPS ?= 50
STEP_DURATION ?= 2m
SOAK_RPS ?= 150
KEEP ?= 10

API_URL := https://api.crypto.local
FRONT_URL := https://front.crypto.local
COLLECTOR_URL := http://collector.crypto.local

K6_IMAGE := grafana/k6:latest

# --------------------------
# Kubernetes / Minikube config
# --------------------------
NAMESPACE := project-crypto
K8S_DIR := k8s

# Images (DOIVENT matcher exactement les champs "image:" dans tes YAML k8s)
API_IMAGE ?= project-crypto-api:latest
FRONT_IMAGE ?= project-crypto-front:latest
COLLECTOR_IMAGE ?= project-crypto-collector:latest


# Build contexts (dossiers contenant les Dockerfile)
API_DIR ?= api
FRONT_DIR ?= front
COLLECTOR_DIR ?= collector

# --------------------------
# Tests (unit + integ) Maven
# --------------------------
test: test-api test-collector

test-api:
	mvn -f api/pom.xml clean verify jacoco:report

test-collector:
	mvn -f collector/pom.xml clean verify jacoco:report

# --------------------------
# Docker Compose helpers (restent dispo si tu veux)
# --------------------------
up:
	docker compose up -d --build

down:
	docker compose down -v

logs:
	docker compose logs -f --tail=200

# --------------------------
# Perf helpers
# --------------------------
perf-clean: perf-prune
	@mkdir -p $(PERF_RESULTS_DIR)

# IMPORTANT: ne pas exit 0 au 1er check, sinon les checks suivants ne tournent pas
perf-wait:
	@echo "Waiting for API on $(API_URL)/api/cryptos ..."
	@bash -lc 'for i in {1..60}; do curl -fsS "$(API_URL)/api/cryptos" >/dev/null && break; sleep 2; done; curl -fsS "$(API_URL)/api/cryptos" >/dev/null || (echo "API not ready"; exit 1)'
	@echo "Waiting for FRONT on $(FRONT_URL) ..."
	@bash -lc 'for i in {1..60}; do code=$$(curl -s -o /dev/null -w "%{http_code}" "$(FRONT_URL)/"); if [ "$$code" = "200" ] || [ "$$code" = "302" ] || [ "$$code" = "403" ]; then exit 0; fi; sleep 2; done; echo "FRONT not ready (last HTTP $$code)"; exit 1'
	@echo "Waiting for COLLECTOR on $(COLLECTOR_URL) (health) ..."
	@bash -lc 'for i in {1..60}; do curl -fsS "$(COLLECTOR_URL)/actuator/health" >/dev/null && break; curl -fsS "$(COLLECTOR_URL)/health" >/dev/null && break; sleep 2; done; (curl -fsS "$(COLLECTOR_URL)/actuator/health" >/dev/null || curl -fsS "$(COLLECTOR_URL)/health" >/dev/null) || (echo "COLLECTOR not ready"; exit 1)'

# --------------------------
# Perf tests
# --------------------------
perf: perf-clean perf-steps perf-soak
	@echo ""
	@echo "✅ Perf results generated:"
	@echo " - $(PERF_RESULTS_DIR)/$(STAMP)_steps/results.json"
	@echo " - $(PERF_RESULTS_DIR)/$(STAMP)_steps/summary.json"
	@echo " - $(PERF_RESULTS_DIR)/$(STAMP)_steps/report.html"
	@echo " - $(PERF_RESULTS_DIR)/$(STAMP)_soak/results.json"
	@echo " - $(PERF_RESULTS_DIR)/$(STAMP)_soak/summary.json"
	@echo " - $(PERF_RESULTS_DIR)/$(STAMP)_soak/report.html"

perf-steps: perf-wait
	@mkdir -p $(PERF_RESULTS_DIR)/$(STAMP)_steps
	@echo "Running k6 steps..."
	docker run --rm --network=host \
		--user $$(id -u):$$(id -g) \
		-v "$(PWD)/$(PERF_DIR):/scripts:ro" \
		-v "$(PWD)/$(PERF_RESULTS_DIR)/$(STAMP)_steps:/results" \
		-e API_URL="$(API_URL)" \
		-e COLLECTOR_URL="$(COLLECTOR_URL)" \
		-e FRONT_URL="$(FRONT_URL)" \
		-e START_RPS="$(START_RPS)" \
		-e END_RPS="$(END_RPS)" \
		-e STEP_RPS="$(STEP_RPS)" \
		-e STEP_DURATION="$(STEP_DURATION)" \
		$(K6_IMAGE) run \
			--out json=/results/results.json \
			--summary-export=/results/summary.json \
			/scripts/perf-load-stress-steps.js

perf-soak: perf-wait
	@mkdir -p $(PERF_RESULTS_DIR)/$(STAMP)_soak
	@echo "Running k6 soak 30m..."
	docker run --rm --network=host \
		--user $$(id -u):$$(id -g) \
		-v "$(PWD)/$(PERF_DIR):/scripts:ro" \
		-v "$(PWD)/$(PERF_RESULTS_DIR)/$(STAMP)_soak:/results" \
		-e API_URL="$(API_URL)" \
		-e COLLECTOR_URL="$(COLLECTOR_URL)" \
		-e FRONT_URL="$(FRONT_URL)" \
		-e SOAK_RPS="$(SOAK_RPS)" \
		$(K6_IMAGE) run \
			--out json=/results/results.json \
			--summary-export=/results/summary.json \
			/scripts/perf-soak-30m.js

perf-prune:
	@mkdir -p $(PERF_RESULTS_DIR)
	@echo "Keeping last $(KEEP) perf runs in $(PERF_RESULTS_DIR) ..."
	@bash -lc 'set -e; \
	  dirs=$$(ls -1dt "$(PERF_RESULTS_DIR)"/20??????_?????? 2>/dev/null || true); \
	  count=$$(echo "$$dirs" | sed "/^$$/d" | wc -l); \
	  if [ "$$count" -gt "$(KEEP)" ]; then \
	    echo "$$dirs" | tail -n +$$(( $(KEEP) + 1 )) | xargs -r rm -rf; \
	  fi'

# --------------------------
# Kubernetes / Minikube helpers
# --------------------------

# Affiche l'environnement docker de minikube (info)
mk-docker-env:
	@echo "🔧 Using minikube docker daemon:"
	@minikube -p minikube docker-env

# Build des images dans le Docker de Minikube (résout ImagePullBackOff en dev)
build:
	@echo "🔨 Building images inside minikube..."
	@bash -lc 'eval "$$(minikube -p minikube docker-env)" && \
	  docker build -t "$(API_IMAGE)" "$(API_DIR)" && \
	  docker build -t "$(FRONT_IMAGE)" "$(FRONT_DIR)" && \
	  docker build -t "$(COLLECTOR_IMAGE)" "$(COLLECTOR_DIR)"'

# Applique les manifests k8s (crée le namespace si besoin)
deploy:
	@kubectl get namespace $(NAMESPACE) >/dev/null 2>&1 || kubectl create namespace $(NAMESPACE)
	kubectl apply -f $(K8S_DIR)/

# Restart les deployments (utile après rebuild)
restart:
	kubectl rollout restart deploy -n $(NAMESPACE)

# Attend que les rollouts soient OK
wait:
	kubectl rollout status deploy/api -n $(NAMESPACE) --timeout=180s
	kubectl rollout status deploy/front -n $(NAMESPACE) --timeout=180s
	kubectl rollout status deploy/collector -n $(NAMESPACE) --timeout=180s

# Affiche les images présentes DANS minikube
images:
	@bash -lc 'eval "$$(minikube -p minikube docker-env)" && docker images | head -n 50'

# Supprime le namespace (reset "soft" côté k8s, sans supprimer minikube)
reset:
	kubectl delete namespace $(NAMESPACE) --ignore-not-found

# Démarrage complet: minikube + build images + apply + restart + wait
start:
	minikube start
	$(MAKE) deploy
	$(MAKE) restart
	$(MAKE) wait

stop:
	minikube stop

status:
	kubectl get pods -n $(NAMESPACE)

logs-api:
	kubectl -n $(NAMESPACE) logs deploy/api -f

logs-front:
	kubectl -n $(NAMESPACE) logs deploy/front -f

logs-collector:
	kubectl -n $(NAMESPACE) logs deploy/collector -f
