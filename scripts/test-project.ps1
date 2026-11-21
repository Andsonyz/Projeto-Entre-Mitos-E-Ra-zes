param(
    [string]$JavaFxPath = 'lib/javafx',
    [ValidateSet('All', 'Cinematics', 'Presentation', 'Gameplay', 'Performance')]
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
    $performanceTests = @('br.com.raizes.core.LowResourceRegressionTest',
        'br.com.raizes.audio.VideoCompatibilityRegressionTest')
    $gameplayTests = @('br.com.raizes.quiz.QuizBankRegressionTest',
        'br.com.raizes.core.GameplayUpdateRegressionTest',
        'br.com.raizes.audio.CinematicSkipRegressionTest',
        'br.com.raizes.core.EndingSimulationRegressionTest')
    if ($Suite -eq 'Performance') { $testClasses = $performanceTests + @(
        'br.com.raizes.core.EndingSimulationRegressionTest', 'br.com.raizes.core.GameFlowRegressionTest') }
    elseif ($Suite -ne 'Cinematics') {
        $presentationTests = @('br.com.raizes.character.CharacterSpriteRegressionTest',
            'br.com.raizes.core.CharacterSelectionRegressionTest',
            'br.com.raizes.core.WorldRendererRegressionTest',
            'br.com.raizes.audio.AmbientAudioRegressionTest',
            'br.com.raizes.core.PresentationRegressionTest',
            'br.com.raizes.StartupRegressionTest')
        if ($Suite -eq 'Presentation') { $testClasses = $presentationTests }
        elseif ($Suite -eq 'Gameplay') { $testClasses = $presentationTests + $gameplayTests + 'br.com.raizes.core.GameFlowRegressionTest' }
        else { $testClasses = $presentationTests + $gameplayTests + $performanceTests + $testClasses + 'br.com.raizes.core.GameFlowRegressionTest' }
    }
    $logDirectory = Join-Path $projectRoot 'out/project-tests/logs'
    New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
    foreach ($testClass in $testClasses) {
        Write-Output "Executando $testClass"
        # Recovery regressions intentionally write to stderr. Judge the process exit code,
        # including when a PowerShell 5 caller redirects stderr into a log pipeline.
        $previousPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        $errorLog = Join-Path $logDirectory ($testClass + '.stderr.log')
        try {
            $runtimeArgs = @('-Xms64m','-Xmx256m')
            if ($Suite -eq 'Performance') { $runtimeArgs = @('-Xms32m','-Xmx128m','-XX:ActiveProcessorCount=2') }
            java @runtimeArgs --module-path $modulePath --add-modules $modules --enable-native-access=javafx.graphics,javafx.media `
                -cp $outputPath $testClass 2> $errorLog
            $testExitCode = $LASTEXITCODE
        } finally { $ErrorActionPreference = $previousPreference }
        $diagnostics = Get-Content -LiteralPath $errorLog -Raw -ErrorAction SilentlyContinue
        if (-not [string]::IsNullOrWhiteSpace($diagnostics)) { Write-Output $diagnostics }
        if ($testExitCode -ne 0) { throw "Falha em $testClass (exit $testExitCode)" }
        if ($diagnostics -match 'Exception in thread') {
            throw "Falha assincrona em $testClass. Consulte $errorLog"
        }
    }
    Write-Output "OK: suite $Suite concluida ($($testClasses.Count) testes)."
} finally {
    Pop-Location
}
