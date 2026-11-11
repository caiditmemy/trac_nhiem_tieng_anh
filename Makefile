.PHONY: build test test-java up down demo smoke report
SERVICES_JAVA = auth-service class-service exercise-service

# Build everything. `mvnw verify` runs the Spring unit tests too (JaCoCo writes target/site/jacoco).
build:
	cd services/gateway && uv sync --locked && uv build --out-dir dist
	cd services/learning-service && uv sync --locked && uv build --out-dir dist
	for service in $(SERVICES_JAVA); do (cd services/$$service && ./mvnw -B -ntp verify) || exit 1; done
	cd web && npm ci && npm run build

test-java:
	for service in $(SERVICES_JAVA); do (cd services/$$service && ./mvnw -B -ntp verify) || exit 1; done

test:
	python3 -m unittest discover -s scripts -p 'test_*.py'
	cd services/gateway && uv run --locked pytest -q
	cd services/learning-service && uv run --locked pytest -q
	cd web && npm test && npm run lint
	@echo "Spring services: make test-java (or make build)"

up:
	python3 scripts/native.py up
down:
	python3 scripts/native.py down

# Demo accounts, question bank + vocabulary (scripts/seed_data.json), a class with its invite code, an exercise.
# Needs `make up` first.
demo:
	python3 scripts/demo.py
# Auth checks and the week 7 end-to-end flow. Needs `make up` first.
smoke:
	python3 scripts/smoke.py

# Test results and coverage of every part, packaged into reports/ (see reports/README.md).
report: test-java
	cd services/gateway && uv run --locked pytest -q --junitxml=report.xml --cov=gateway --cov-report=xml:coverage.xml --cov-report=html:htmlcov
	cd services/learning-service && uv run --locked pytest -q --junitxml=report.xml --cov=learning_service --cov-report=xml:coverage.xml --cov-report=html:htmlcov
	cd web && npx vitest run --reporter=junit --outputFile=vitest-report.xml
	python3 scripts/report.py
