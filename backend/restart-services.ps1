# Quick Service Restart Script
# Run this script to stop all Java processes and restart services

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Service Restart Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Stop all Java processes
Write-Host "[1/5] Stopping all Java processes..." -ForegroundColor Yellow
$javaProcesses = Get-Process -Name java -ErrorAction SilentlyContinue
if ($javaProcesses) {
    $javaProcesses | Stop-Process -Force
    Write-Host "   ✓ Stopped $($javaProcesses.Count) Java process(es)" -ForegroundColor Green
    Start-Sleep -Seconds 2
} else {
    Write-Host "   ✓ No Java processes running" -ForegroundColor Green
}

# Step 2: Verify ports are free
Write-Host ""
Write-Host "[2/5] Checking if ports are free..." -ForegroundColor Yellow
$ports = @(8761, 9090, 18081, 18083)
foreach ($port in $ports) {
    $connection = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($connection) {
        Write-Host "   ⚠ Port $port is still in use!" -ForegroundColor Red
    } else {
        Write-Host "   ✓ Port $port is free" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "[3/5] Services have been stopped." -ForegroundColor Green
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Next Steps - Start Services in IntelliJ" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Please start the services in this order:" -ForegroundColor White
Write-Host ""
Write-Host "1. discovery-service   (port 8761)" -ForegroundColor Cyan
Write-Host "   Wait for: 'Started DiscoveryServiceApplication'" -ForegroundColor Gray
Write-Host ""
Write-Host "2. api-gateway        (port 9090)" -ForegroundColor Cyan
Write-Host "   Wait for: 'Started ApiGatewayApplication'" -ForegroundColor Gray
Write-Host ""
Write-Host "3. user-service       (port 18081)" -ForegroundColor Cyan
Write-Host "   Wait for: 'Started UserServiceApplication'" -ForegroundColor Gray
Write-Host "   LOOK FOR: Log messages about 'Fetching users with role'" -ForegroundColor Yellow
Write-Host ""
Write-Host "4. tracking-service   (port 18083) [Optional]" -ForegroundColor Cyan
Write-Host "   Wait for: 'Started TrackingServiceApplication'" -ForegroundColor Gray
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "After starting, verify:" -ForegroundColor White
Write-Host "• Eureka: http://localhost:8761" -ForegroundColor Cyan
Write-Host "• Test:   http://localhost:9090/api/users/role/patient" -ForegroundColor Cyan
Write-Host ""
Write-Host "Press any key to exit..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

