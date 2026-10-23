[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$TomcatHome,

    [string]$TomcatServiceName,

    [ValidatePattern('^[A-Za-z0-9._-]+$')]
    [string]$AppContext = 'sgcomplaint',

    [string]$WarPath,

    [switch]$SkipBuild,
    [switch]$TomcatAlreadyStopped,

    [string]$HealthUrl = 'http://127.0.0.1:8080/sgcomplaint/actuator/health'
)

$ErrorActionPreference = 'Stop'
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$TomcatRoot = (Resolve-Path -LiteralPath $TomcatHome).Path
$WebApps = Join-Path $TomcatRoot 'webapps'

${CatalinaCommand} = Join-Path $TomcatRoot 'bin\catalina.bat'
if (-not (Test-Path -LiteralPath $CatalinaCommand)) {
    throw "Tomcat 10.1 경로가 아닙니다: $TomcatRoot"
}
$TomcatVersion = (& $CatalinaCommand version 2>&1 | Out-String)
if ($LASTEXITCODE -ne 0 -or $TomcatVersion -notmatch 'Apache Tomcat/10\.1\.') {
    throw "Tomcat 10.1.x만 지원합니다. 확인된 경로: $TomcatRoot"
}
if (-not (Test-Path -LiteralPath $WebApps -PathType Container)) {
    throw "Tomcat webapps 디렉터리가 없습니다: $WebApps"
}

if (-not $SkipBuild) {
    & (Join-Path $ProjectRoot 'gradlew.bat') --no-daemon clean test bootWar
    if ($LASTEXITCODE -ne 0) {
        throw 'Gradle test 또는 bootWar가 실패했습니다.'
    }
}

if ([string]::IsNullOrWhiteSpace($WarPath)) {
    $WarPath = Join-Path $ProjectRoot 'build\libs\sgcomplaint.war'
}
$SourceWar = (Resolve-Path -LiteralPath $WarPath).Path

if (-not $TomcatAlreadyStopped) {
    if ([string]::IsNullOrWhiteSpace($TomcatServiceName)) {
        throw '안전한 배포를 위해 -TomcatServiceName 또는 -TomcatAlreadyStopped 중 하나를 지정하세요.'
    }
    $service = Get-Service -Name $TomcatServiceName -ErrorAction Stop
    if ($service.Status -ne 'Stopped') {
        Stop-Service -Name $TomcatServiceName -Force
        $service.WaitForStatus('Stopped', [TimeSpan]::FromSeconds(60))
    }
}

$TargetWar = Join-Path $WebApps "$AppContext.war"
$ExplodedDirectory = Join-Path $WebApps $AppContext
$WebAppsFull = [IO.Path]::GetFullPath($WebApps).TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
$ExplodedFull = [IO.Path]::GetFullPath($ExplodedDirectory)

if (-not $ExplodedFull.StartsWith($WebAppsFull, [StringComparison]::OrdinalIgnoreCase)) {
    throw "삭제 대상이 webapps 밖에 있습니다: $ExplodedFull"
}

$Timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$BackupDirectory = Join-Path $ProjectRoot "backup\tomcat\$Timestamp"
New-Item -ItemType Directory -Path $BackupDirectory -Force | Out-Null

if (Test-Path -LiteralPath $TargetWar -PathType Leaf) {
    Copy-Item -LiteralPath $TargetWar -Destination (Join-Path $BackupDirectory "$AppContext.war")
}
if (Test-Path -LiteralPath $ExplodedDirectory -PathType Container) {
    Compress-Archive -LiteralPath $ExplodedDirectory -DestinationPath (Join-Path $BackupDirectory "$AppContext-exploded.zip")
    Remove-Item -LiteralPath $ExplodedDirectory -Recurse -Force
}

$StagedWar = Join-Path $WebApps "$AppContext.war.deploying"
Copy-Item -LiteralPath $SourceWar -Destination $StagedWar -Force
Move-Item -LiteralPath $StagedWar -Destination $TargetWar -Force

if (-not [string]::IsNullOrWhiteSpace($TomcatServiceName)) {
    Start-Service -Name $TomcatServiceName
    (Get-Service -Name $TomcatServiceName).WaitForStatus('Running', [TimeSpan]::FromSeconds(60))
}

if ([string]::IsNullOrWhiteSpace($TomcatServiceName)) {
    Write-Host "WAR 배포 완료. Tomcat을 시작한 뒤 확인하세요: $HealthUrl"
    exit 0
}

$Healthy = $false
for ($attempt = 1; $attempt -le 30; $attempt++) {
    try {
        $response = Invoke-RestMethod -Uri $HealthUrl -TimeoutSec 3
        if ($response.status -eq 'UP') {
            $Healthy = $true
            break
        }
    } catch {
        Start-Sleep -Seconds 2
    }
}

if (-not $Healthy) {
    throw "배포 후 health 확인이 실패했습니다. 백업: $BackupDirectory, 로그: $TomcatRoot\logs"
}

Write-Host "배포 및 health 확인 완료: $HealthUrl"
Write-Host "이전 버전 백업: $BackupDirectory"
