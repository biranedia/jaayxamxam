# Lance Maven sans l'installer. (lanceur v3)
#
# Maven est telecharge une seule fois dans backend/.maven/ puis reutilise.
# Usage :  .\mvnw.cmd test
#          .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
#
# Note : ce projet vise Java 21. Un JDK plus recent compile en principe sans probleme,
# mais Lombok (qui genere les getNom()/setNom()) casse parfois sur un JDK tout neuf.
# Si la compilation echoue en parlant de Lombok ou de com.sun.tools.javac, installe
# le JDK 21 et pointe JAVA_HOME dessus le temps de la session :
#     winget install Microsoft.OpenJDK.21
#     $env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.x.y-hotspot"
#
# Aucune commande ici ne lit la sortie de « java -version » : cette sortie part sur le
# canal d'erreur, et PowerShell la prend pour une panne. Maven dira lui-meme ce qui ne va pas.

$ErrorActionPreference = "Stop"

Write-Host "lanceur Maven v3" -ForegroundColor DarkGray

$version = "3.9.9"
$dossier = Join-Path $PSScriptRoot ".maven"
$maven   = Join-Path $dossier "apache-maven-$version"
$exe     = Join-Path $maven "bin\mvn.cmd"

# --- Java doit etre la : Maven ne l'installe pas
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host ""
    Write-Host "Java n'est pas installe, ou pas dans le PATH." -ForegroundColor Red
    Write-Host "Ce projet demande Java 21 :"
    Write-Host "    winget install Microsoft.OpenJDK.21"
    Write-Host "Puis ferme et rouvre PowerShell."
    exit 1
}

# --- Telechargement de Maven, une seule fois
if (-not (Test-Path $exe)) {
    New-Item -ItemType Directory -Force -Path $dossier | Out-Null
    $archive = Join-Path $dossier "maven.zip"

    # dlcdn ne garde que les versions courantes, archive.apache.org les garde toutes
    $adresses = @(
        "https://dlcdn.apache.org/maven/maven-3/$version/binaries/apache-maven-$version-bin.zip",
        "https://archive.apache.org/dist/maven/maven-3/$version/binaries/apache-maven-$version-bin.zip"
    )

    $telecharge = $false
    foreach ($adresse in $adresses) {
        try {
            Write-Host "Telechargement de Maven $version..." -ForegroundColor Cyan
            Invoke-WebRequest -Uri $adresse -OutFile $archive -UseBasicParsing
            $telecharge = $true
            break
        } catch {
            Write-Host "  echec depuis $adresse" -ForegroundColor DarkYellow
        }
    }

    if (-not $telecharge) {
        Write-Host ""
        Write-Host "Impossible de telecharger Maven. Verifie ta connexion, ou installe-le :" -ForegroundColor Red
        Write-Host "    winget install Apache.Maven"
        exit 1
    }

    Write-Host "Decompression..." -ForegroundColor Cyan
    Expand-Archive -Path $archive -DestinationPath $dossier -Force
    Remove-Item $archive
    Write-Host "Maven installe dans backend\.maven\" -ForegroundColor Green
}

# --- Execution : a partir d'ici, c'est Maven qui parle
$ErrorActionPreference = "Continue"
& $exe @args
exit $LASTEXITCODE
