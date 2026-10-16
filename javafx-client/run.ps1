param(
    [string]$MavenPath = 'mvn.cmd'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot '.env'

if (-not (Test-Path -LiteralPath $envFile)) {
    throw 'Не найден .env в корне проекта. Скопируйте .env.example в .env и задайте параметры PostgreSQL.'
}
if (-not (Get-Command $MavenPath -ErrorAction SilentlyContinue)) {
    throw 'Maven не найден. Установите Maven 3.9+ или передайте путь через -MavenPath.'
}

Push-Location $projectRoot
try {
    docker compose --env-file .env -f infra/docker/compose.kr2.yml up -d --wait
    if ($LASTEXITCODE -ne 0) { throw 'Не удалось запустить PostgreSQL через Docker Compose.' }

    # JavaFX-клиент зависит от артефакта КР1: сначала собираем корневой Maven-проект.
    & $MavenPath -B install
    if ($LASTEXITCODE -ne 0) { throw 'Не удалось собрать Maven-проект КР1.' }

    Push-Location (Join-Path $projectRoot 'javafx-client')
    try {
        & $MavenPath -B javafx:run
        if ($LASTEXITCODE -ne 0) { throw 'Не удалось запустить JavaFX-клиент.' }
    }
    finally {
        Pop-Location
    }
}
finally {
    Pop-Location
}
