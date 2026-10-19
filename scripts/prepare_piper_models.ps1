$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$ttsRoot = Join-Path $repoRoot 'distribution/tts'
$models = @(
    @{ Name = 'vits-piper-fr_FR-upmc-medium'; Onnx = 'fr_FR-upmc-medium.onnx'; Voice = 'upmc' },
    @{ Name = 'vits-piper-fr_FR-siwis-medium'; Onnx = 'fr_FR-siwis-medium.onnx'; Voice = 'siwis' }
)

foreach ($model in $models) {
    $target = Join-Path $ttsRoot $model.Name
    $required = @(
        (Join-Path $target $model.Onnx),
        (Join-Path $target 'tokens.txt'),
        (Join-Path $target 'espeak-ng-data')
    )
    if (($required | ForEach-Object { Test-Path -LiteralPath $_ }) -notcontains $false) {
        Write-Host "Already prepared: $($model.Name)"
        continue
    }

    $archiveUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/$($model.Name).tar.bz2"
    $tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("eliteintel-piper-" + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $tempRoot | Out-Null
    try {
        $archive = Join-Path $tempRoot "$($model.Name).tar.bz2"
        $unpack = Join-Path $tempRoot 'unpacked'
        New-Item -ItemType Directory -Path $unpack | Out-Null
        Write-Host "Downloading $($model.Name) from the official sherpa-onnx release..."
        Invoke-WebRequest -Uri $archiveUrl -OutFile $archive

        $entries = & tar.exe -tjf $archive
        if ($LASTEXITCODE -ne 0) {
            throw "Could not inspect the model archive: $archiveUrl"
        }
        if ($entries | Where-Object { $_ -match '(^/|(^|/)\.\.(/|$))' -or $_ -notmatch "^$([regex]::Escape($model.Name))/" }) {
            throw "The archive contains an unexpected path: $archiveUrl"
        }

        & tar.exe -xjf $archive -C $unpack
        if ($LASTEXITCODE -ne 0) {
            throw "Could not extract the model archive: $archiveUrl"
        }

        $source = Join-Path $unpack $model.Name
        foreach ($path in @(
            (Join-Path $source $model.Onnx),
            (Join-Path $source 'tokens.txt'),
            (Join-Path $source 'espeak-ng-data')
        )) {
            if (-not (Test-Path -LiteralPath $path)) {
                throw "The downloaded archive is missing a required model file: $path"
            }
        }

        New-Item -ItemType Directory -Path $target -Force | Out-Null
        Copy-Item -Path (Join-Path $source '*') -Destination $target -Recurse -Force
        $modelCardUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/fr/fr_FR/$($model.Voice)/medium/MODEL_CARD"
        Invoke-WebRequest -Uri $modelCardUrl -OutFile (Join-Path $target 'PIPER_MODEL_CARD')
        Write-Host "Prepared: $target"
    } finally {
        $tempFullPath = [System.IO.Path]::GetFullPath($tempRoot)
        $tempBase = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
        $tempPrefix = $tempBase.TrimEnd([System.IO.Path]::DirectorySeparatorChar,
            [System.IO.Path]::AltDirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
        if ($tempFullPath.StartsWith($tempPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
            Remove-Item -LiteralPath $tempFullPath -Recurse -Force
        }
    }
}
