param(
    [string] $Repository = "humblejgit/Squares",
    [string] $JarPath = "dist\squares.jar"
)

$ErrorActionPreference = "Stop"

function Get-ProjectVersion {
    [xml] $Pom = Get-Content -LiteralPath "pom.xml"
    return $Pom.project.version
}

function Build-Jar {
    if (-not (Test-Path -LiteralPath "dist")) {
        New-Item -ItemType Directory -Path "dist" | Out-Null
    }

    & mvn clean package
    if ($LASTEXITCODE -ne 0) {
        throw "Sestaveni aplikace selhalo."
    }

    Copy-Item -LiteralPath "target\squares.jar" -Destination $JarPath -Force
}

function Assert-GitHubCli {
    $Gh = Get-Command gh -ErrorAction SilentlyContinue
    if (-not $Gh) {
        throw "Neni nainstalovany GitHub CLI (gh)."
    }
}

$Version = Get-ProjectVersion
$Tag = "v$Version"

Assert-GitHubCli
Build-Jar

$JarHash = (Get-FileHash -LiteralPath $JarPath -Algorithm SHA256).Hash.ToLowerInvariant()
Write-Host "JAR: $JarPath"
Write-Host "SHA-256: $JarHash"

$PreviousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$null = & gh release view $Tag --repo $Repository 2>$null
$ReleaseExists = $LASTEXITCODE -eq 0
$ErrorActionPreference = $PreviousErrorActionPreference

if (-not $ReleaseExists) {
    gh release create $Tag $JarPath start.bat squares-launcher.ps1 --repo $Repository --title "Squares $Version" --notes "Squares $Version"
} else {
    gh release upload $Tag $JarPath start.bat squares-launcher.ps1 --repo $Repository --clobber
}

if ($LASTEXITCODE -ne 0) {
    throw "Publikovani GitHub releasu selhalo."
}

Write-Host "Release $Tag je publikovany."
