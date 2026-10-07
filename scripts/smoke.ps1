param([string]$BaseUrl = 'http://127.0.0.1:8080', [string]$Username = 'admin')
$ErrorActionPreference = 'Stop'
# Keep local bootstrap credentials local: never send them to an arbitrary host.
$taskUri = [Uri]$BaseUrl
if ($taskUri.Host -notin @('127.0.0.1','localhost','[::1]')) { throw 'Local smoke tests require a loopback address.' }
$taskConfig = Join-Path (Split-Path -Parent $PSScriptRoot) '.local/application-local.properties'
$taskPassword = $env:ZGZ_ADMIN_PASSWORD
if (-not $taskPassword -and (Test-Path -LiteralPath $taskConfig)) {
    $taskLine = Get-Content -LiteralPath $taskConfig | Where-Object { $_ -match '^app.bootstrap.password=' } | Select-Object -First 1
    if ($taskLine) { $taskPassword = $taskLine.Substring('app.bootstrap.password='.Length) }
}
if (-not $taskPassword) { throw 'Set ZGZ_ADMIN_PASSWORD or configure the local bootstrap password first.' }
$taskCsrf = Invoke-RestMethod -Uri "$BaseUrl/api/auth/csrf" -SessionVariable taskSession
$taskHeaders = @{}
$taskHeaders[$taskCsrf.data.headerName] = $taskCsrf.data.token
$null = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -WebSession $taskSession -Headers $taskHeaders -Body @{username=$Username;password=$taskPassword}
$taskRoutes = @('health', 'health/live', 'modules', 'statistics/overview', 'schools', 'departments', 'users', 'students', 'enterprises', 'jobs', 'job-favorites', 'applications', 'recruitment-events', 'batches', 'placements', 'approvals', 'materials', 'reports', 'attendance', 'changes', 'guidance', 'alerts', 'alert-follow-ups', 'evaluations', 'archives', 'notifications', 'workflow-rules', 'audit-logs', 'match-feedback')
foreach ($taskRoute in $taskRoutes) {
    $taskResponse = Invoke-RestMethod -Uri "$BaseUrl/api/$taskRoute" -WebSession $taskSession -TimeoutSec 10
    if ($taskResponse.code -ne 'OK') { throw "Unexpected result for $taskRoute" }
    if ($taskRoute -eq 'health' -and $taskResponse.data.status -ne 'UP') { throw 'Database health check failed.' }
    Write-Output "PASS GET /api/$taskRoute"
}
$taskCsrf = Invoke-RestMethod -Uri "$BaseUrl/api/auth/csrf" -WebSession $taskSession
$taskHeaders[$taskCsrf.data.headerName] = $taskCsrf.data.token
$null = Invoke-RestMethod -Uri "$BaseUrl/api/auth/logout" -Method Post -WebSession $taskSession -Headers $taskHeaders
Write-Output 'PASS authenticated session and logout'
