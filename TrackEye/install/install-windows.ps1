# TrackEye Agent installer - Windows
#
# What this does:
#   1. Copies the agent jar to %LOCALAPPDATA%\TrackEye
#   2. Registers a Scheduled Task that starts it at logon
#   3. Starts it now and opens the setup page in your browser
#
# Usage (PowerShell, run from the folder containing the jar):
#   .\install.ps1 -JarPath .\TrackEye-2.0.0.jar -ServerUrl http://192.168.1.50:8080

param(
    [Parameter(Mandatory=$true)][string]$JarPath,
    [string]$ServerUrl = "http://localhost:8080"
)

if (-not (Test-Path $JarPath)) {
    Write-Error "Jar not found: $JarPath"
    exit 1
}

$InstallDir = "$env:LOCALAPPDATA\TrackEye"
New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
Copy-Item $JarPath "$InstallDir\TrackEye.jar" -Force

@"
server.port=8765
trackeye.server.url=$ServerUrl
trackeye.storage-path=`${user.home}/TrackEyeData
server.address=127.0.0.1
"@ | Set-Content "$InstallDir\application.properties"

$Action = New-ScheduledTaskAction -Execute "javaw.exe" `
    -Argument "-jar `"$InstallDir\TrackEye.jar`" --spring.config.location=`"$InstallDir\application.properties`"" `
    -WorkingDirectory $InstallDir
$Trigger = New-ScheduledTaskTrigger -AtLogOn
$Settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable

Unregister-ScheduledTask -TaskName "TrackEyeAgent" -Confirm:$false -ErrorAction SilentlyContinue
Register-ScheduledTask -TaskName "TrackEyeAgent" -Action $Action -Trigger $Trigger -Settings $Settings -RunLevel Highest | Out-Null

Write-Host "Installed as a Scheduled Task - it will start automatically at logon."

Start-ScheduledTask -TaskName "TrackEyeAgent"
Write-Host "Waiting for the agent to start..."
Start-Sleep -Seconds 5

$SetupUrl = "http://localhost:8765/setup.html"
Write-Host "Opening $SetupUrl - paste the email and token your admin gave you."
Start-Process $SetupUrl
