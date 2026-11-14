.PHONY: build test test-java up down demo smoke report docker-up docker-down docker-reset docker-smoke docker-logs
SERVICES_JAVA = auth-service class-service

# Build everything. `mvnw verify` runs the Spring unit tests too (JaCoCo writes target/site/jacoco).
build:
	cd services/gateway && uv sync --locked && uv build --out-dir dist
	for service in $(SERVICES_JAVA); do (cd services/$$service && ./mvnw -B -ntp verify) || exit 1; done
	cd web && npm ci && npm run build

test-java:
	for service in $(SERVICES_JAVA); do (cd services/$$service && ./mvnw -B -ntp verify) || exit 1; done

test:
	python3 -m unittest discover -s scripts -p 'test_*.py'
	cd services/gateway && uv run --locked pytest -q
	cd web && npm test && npm run lint
	@echo "Spring services: make test-java (or make build)"

up:
	python3 scripts/native.py up
down:
	python3 scripts/native.py down

# Demo accounts and a class with its invite code, a student already joined.
# Needs `make up` first.
demo:
	python3 scripts/demo.py
# Auth checks and the class flow (create, invite code, join). Needs `make up` first.
smoke:
	python3 scripts/smoke.py

# Test results and coverage of every part, packaged into reports/ (see reports/README.md).
report: test-java
	cd services/gateway && uv run --locked pytest -q --junitxml=report.xml --cov=gateway --cov-report=xml:coverage.xml --cov-report=html:htmlcov
	cd web && npx vitest run --reporter=junit --outputFile=vitest-report.xml
	python3 scripts/report.py

# Docker (Docker Desktop on macOS/Windows, Docker Engine on Linux). Same ports as native: run only one of the two.
# docker-up builds, starts everything, waits until healthy and until demo-seed has made the demo accounts.
docker-up:
	docker compose up -d --build --wait
	docker compose wait demo-seed
	docker compose logs --no-log-prefix demo-seed
docker-smoke:
	docker compose run --rm --no-deps demo-seed python smoke.py --api http://web/api --auth http://auth-service:8081
docker-logs:
	docker compose logs -f --tail=100
# Stops the containers, keeps the database volume
docker-down:
	docker compose down
# Stops the containers and deletes the database volume (next docker-up starts from an empty database)
docker-reset:
	docker compose down -v
