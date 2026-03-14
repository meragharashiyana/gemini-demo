@echo off
REM Stop the Spring Boot app started by start.bat (kills process bound to port 8080)

echo Stopping application running on port 8080...
powershell -NoProfile -Command "try { $pid = (Get-NetTCPConnection -LocalPort 8080 -State Listen | Select-Object -First 1 -ExpandProperty OwningProcess); if ($pid) { Stop-Process -Id $pid -Force; Write-Host 'Stopped process:' $pid } else { Write-Host 'No process found listening on port 8080.' } } catch { Write-Warning \"Failed to stop process: $_\" }"
echo Done.
