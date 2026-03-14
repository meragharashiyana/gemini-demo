@echo off
REM Start all project Docker services (Redis, Prometheus, Zipkin)

echo Starting Docker services...
docker-compose up -d
echo Docker services are starting or already running.
docker-compose ps