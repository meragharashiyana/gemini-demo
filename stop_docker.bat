@echo off
REM Stop all project Docker services started by docker-compose

echo Stopping Docker services...
docker-compose down
echo Docker services stopped.
