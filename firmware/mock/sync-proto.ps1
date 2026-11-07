# Kopiuje wspólny kodek protokołu HARFA do szkicu mocka.
# Źródło prawdy: firmware/protocol. Nie edytuj kopii w mock_harp/src/harfa.
$root = $PSScriptRoot
$dest = Join-Path $root "mock_harp/src/harfa"
New-Item -ItemType Directory -Force $dest | Out-Null
Copy-Item (Join-Path $root "../protocol/harfa_proto.h") $dest -Force
Copy-Item (Join-Path $root "../protocol/harfa_proto.c") $dest -Force
Write-Host "Skopiowano kodek do $dest"