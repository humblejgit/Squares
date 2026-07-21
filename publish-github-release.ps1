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

function Assert-JarLicenses {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Path
    )

    $RequiredLicenses = @(
        @{
            Entry = "META-INF/LICENSE.txt"
            Source = "LICENSE.txt"
            Marker = "Copyright (c) 2026 Jan Pokorny. All rights reserved."
        },
        @{
            Entry = "META-INF/licenses/THIRD-PARTY-NOTICES.txt"
            Source = "squares-desktop\src\main\resources\META-INF\licenses\THIRD-PARTY-NOTICES.txt"
            Marker = "Squares third-party software notices"
        },
        @{
            Entry = "META-INF/licenses/sqlite-jdbc-3.53.2.0-Apache-2.0.txt"
            Source = "squares-desktop\src\main\resources\META-INF\licenses\sqlite-jdbc-3.53.2.0-Apache-2.0.txt"
            Marker = "Apache License"
        },
        @{
            Entry = "META-INF/licenses/sqlite-jdbc-Zentus-BSD-2-Clause.txt"
            Source = "squares-desktop\src\main\resources\META-INF\licenses\sqlite-jdbc-Zentus-BSD-2-Clause.txt"
            Marker = "Copyright (c) 2006, David Crawshaw"
        },
        @{
            Entry = "META-INF/licenses/slf4j-1.7.36-MIT.txt"
            Source = "squares-desktop\src\main\resources\META-INF\licenses\slf4j-1.7.36-MIT.txt"
            Marker = "Copyright (c) 2004-2022 QOS.ch"
        }
    )

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $ResolvedJar = (Resolve-Path -LiteralPath $Path).Path
    $Archive = [System.IO.Compression.ZipFile]::OpenRead($ResolvedJar)

    try {
        foreach ($License in $RequiredLicenses) {
            if (-not (Test-Path -LiteralPath $License.Source)) {
                throw "Chybi zdrojovy licencni soubor: $($License.Source)"
            }

            $Entry = $Archive.GetEntry($License.Entry)
            if (-not $Entry) {
                throw "V JARu chybi licencni soubor: $($License.Entry)"
            }

            $SourceText = [System.IO.File]::ReadAllText((Resolve-Path -LiteralPath $License.Source).Path)
            $Reader = New-Object System.IO.StreamReader($Entry.Open())
            try {
                $JarText = $Reader.ReadToEnd()
            } finally {
                $Reader.Dispose()
            }

            if ($JarText -ne $SourceText) {
                throw "Licencni soubor v JARu neodpovida zdroji: $($License.Entry)"
            }
            if (-not $JarText.Contains($License.Marker)) {
                throw "Licencni soubor nema ocekavany obsah: $($License.Entry)"
            }
        }
    } finally {
        $Archive.Dispose()
    }

    Write-Host "Licencni soubory v JARu byly overeny."
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
Assert-JarLicenses -Path $JarPath

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
