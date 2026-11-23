#requires -Version 7.0
param([string]$BaseUrl = 'http://localhost:8080', [string]$MailpitUrl = 'http://localhost:8025')
$ErrorActionPreference = 'Stop'
if (!$env:ADMIN_EMAIL -or !$env:ADMIN_PASSWORD) { throw 'Carga .env con scripts/Load-Env.ps1 antes de ejecutar la prueba' }
function Invoke-Api([string]$Method, [string]$Path, $Body = $null, [string]$Token = '') {
    $params = @{ Method = $Method; Uri = "$BaseUrl$Path"; ContentType = 'application/json'; TimeoutSec = 15 }
    if ($null -ne $Body) { $params.Body = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 6)) }
    if ($Token) { $params.Headers = @{ Authorization = "Bearer $Token" } }
    Invoke-RestMethod @params
}
$rutBody = (Get-Random -Minimum 10000000 -Maximum 99000000).ToString()
$sum = 0
for ($i = 0; $i -lt $rutBody.Length; $i++) { $sum += [int]::Parse($rutBody[$rutBody.Length - 1 - $i].ToString()) * ($i % 6 + 2) }
$digit = 11 - $sum % 11
$dv = if ($digit -eq 11) { '0' } elseif ($digit -eq 10) { 'K' } else { $digit.ToString() }
$personal = "smoke-$([Guid]::NewGuid().ToString('N'))@example.org"
$question = '¿Cuál es el nombre de tu primera mascota?'
$created = Invoke-Api POST '/api/v1/auth/registro' @{
    nombre = 'Prueba Backend Induztek'; rut = "$rutBody$dv"; telefono = '+56 9 1234 5678'
    correoPersonal = $personal; preguntaSeguridad = $question; respuestaSeguridad = 'Luna'
}
if (!$created.correoInterno) { throw 'Registro sin correoInterno' }
$adminSession = Invoke-Api POST '/api/v1/auth/login' @{ correo = $env:ADMIN_EMAIL; contrasena = $env:ADMIN_PASSWORD }
$null = Invoke-Api PATCH "/api/v1/usuarios/$($created.id)/rol" @{ rolCodigo = 'TECNICO' } $adminSession.accessToken
$null = Invoke-Api POST '/api/v1/auth/recuperacion/solicitar' @{
    correo = $created.correoInterno; preguntaSeguridad = $question; respuestaSeguridad = 'luna'
}
$message = $null
for ($attempt = 0; $attempt -lt 20 -and !$message; $attempt++) {
    $messages = Invoke-RestMethod "$MailpitUrl/api/v1/messages" -TimeoutSec 5
    # Enumerar explícitamente: Object[].Address es un método de .NET y oculta la propiedad de sus elementos.
    $message = $messages.messages | Where-Object { @($_.To | ForEach-Object { $_.Address }) -contains $personal } | Select-Object -First 1
    if (!$message) { Start-Sleep -Milliseconds 300 }
}
if (!$message) { throw 'Mailpit no recibió el código' }
$detail = Invoke-RestMethod "$MailpitUrl/api/v1/message/$($message.ID)" -TimeoutSec 5
$code = [regex]::Match($detail.Text, '[0-9]{8}').Value
if (!$code) { throw 'Correo sin código de ocho dígitos' }
$password = "Prueba segura $([Guid]::NewGuid().ToString('N'))"
$null = Invoke-Api POST '/api/v1/auth/recuperacion/restablecer' @{
    correo = $created.correoInterno; codigo = $code; nuevaContrasena = $password; confirmacion = $password
}
$session = Invoke-Api POST '/api/v1/auth/login' @{ correo = $created.correoInterno; contrasena = $password }
$taskProfile = Invoke-Api GET '/api/v1/usuarios/me' $null $session.accessToken
if ($taskProfile.rolCodigo -ne 'TECNICO' -or $taskProfile.id -ne $created.id) { throw 'Perfil inconsistente' }
$null = Invoke-Api POST '/api/v1/auth/logout' $null $session.accessToken
$rejected = $false
try { $null = Invoke-Api GET '/api/v1/usuarios/me' $null $session.accessToken }
catch { if ([int]$_.Exception.Response.StatusCode -eq 401) { $rejected = $true } else { throw } }
if (!$rejected) { throw 'El token revocado todavía funciona' }
$null = Invoke-Api PATCH "/api/v1/usuarios/$($created.id)/activo" @{ activo = $false } $adminSession.accessToken
$null = Invoke-Api POST '/api/v1/auth/logout' $null $adminSession.accessToken
Write-Output "OK: registro, rol, correo, contraseña, login, perfil y logout. Usuario sintético $($created.id) desactivado."
