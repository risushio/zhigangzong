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
    } elseif (Test-Path -LiteralPath 'D:\maven') {
        $taskNestedMaven = @(Get-ChildItem -LiteralPath 'D:\maven' -Directory -Filter 'apache-maven-*' |
            ForEach-Object { Join-Path $_.FullName 'bin\mvn.cmd' } |
            Where-Object { Test-Path -LiteralPath $_ })
        if ($taskNestedMaven.Count -ne 1) {
            throw 'Set MAVEN_HOME to select the installed Maven distribution.'
        }
        $taskMaven = $taskNestedMaven[0]
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
