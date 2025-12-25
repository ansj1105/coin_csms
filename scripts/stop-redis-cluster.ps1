# Redis Cluster Stop Script (PowerShell)
# Usage: .\scripts\stop-redis-cluster.ps1

# Set console encoding to UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "Stopping Redis Cluster..." -ForegroundColor Yellow

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

if ($dockerComposeCmd -eq "docker-compose") {
    & docker-compose -f docker-compose.cluster.yml down
} else {
    & docker compose -f docker-compose.cluster.yml down
}

Write-Host "Redis Cluster stopped!" -ForegroundColor Green

