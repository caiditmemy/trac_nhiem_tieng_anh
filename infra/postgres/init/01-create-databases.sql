-- Runs only on the first start of an empty data volume.
-- To re-run it: docker compose down -v (this deletes all data).
-- One database per service: no service reads another service's tables. Sprint 1 has only auth-service.
CREATE DATABASE auth_db;
