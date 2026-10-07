param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArguments = @('verify')
)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskOldLocation = Get-Location
$taskOldJavaHome = $env:JAVA_HOME
try {
    Set-Location -LiteralPath $taskRoot
    if (-not $env:JAVA_HOME -and (Test-Path -LiteralPath 'D:\java\bin\javac.exe')) {
        $env:JAVA_HOME = 'D:\java'
    }
    $taskMavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($taskMavenCommand) {
        $taskMaven = $taskMavenCommand.Source
    } elseif ($env:MAVEN_HOME -and (Test-Path -LiteralPath (Join-Path $env:MAVEN_HOME 'bin\mvn.cmd'))) {
        $taskMaven = Join-Path $env:MAVEN_HOME 'bin\mvn.cmd'
    } elseif (Test-Path -LiteralPath 'D:\maven\bin\mvn.cmd') {
        $taskMaven = 'D:\maven\bin\mvn.cmd'
    } else {
        throw 'Maven not found. Set MAVEN_HOME for this process or put mvn.cmd on PATH.'
    }
    & $taskMaven -B -ntp @MavenArguments
    $taskExitCode = $LASTEXITCODE
} finally {
    $env:JAVA_HOME = $taskOldJavaHome
    Set-Location -LiteralPath $taskOldLocation
}
exit $taskExitCode
