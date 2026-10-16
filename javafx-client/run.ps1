$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$maven = (Get-Command mvn.cmd -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty Source)
if (-not $maven) {
    $maven = Join-Path $env:LOCALAPPDATA 'Programs\IntelliJ IDEA\plugins\maven-plugin\lib\maven3\bin\mvn.cmd'
}
if (-not (Test-Path -LiteralPath $maven)) {
    throw 'Maven 3.9+ was not found. Install Maven or update PATH.'
}
$release = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'release' } else { '' }
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath $release) -or
    -not (Select-String -LiteralPath $release -Pattern '^JAVA_VERSION="21(\.|\")' -Quiet)) {
    throw 'Java 21 is required. Set JAVA_HOME to a JDK 21 installation.'
}
if (-not (Test-Path -LiteralPath (Join-Path $projectRoot '.env'))) {
    throw 'Create the root .env from .env.example and set the PostgreSQL password.'
}

Push-Location $projectRoot
try {
    docker compose --env-file .env -f infra/docker/compose.kr2.yml up -d --wait
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL did not start in Docker.' }
    & $maven -B install
    if ($LASTEXITCODE -ne 0) { throw 'Could not build the KR1 dependency.' }
} finally {
    Pop-Location
}

Push-Location $PSScriptRoot
try {
    & $maven javafx:run
    if ($LASTEXITCODE -ne 0) { throw 'Could not start JavaFX.' }
} finally {
    Pop-Location
}
