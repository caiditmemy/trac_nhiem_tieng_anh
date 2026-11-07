.PHONY: build test up down demo smoke
build:
	cd services/auth-service && ./mvnw -B -ntp verify
	cd web && npm ci && npm run build
test:
	python3 -m unittest discover -s scripts -p 'test_*.py'
	cd services/gateway && uv run --locked pytest -q
	cd web && npm test && npm run lint
up:
	python3 scripts/native.py up
down:
	python3 scripts/native.py down
demo:
	python3 scripts/smoke.py --demo
smoke:
	python3 scripts/smoke.py
