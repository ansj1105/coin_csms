# Redis Cluster Start Script (PowerShell)
# Usage: .\scripts\start-redis-cluster.ps1

# Set console encoding to UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "Starting Redis Cluster..." -ForegroundColor Green

# Check if Docker is installed
try {
    $null = docker --version 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Docker not found"
    }
} catch {
    Write-Host "ERROR: Docker is not installed or not in PATH" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please install Docker Desktop:" -ForegroundColor Yellow
    Write-Host "  1. Download from: https://www.docker.com/products/docker-desktop" -ForegroundColor Cyan
    Write-Host "  2. Install Docker Desktop" -ForegroundColor Cyan
    Write-Host "  3. Start Docker Desktop application" -ForegroundColor Cyan
    Write-Host "  4. Wait for Docker to be ready (whale icon in system tray)" -ForegroundColor Cyan
    Write-Host "  5. Run this script again" -ForegroundColor Cyan
    Write-Host ""
    exit 1
}

# Check Docker Compose command (docker compose or docker-compose)
$dockerComposeCmd = $null
try {
    $null = docker-compose --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        $dockerComposeCmd = "docker-compose"
    } else {
        throw "docker-compose not found"
    }
} catch {
    try {
        $null = docker compose version 2>&1
        if ($LASTEXITCODE -eq 0) {
            $dockerComposeCmd = "docker compose"
        } else {
            throw "docker compose not found"
        }
    } catch {
        Write-Host "ERROR: docker-compose or docker compose is not available" -ForegroundColor Red
        exit 1
    }
}

# Clean existing cluster data (optional)
if ($args[0] -eq "--clean") {
    Write-Host "Cleaning up existing cluster data..." -ForegroundColor Yellow
    $nodeDirs = @("node-1", "node-2", "node-3", "node-4", "node-5", "node-6")
    foreach ($nodeDir in $nodeDirs) {
        $path = Join-Path "redis-cluster" $nodeDir
        if (Test-Path $path) {
            Remove-Item -Path $path -Recurse -Force -ErrorAction SilentlyContinue
        }
        New-Item -ItemType Directory -Path $path -Force | Out-Null
    }
}

# Start Redis Cluster with Docker Compose
Write-Host "Starting Redis Cluster containers..." -ForegroundColor Cyan
if ($dockerComposeCmd -eq "docker-compose") {
    & docker-compose -f docker-compose.cluster.yml up -d
} else {
    & docker compose -f docker-compose.cluster.yml up -d
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to start Redis Cluster" -ForegroundColor Red
    exit 1
}

# Wait for cluster initialization
Write-Host "Waiting for cluster initialization..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Check cluster status
Write-Host "Checking cluster status..." -ForegroundColor Cyan
try {
    docker exec redis-node-1 redis-cli -p 7001 cluster info | Select-String "cluster_state"
    Write-Host ""
    Write-Host "Cluster nodes:" -ForegroundColor Green
    docker exec redis-node-1 redis-cli -p 7001 cluster nodes
} catch {
    Write-Host "WARNING: Cluster may still be initializing, please wait a few more seconds" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Redis Cluster is ready!" -ForegroundColor Green
Write-Host ""
Write-Host "Connection info:" -ForegroundColor Cyan
Write-Host "   - Node 1: localhost:7001"
Write-Host "   - Node 2: localhost:7002"
Write-Host "   - Node 3: localhost:7003"
Write-Host "   - Node 4: localhost:7004"
Write-Host "   - Node 5: localhost:7005"
Write-Host "   - Node 6: localhost:7006"
Write-Host ""
Write-Host "To use cluster mode, set 'mode': 'cluster' in config.json" -ForegroundColor Yellow

