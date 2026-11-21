param([string]$JavaFxPath = 'lib/javafx')

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $projectRoot
try {
    & (Join-Path $PSScriptRoot 'build-project.ps1') -JavaFxPath $JavaFxPath -Release 25
    $modulePath = (Resolve-Path -LiteralPath $JavaFxPath).Path
    $package = Join-Path $projectRoot 'out/package'
    $classes = Join-Path $package 'tests'
    $work = Join-Path $package 'work'
    New-Item -ItemType Directory -Path $package,$classes,$work -Force | Out-Null
    $jar = Join-Path $package 'game.jar'
    # PNG/MP4 assets are already compressed. Store entries directly and bound the packager heap.
    jar -J-Xms16m -J-Xmx64m --create --no-compress --file $jar --main-class br.com.raizes.Main -C out/runtime .
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao empacotar.' }
    $sources = @('src/test/java/br/com/raizes/assets/AssetExtractionRegressionTest.java',
        'src/test/java/br/com/raizes/core/EndingSimulationRegressionTest.java')
    javac -encoding UTF-8 --release 25 --module-path $modulePath --add-modules javafx.controls,javafx.media,javafx.swing `
        -cp $jar -d $classes @sources
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar testes do pacote.' }
    $expectedHash = (Get-FileHash 'Videos/Cenas/CenaFase1.mp4' -Algorithm SHA256).Hash
    Push-Location -LiteralPath $work
    try {
        foreach ($folder in @('Image','Audio','Videos')) {
            if (Test-Path -LiteralPath $folder) { throw 'O teste exige cwd sem pastas de recursos.' }
        }
        java -Xmx64m -cp "$jar;$classes" br.com.raizes.assets.AssetExtractionRegressionTest $expectedHash
        if ($LASTEXITCODE -ne 0) { throw 'Falha de extracao/integridade.' }
        $errorLog = Join-Path $package 'ending.stderr.log'
        $previousPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try {
            java -Xms32m -Xmx128m -XX:ActiveProcessorCount=2 --module-path $modulePath `
                --add-modules javafx.controls,javafx.media,javafx.swing --enable-native-access=javafx.graphics,javafx.media `
                -cp "$jar;$classes" br.com.raizes.core.EndingSimulationRegressionTest 2> $errorLog
            $testExitCode = $LASTEXITCODE
        } finally { $ErrorActionPreference = $previousPreference }
        $diagnostics = Get-Content -LiteralPath $errorLog -Raw -ErrorAction SilentlyContinue
        if (-not [string]::IsNullOrWhiteSpace($diagnostics)) { Write-Output $diagnostics }
        if ($testExitCode -ne 0 -or $diagnostics -match 'Exception in thread') { throw 'Falha da campanha empacotada.' }
    } finally { Pop-Location }
    Write-Output "Pacote verificado: $jar"
} finally { Pop-Location }
