$ErrorActionPreference = "Stop"

$Repository = "humblejgit/Squares"
$JarAssetName = "squares.jar"
$JarFileName = "squares.jar"
$VersionFileName = ".squares-version"

$InstallDirectory = $PSScriptRoot
$JarPath = Join-Path $InstallDirectory $JarFileName
$VersionPath = Join-Path $InstallDirectory $VersionFileName
$ApiUrl = "https://api.github.com/repos/$Repository/releases/latest"
$Headers = @{
    "User-Agent" = "Squares launcher"
    "Accept" = "application/vnd.github+json"
}

function Get-LocalVersion {
    if (-not (Test-Path -LiteralPath $VersionPath)) {
        return ""
    }

    return (Get-Content -LiteralPath $VersionPath -Raw).Trim()
}

function Download-LatestJar {
    param(
        [string] $Url,
        [string] $Version,
        [string] $ExpectedSha256
    )

    $TempPath = "$JarPath.download"

    if (Test-Path -LiteralPath $TempPath) {
        Remove-Item -LiteralPath $TempPath -Force
    }

    Write-Host "Stahuji Squares $Version..."
    Invoke-WebRequest -Uri $Url -OutFile $TempPath -Headers $Headers

    if ($ExpectedSha256) {
        $DownloadedSha256 = (Get-FileHash -LiteralPath $TempPath -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($DownloadedSha256 -ne $ExpectedSha256) {
            Remove-Item -LiteralPath $TempPath -Force
            throw "Stazeny JAR neodpovida SHA-256 kontrolnimu souctu."
        }
    }

    Move-Item -LiteralPath $TempPath -Destination $JarPath -Force
    Set-Content -LiteralPath $VersionPath -Value $Version -Encoding ASCII
}

function Get-AssetSha256 {
    param(
        $Asset
    )

    if (-not $Asset.digest) {
        return ""
    }

    $Digest = [string] $Asset.digest
    if (-not $Digest.StartsWith("sha256:", [System.StringComparison]::OrdinalIgnoreCase)) {
        return ""
    }

    return $Digest.Substring("sha256:".Length).ToLowerInvariant()
}

function Get-LocalJarSha256 {
    if (-not (Test-Path -LiteralPath $JarPath)) {
        return ""
    }

    return (Get-FileHash -LiteralPath $JarPath -Algorithm SHA256).Hash.ToLowerInvariant()
}

try {
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

    Write-Host "Kontroluji aktualni verzi Squares..."
    $Release = Invoke-RestMethod -Uri $ApiUrl -Headers $Headers
    $RemoteVersion = ($Release.tag_name -replace "^v", "").Trim()
    $Asset = $Release.assets | Where-Object { $_.name -eq $JarAssetName } | Select-Object -First 1

    if (-not $Asset) {
        throw "V poslednim GitHub releasu neni asset $JarAssetName."
    }

    $LocalVersion = Get-LocalVersion
    $RemoteSha256 = Get-AssetSha256 -Asset $Asset
    $LocalSha256 = Get-LocalJarSha256
    $JarMissing = -not (Test-Path -LiteralPath $JarPath)
    $VersionChanged = $LocalVersion -ne $RemoteVersion
    $HashChanged = $RemoteSha256 -and $LocalSha256 -ne $RemoteSha256

    if ($JarMissing -or $VersionChanged -or $HashChanged) {
        Download-LatestJar -Url $Asset.browser_download_url -Version $RemoteVersion -ExpectedSha256 $RemoteSha256
    } else {
        if ($RemoteSha256) {
            Write-Host "Squares $LocalVersion je aktualni. SHA-256 sedi."
        } else {
            Write-Host "Squares $LocalVersion je aktualni."
        }
    }

    $JavaCommand = Get-Command java -ErrorAction SilentlyContinue
    if (-not $JavaCommand) {
        throw "Java nebyla nalezena v PATH. Nainstalujte Javu nebo ji pridejte do PATH."
    }

    Write-Host "Spoustim hru..."
    & java -jar $JarPath
    exit $LASTEXITCODE
} catch {
    Write-Host ""
    Write-Host "Chyba: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
