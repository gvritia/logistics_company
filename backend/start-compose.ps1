$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $projectRoot '.env'
$composeFile = Join-Path $PSScriptRoot 'docker-compose.yml'

if (-not (Test-Path -LiteralPath $envFile)) {
    throw "Не найден $envFile. Скопируйте .env.example в .env и задайте пароль PostgreSQL."
}

docker compose --env-file $envFile -f $composeFile up -d --build --wait
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host 'Backend: http://localhost:8080/api/system/info'
Write-Host 'Health:  http://localhost:8080/actuator/health'
Write-Host 'Swagger: http://localhost:8080/swagger-ui.html'
