param(
    [string]$JavaFxPath = 'lib/javafx-sdk-27/lib'
)

& (Join-Path $PSScriptRoot 'test-project.ps1') -JavaFxPath $JavaFxPath -Suite Cinematics
