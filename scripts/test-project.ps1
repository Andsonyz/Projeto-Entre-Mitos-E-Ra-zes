param(
    [string]$JavaFxPath = 'lib/javafx',
    [ValidateSet('All', 'Cinematics', 'Presentation', 'Gameplay')]
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
    $testClasses = @('br.com.raizes.audio.CinematicResourceRegressionTest',
        'br.com.raizes.audio.CinematicRegressionTest')
    $gameplayTests = @('br.com.raizes.quiz.QuizBankRegressionTest',
        'br.com.raizes.core.GameplayUpdateRegressionTest',
        'br.com.raizes.audio.CinematicSkipRegressionTest')
    if ($Suite -ne 'Cinematics') {
        $presentationTests = @('br.com.raizes.character.CharacterSpriteRegressionTest',
            'br.com.raizes.core.CharacterSelectionRegressionTest',
            'br.com.raizes.core.WorldRendererRegressionTest',
            'br.com.raizes.audio.AmbientAudioRegressionTest',
            'br.com.raizes.core.PresentationRegressionTest',
            'br.com.raizes.StartupRegressionTest')
        if ($Suite -eq 'Presentation') { $testClasses = $presentationTests }
        elseif ($Suite -eq 'Gameplay') { $testClasses = $presentationTests + $gameplayTests + 'br.com.raizes.core.GameFlowRegressionTest' }
        else { $testClasses = $presentationTests + $gameplayTests + $testClasses + 'br.com.raizes.core.GameFlowRegressionTest' }
    }
    foreach ($testClass in $testClasses) {
        Write-Output "Executando $testClass"
        # Recovery regressions intentionally write to stderr. Judge the process exit code,
        # including when a PowerShell 5 caller redirects stderr into a log pipeline.
        $previousPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try {
            java --module-path $modulePath --add-modules $modules --enable-native-access=javafx.graphics,javafx.media `
                -cp $outputPath $testClass
            $testExitCode = $LASTEXITCODE
        } finally { $ErrorActionPreference = $previousPreference }
        if ($testExitCode -ne 0) { throw "Falha em $testClass (exit $testExitCode)" }
    }
} finally {
    Pop-Location
}
