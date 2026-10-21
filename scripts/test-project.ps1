param(
    [string]$JavaFxPath = 'lib/javafx-sdk-27/lib',
    [ValidateSet('All', 'Cinematics')]
    [string]$Suite = 'All',
    [int]$Release = 0
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $projectRoot
try {
    $modulePath = (Resolve-Path -LiteralPath $JavaFxPath).Path
    $outputPath = Join-Path $projectRoot 'out/project-tests/classes'
    $modules = 'javafx.controls,javafx.media,javafx.swing'
    New-Item -ItemType Directory -Path $outputPath -Force | Out-Null
    $sources = @(Get-ChildItem -LiteralPath 'src/main/java', 'src/test/java' -Recurse -Filter '*.java' |
        ForEach-Object { $_.FullName })
    $compilerArgs = @('-encoding', 'UTF-8', '--module-path', $modulePath, '--add-modules', $modules, '-d', $outputPath)
    if ($Release -gt 0) { $compilerArgs += @('--release', [string]$Release) }
    javac @compilerArgs @sources
    if ($LASTEXITCODE -ne 0) { throw 'Falha na compilacao do projeto e dos testes.' }
    $testClasses = @('br.com.raizes.audio.CinematicRegressionTest')
    if ($Suite -eq 'All') {
        $testClasses = @('br.com.raizes.character.CharacterSpriteRegressionTest',
            'br.com.raizes.core.CharacterSelectionRegressionTest') + $testClasses + 'br.com.raizes.core.GameFlowRegressionTest'
    }
    foreach ($testClass in $testClasses) {
        Write-Output "Executando $testClass"
        java --module-path $modulePath --add-modules $modules --enable-native-access=javafx.graphics,javafx.media `
            -cp $outputPath $testClass
        if ($LASTEXITCODE -ne 0) { throw "Falha em $testClass" }
    }
} finally {
    Pop-Location
}
