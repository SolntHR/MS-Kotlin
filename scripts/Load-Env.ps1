#requires -Version 7.0
param([string]$Path = (Join-Path $PSScriptRoot '..\.env'))
$ErrorActionPreference = 'Stop'
foreach ($line in Get-Content -LiteralPath $Path) {
    $valueLine = $line.Trim()
    if (!$valueLine -or $valueLine.StartsWith('#')) { continue }
    $parts = $valueLine.Split('=', 2)
    if ($parts.Count -ne 2 -or $parts[0] -notmatch '^(RECOVERY_CODE_SECRET|ADMIN_[A-Z_]+|INTERNAL_EMAIL_DOMAIN|DB_[A-Z_]+|SMTP_[A-Z_]+|EUREKA_[A-Z_]+|USUARIOS_URI|SERVER_PORT)$') {
        throw 'Variable de configuración no permitida o línea sin signo igual en .env'
    }
    $taskValue = $parts[1].Trim()
    if ($taskValue.Length -ge 2 -and (($taskValue.StartsWith('"') -and $taskValue.EndsWith('"')) -or ($taskValue.StartsWith("'") -and $taskValue.EndsWith("'")))) {
        $taskValue = $taskValue.Substring(1, $taskValue.Length - 2)
    }
    [Environment]::SetEnvironmentVariable($parts[0], $taskValue, 'Process')
}
