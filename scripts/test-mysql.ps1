$ErrorActionPreference = 'Stop'
$taskPrevious = $env:RUN_MYSQL_TESTS
try {
    $env:RUN_MYSQL_TESTS = 'true'
    & (Join-Path $PSScriptRoot 'maven.ps1') verify
    $taskExitCode = $LASTEXITCODE
} finally {
    $env:RUN_MYSQL_TESTS = $taskPrevious
}
exit $taskExitCode
