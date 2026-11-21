param(
    [string]$JavaFxPath = 'lib/javafx'
)

& (Join-Path $PSScriptRoot 'test-project.ps1') -JavaFxPath $JavaFxPath -Suite Cinematics
