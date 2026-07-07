param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('install', 'launch', 'stop')]
    [string]$Action
)

$workspaceRoot = Split-Path -Path $PSScriptRoot -Parent
$localPropertiesPath = Join-Path $workspaceRoot 'client/local.properties'

function Get-AdbPath {
    $sdkDir = $null

    if (Test-Path $localPropertiesPath) {
        $line = Get-Content $localPropertiesPath | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($line) {
            $raw = $line -replace '^sdk\.dir=', ''
            # local.properties escapes separators and ':' for Windows paths.
            $sdkDir = $raw
            $sdkDir = $sdkDir.Replace('\:', ':')
            $sdkDir = $sdkDir.Replace('\/', '/')
            $sdkDir = $sdkDir.Replace('\\', '\')
        }
    }

    if (-not $sdkDir) {
        $sdkDir = $env:ANDROID_SDK_ROOT
    }
    if (-not $sdkDir) {
        $sdkDir = $env:ANDROID_HOME
    }

    if ($sdkDir) {
        $candidate = Join-Path $sdkDir 'platform-tools/adb.exe'
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) {
        return $cmd.Source
    }

    return $null
}

$adb = Get-AdbPath
if (-not $adb) {
    Write-Host 'adb not found. Set client/local.properties sdk.dir or ANDROID_SDK_ROOT.'
    exit 1
}

& $adb start-server | Out-Null
$devices = & $adb devices | Select-String '\tdevice$'

if ($Action -eq 'install') {
    if (-not $devices) {
        Write-Host 'No Android device/emulator connected. Skipping :app:installDebug.'
        exit 0
    }

    Set-Location (Join-Path $workspaceRoot 'client')
    & .\gradlew.bat :app:installDebug
    exit $LASTEXITCODE
}

if ($Action -eq 'launch') {
    if (-not $devices) {
        Write-Host 'No Android device/emulator connected. Skipping app launch.'
        exit 0
    }

    & $adb shell am start -n sg.edu.nus.iss.client/.MainActivity
    exit $LASTEXITCODE
}

if ($Action -eq 'stop') {
    & $adb shell am force-stop sg.edu.nus.iss.client
    exit $LASTEXITCODE
}
