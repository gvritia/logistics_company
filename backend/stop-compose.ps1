$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $projectRoot '.env'
$composeFile = Join-Path $PSScriptRoot 'docker-compose.yml'

docker compose --env-file $envFile -f $composeFile down
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
