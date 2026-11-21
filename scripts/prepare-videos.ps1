param(
    [Parameter(Mandatory = $true)][string]$FfmpegPath,
    [string]$OutputDirectory = 'out/video-candidates'
)

# Produces reviewable candidates. Source videos are never overwritten by this script.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$binary = (Resolve-Path -LiteralPath $FfmpegPath).Path
$target = [IO.Path]::GetFullPath((Join-Path $projectRoot $OutputDirectory))
$allowed = [IO.Path]::GetFullPath((Join-Path $projectRoot 'out')) + [IO.Path]::DirectorySeparatorChar
if (-not $target.StartsWith($allowed, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'A saida deve estar dentro de out/.'
}
New-Item -ItemType Directory -Path $target -Force | Out-Null
foreach ($name in @('CenaFase1','CenaFase2','CenaFase3','CenaFase4','CenaFinal')) {
    $source = Join-Path $projectRoot "Videos/Cenas/$name.mp4"
    $destination = Join-Path $target "$name.mp4"
    & $binary -hide_banner -loglevel error -y -i $source -map 0:v:0 -map '0:a:0?' `
        -vf 'scale=960:540:flags=lanczos' -r 30 -c:v libx264 -preset medium -threads 2 `
        -profile:v baseline -level:v 3.1 -pix_fmt yuv420p -crf 23 -maxrate 1600k -bufsize 3200k `
        -g 60 -keyint_min 30 -c:a aac -b:a 128k -ar 44100 -ac 2 -movflags +faststart $destination
    if ($LASTEXITCODE -ne 0) { throw "Falha ao preparar $name" }
    [PSCustomObject]@{ Video = $name; OriginalBytes = (Get-Item $source).Length; CandidateBytes = (Get-Item $destination).Length }
}
