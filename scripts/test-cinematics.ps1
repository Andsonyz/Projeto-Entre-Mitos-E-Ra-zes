param(
    [string]$JavaFxPath = 'lib/javafx-sdk-27/lib'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $projectRoot
try {
    $modulePath = (Resolve-Path -LiteralPath $JavaFxPath).Path
    $outputPath = Join-Path $projectRoot 'out/cinematic-tests'
    $modules = 'javafx.controls,javafx.media,javafx.swing'
    New-Item -ItemType Directory -Path $outputPath -Force | Out-Null
    $sources = @(Get-ChildItem -LiteralPath 'src/main/java', 'src/test/java' -Recurse -Filter '*.java' |
        ForEach-Object { $_.FullName })
    javac -encoding UTF-8 --module-path $modulePath --add-modules $modules -d $outputPath @sources
    if ($LASTEXITCODE -ne 0) { throw 'Falha na compilacao dos testes.' }
    java --module-path $modulePath --add-modules $modules --enable-native-access=javafx.graphics,javafx.media `
        -cp $outputPath br.com.raizes.audio.CinematicRegressionTest
    if ($LASTEXITCODE -ne 0) { throw 'Falha nos testes de cinematicas.' }
} finally {
    Pop-Location
}
