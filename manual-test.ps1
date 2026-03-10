Write-Host "--- Phase 1: Triggering Circuit Breaker (15 requests) ---" -ForegroundColor Cyan
1..15 | ForEach-Object { 
    $out = Invoke-RestMethod -Uri "http://localhost:8080/api/hello"
    if ($out -match "Fallback") { Write-Host "Req $_ : $out" -ForegroundColor Red }
    else { Write-Host "Req $_ : $out" -ForegroundColor Green }
    Start-Sleep -Milliseconds 200
}

Write-Host "`n--- Phase 2: Waiting for 10s (Circuit is OPEN - All requests would fail) ---" -ForegroundColor Yellow
Start-Sleep -Seconds 10
Write-Host "Done waiting.`n"

Write-Host "--- Phase 3: Testing Half-Open (5 requests) ---" -ForegroundColor Cyan
1..5 | ForEach-Object { 
    $out = Invoke-RestMethod -Uri "http://localhost:8080/api/hello"
    if ($out -match "Fallback") { Write-Host "Req $_ : $out" -ForegroundColor Red }
    else { Write-Host "Req $_ : $out" -ForegroundColor Green }
    Start-Sleep -Milliseconds 200
}

1..20 | ForEach-Object { Write-Host "Req ${$_}: " -NoNewline; curl.exe -s http://localhost:8080/api/hello; Write-Host ""; Start-Sleep -Milliseconds 200 }
