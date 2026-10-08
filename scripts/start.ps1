param(
    [switch]$SkipBuild,
    [ValidateRange(1, 65535)][int]$Port = 8080
)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskOldLocation = Get-Location
try {
    Set-Location -LiteralPath $taskRoot
    if (-not $SkipBuild) {
        & (Join-Path $PSScriptRoot 'maven.ps1') package
        if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' }
    }
    $taskJar = Join-Path $taskRoot 'target\zhigangzong-1.1.0.jar'
    if (-not (Test-Path -LiteralPath $taskJar)) { throw 'Build the project first.' }
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        $taskJava = Join-Path $env:JAVA_HOME 'bin\java.exe'
    } elseif (Test-Path -LiteralPath 'D:\java\bin\java.exe') {
        $taskJava = 'D:\java\bin\java.exe'
    } else {
        $taskJava = (Get-Command java.exe -ErrorAction Stop).Source
    }
    & $taskJava '-Dfile.encoding=UTF-8' -jar $taskJar "--server.port=$Port"
    $taskExitCode = $LASTEXITCODE
} finally {
    Set-Location -LiteralPath $taskOldLocation
}
exit $taskExitCode
