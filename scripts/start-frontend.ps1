$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskNode = Get-Command node.exe -ErrorAction SilentlyContinue
if (-not $taskNode) { throw 'Node.js 18+ is required. Install Node.js or put node.exe on PATH.' }
& $taskNode.Source (Join-Path $taskRoot 'scripts\frontend-dev.mjs')
exit $LASTEXITCODE
