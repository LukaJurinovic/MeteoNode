<#
.SYNOPSIS
  Discover the backend server's IPv4 (by pinging a hostname) and write it into
  the gateway's config.h SERVER_URL, keeping the port and path intact.

.DESCRIPTION
  At a new site the server (your laptop running the backend) usually gets a new
  LAN IP. Run this before flashing the gateway:

      .\set-server-url.ps1 -ServerHost MYLAPTOP

  It pings the host, grabs the resolved IPv4, and rewrites the
  #define SERVER_URL "..." line in config.h to http://<ip>:<port><path>.

  Use -Ip to set an explicit address without pinging:

      .\set-server-url.ps1 -Ip 192.168.1.72

.PARAMETER ServerHost
  Hostname (or IP) of the machine running the backend. It is pinged to resolve
  the current IPv4. Ignored if -Ip is given.

.PARAMETER Ip
  Explicit IPv4 to use; skips the ping/resolve step.

.PARAMETER Port
  Server port. Default 8080.

.PARAMETER Path
  API path. Default /api/gateway.

.PARAMETER ConfigPath
  Path to config.h. Defaults to config.h next to this script.
#>
[CmdletBinding(DefaultParameterSetName = 'Ping')]
param(
  [Parameter(Mandatory, ParameterSetName = 'Ping')]
  [string]$ServerHost,

  [Parameter(Mandatory, ParameterSetName = 'Explicit')]
  [string]$Ip,

  [int]$Port = 8080,
  [string]$Path = '/api/gateway',
  [string]$ConfigPath = (Join-Path $PSScriptRoot 'config.h')
)

$ErrorActionPreference = 'Stop'

if ($PSCmdlet.ParameterSetName -eq 'Ping') {
  Write-Host "Pinging '$ServerHost' to resolve its IPv4..."
  try {
    $Ip = (Test-Connection -ComputerName $ServerHost -Count 1).IPV4Address.IPAddressToString
  } catch {
    Write-Warning "Ping failed ($($_.Exception.Message)); falling back to DNS resolution."
    $Ip = ([System.Net.Dns]::GetHostAddresses($ServerHost) |
           Where-Object { $_.AddressFamily -eq 'InterNetwork' } |
           Select-Object -First 1).IPAddressToString
  }
}

if ([string]::IsNullOrWhiteSpace($Ip) -or $Ip -notmatch '^\d{1,3}(\.\d{1,3}){3}$') {
  throw "Could not determine a valid IPv4 (got '$Ip')."
}

if (-not (Test-Path $ConfigPath)) {
  throw "config.h not found at $ConfigPath (copy config.example.h to config.h first)."
}

$url = "http://${Ip}:${Port}${Path}"
$pattern = '(?m)^#define\s+SERVER_URL\s+"[^"]*"'
$content = Get-Content -Path $ConfigPath -Raw

if ($content -notmatch $pattern) {
  throw "No SERVER_URL define line found in $ConfigPath."
}

$old = ([regex]::Match($content, $pattern)).Value
$content = [regex]::Replace($content, $pattern, "#define SERVER_URL `"$url`"")
Set-Content -Path $ConfigPath -Value $content -Encoding ascii -NoNewline

Write-Host "Updated SERVER_URL in $ConfigPath"
Write-Host "  was: $old"
Write-Host "  now: #define SERVER_URL `"$url`""
