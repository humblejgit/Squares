[CmdletBinding()]
param(
    [string]$Issuer = "http://localhost:9090/realms/squares",
    [string]$ApiBaseUri = "http://localhost:8080/api/v1",
    [string]$ClientId = "squares-desktop"
)

$ErrorActionPreference = "Stop"

function ConvertTo-Base64Url {
    param([byte[]]$Bytes)

    return [Convert]::ToBase64String($Bytes).TrimEnd("=").Replace("+", "-").Replace("/", "_")
}

function New-RandomBase64Url {
    param([int]$Length)

    $bytes = New-Object byte[] $Length
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }

    return ConvertTo-Base64Url $bytes
}

$portProbe = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
$portProbe.Start()
$redirectPort = ([Net.IPEndPoint]$portProbe.LocalEndpoint).Port
$portProbe.Stop()

$redirectUri = "http://127.0.0.1:$redirectPort/"
$verifier = New-RandomBase64Url 64
$state = New-RandomBase64Url 32
$sha256 = [Security.Cryptography.SHA256]::Create()
try {
    $challenge = ConvertTo-Base64Url $sha256.ComputeHash([Text.Encoding]::ASCII.GetBytes($verifier))
} finally {
    $sha256.Dispose()
}

$authorizeEndpoint = "$Issuer/protocol/openid-connect/auth"
$tokenEndpoint = "$Issuer/protocol/openid-connect/token"
$authorizationUri = $authorizeEndpoint `
    + "?response_type=code" `
    + "&client_id=$([Uri]::EscapeDataString($ClientId))" `
    + "&redirect_uri=$([Uri]::EscapeDataString($redirectUri))" `
    + "&scope=openid" `
    + "&code_challenge=$([Uri]::EscapeDataString($challenge))" `
    + "&code_challenge_method=S256" `
    + "&state=$([Uri]::EscapeDataString($state))"

$listener = [Net.HttpListener]::new()
$listener.Prefixes.Add($redirectUri)

try {
    $listener.Start()
    Write-Host "Oteviram prihlaseni v systemovem prohlizeci..."
    Start-Process $authorizationUri

    $pendingContext = $listener.BeginGetContext($null, $null)
    if (-not $pendingContext.AsyncWaitHandle.WaitOne([TimeSpan]::FromMinutes(3))) {
        throw "Prihlaseni nebylo dokonceno do tri minut."
    }

    $context = $listener.EndGetContext($pendingContext)
    $returnedState = $context.Request.QueryString["state"]
    $code = $context.Request.QueryString["code"]
    $oauthError = $context.Request.QueryString["error"]

    $responseText = if ($oauthError) {
        "Prihlaseni se nezdarilo. Toto okno muzete zavrit."
    } else {
        "Prihlaseni probehlo. Toto okno muzete zavrit."
    }
    $responseBytes = [Text.Encoding]::UTF8.GetBytes(
        "<!doctype html><html><body><h1>$responseText</h1></body></html>")
    $context.Response.ContentType = "text/html; charset=utf-8"
    $context.Response.ContentLength64 = $responseBytes.Length
    $context.Response.OutputStream.Write($responseBytes, 0, $responseBytes.Length)
    $context.Response.Close()

    if ($oauthError) {
        throw "OIDC provider vratil chybu: $oauthError"
    }
    if ($returnedState -ne $state) {
        throw "OIDC odpoved obsahuje neplatny state."
    }
    if (-not $code) {
        throw "OIDC odpoved neobsahuje autorizacni kod."
    }

    $tokens = Invoke-RestMethod `
        -Method Post `
        -Uri $tokenEndpoint `
        -ContentType "application/x-www-form-urlencoded" `
        -Body @{
            grant_type = "authorization_code"
            client_id = $ClientId
            code = $code
            redirect_uri = $redirectUri
            code_verifier = $verifier
        }

    $me = Invoke-RestMethod `
        -Method Get `
        -Uri "$ApiBaseUri/me" `
        -Headers @{ Authorization = "Bearer $($tokens.access_token)" }

    Write-Host "Squares API prijalo OIDC access token:"
    $me | ConvertTo-Json -Depth 6
} finally {
    if ($listener.IsListening) {
        $listener.Stop()
    }
    $listener.Close()
    $tokens = $null
}
