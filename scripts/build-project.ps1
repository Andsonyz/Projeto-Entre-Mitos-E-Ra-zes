param(
    [string]$JavaFxPath = 'lib/javafx',
    [string]$OutputDirectory = 'out/runtime',
    [int]$Release = 25
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $projectRoot
try {
    $modulePath = (Resolve-Path -LiteralPath $JavaFxPath).Path
    $outputPath = [IO.Path]::GetFullPath((Join-Path $projectRoot $OutputDirectory))
    $allowedRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot 'out')) + [IO.Path]::DirectorySeparatorChar
    if (-not $outputPath.StartsWith($allowedRoot, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'A saida de compilacao deve ser uma subpasta de out/.'
    }
    New-Item -ItemType Directory -Path $outputPath -Force | Out-Null
    $sources = @(Get-ChildItem -LiteralPath 'src/main/java' -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
    javac -encoding UTF-8 --release $Release --module-path $modulePath --add-modules javafx.controls,javafx.media,javafx.swing -d $outputPath @sources
    if ($LASTEXITCODE -ne 0) { throw 'Falha na compilacao do jogo.' }

    # Remove only assets owned by the previous build that are no longer in the source.
    $manifestPath = Join-Path $outputPath 'assets-manifest.txt'
    $assets = @(foreach ($folder in @('Image','Audio','Videos')) {
        Get-ChildItem -LiteralPath $folder -Recurse -File | ForEach-Object {
            $_.FullName.Substring($projectRoot.Length + 1)
        }
    })
    if (Test-Path -LiteralPath $manifestPath) {
        foreach ($previous in Get-Content -LiteralPath $manifestPath) {
            if ($assets -contains $previous) { continue }
            $stale = [IO.Path]::GetFullPath((Join-Path $outputPath $previous))
            if (-not $stale.StartsWith($outputPath + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
                throw 'Manifesto de recursos contem caminho fora da saida.'
            }
            if (Test-Path -LiteralPath $stale -PathType Leaf) { Remove-Item -LiteralPath $stale }
        }
    }
    foreach ($relative in $assets) {
        $destination = Join-Path $outputPath $relative
        New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
        Copy-Item -LiteralPath (Join-Path $projectRoot $relative) -Destination $destination -Force
    }
    Set-Content -LiteralPath $manifestPath -Value $assets -Encoding UTF8
    Write-Output "Compilacao e recursos prontos em $outputPath"
} finally { Pop-Location }
