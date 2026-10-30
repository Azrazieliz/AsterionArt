# ASTERION ART: Persisted 2026-10-30 owner key.
# This script NEVER creates another key and NEVER uninstalls or clears app data.
param([Parameter(Mandatory=$true)][string]$UnsignedApk,[switch]$Install)
$ErrorActionPreference='Stop'
$root='D:\AsterionArtSigning\identity-20261030'
$manifest=Get-Content (Join-Path $root 'identity.json') -Raw | ConvertFrom-Json
$keystore=Join-Path $root 'asterion-art-persistent-20261030.p12'
$passwordFile=Join-Path $root 'release-password.dpapi'
$alias='asterion-art-persistent-20261030'
$expectedKey='8126c65bb771981ca4636269d2e2a3bed900c66b3b9716ddf5b18b02af777ae8'
$expectedCert='51e506a7ecfd7c7bf5962e442d55a10ceaf12f796583330a9a86f0bd749d3773'
if(-not(Test-Path -LiteralPath $keystore) -or -not(Test-Path -LiteralPath $passwordFile)){
    throw 'Previously saved owner identity unavailable. DO NOT rotate signing key.'
}
$actualKey=(Get-FileHash -LiteralPath $keystore -Algorithm SHA256).Hash.ToLowerInvariant()
if($actualKey -ne $expectedKey -or $manifest.certificate_sha256 -ne $expectedCert){
    throw 'Persistent owner identity mismatch; refusing to sign with another key.'
}
$source=(Resolve-Path -LiteralPath $UnsignedApk).Path
if(-not($source.ToLowerInvariant().EndsWith('.apk'))){throw 'APK path is required'}
$tools=Get-ChildItem -LiteralPath "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Directory |
    Sort-Object {[version]$_.Name} -Descending | Select-Object -First 1
if(-not $tools){throw 'Android build-tools not installed'}
$apksigner=Join-Path $tools.FullName 'apksigner.bat'
if(-not(Test-Path $apksigner)){throw 'apksigner not installed'}
$output=Join-Path (Split-Path $source -Parent) (
    [IO.Path]::GetFileNameWithoutExtension($source)+'-ASTERION-PERSISTENT-20261030.apk')
if($source -eq $output){throw 'Never overwrite the original APK'}
function Read-Cert([string]$path) {
    $result=& $apksigner verify --print-certs $path
    if($LASTEXITCODE -ne 0){throw 'APK signature verification failed'}
    $match=[regex]::Match(($result -join "`n"),'certificate SHA-256 digest:\s*([0-9a-fA-F]{64})')
    if(-not $match.Success){throw 'Cannot determine signed APK certificate'}
    return $match.Groups[1].Value.ToLowerInvariant()
}
$protected=(Get-Content -LiteralPath $passwordFile -Raw).Trim() | ConvertTo-SecureString
$ptr=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($protected)
try {
    $env:ASTERION_NEW_RELEASE_PASSWORD=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    & $apksigner sign --ks $keystore --ks-type PKCS12 --ks-key-alias $alias --ks-pass env:ASTERION_NEW_RELEASE_PASSWORD --key-pass env:ASTERION_NEW_RELEASE_PASSWORD --out $output $source
    if($LASTEXITCODE -ne 0){throw 'Owner signing failed; no new certificate generated'}
    if((Read-Cert $output) -ne $expectedCert){throw 'Signed output uses wrong certificate'}
    Write-Output ('SIGNED_APK='+$output)
    Write-Output ('SIGNED_SHA256='+((Get-FileHash $output -Algorithm SHA256).Hash.ToLowerInvariant()))
    Write-Output ('CERTIFICATE_SHA256='+$expectedCert)
    if($Install) {
        $attached=@(& adb devices | Select-Object -Skip 1 |
            Where-Object {$_ -match '^\S+\s+device\b'})
        if($attached.Count -ne 1){throw 'Exactly one authorized connected phone required'}
        $installed=(& adb shell pm path com.asterion.art | Select-Object -First 1).Trim()
        if($installed.StartsWith('package:')) {
            $temp=Join-Path $env:TEMP 'asterion-preupgrade-certificate-check.apk'
            try {
                & adb pull ($installed.Substring(8)) $temp | Out-Null
                if($LASTEXITCODE -ne 0){throw 'Could not inspect installed app certificate'}
                $onPhone=Read-Cert $temp
                if($onPhone -ne $expectedCert) {
                    throw ('REFUSED: installed app certificate '+$onPhone+
                        ' differs from new permanent signer. Existing app and user models unchanged.')
                }
            } finally {Remove-Item -LiteralPath $temp -ErrorAction SilentlyContinue}
        }
        & adb install -r $output
        if($LASTEXITCODE -ne 0){throw 'Upgrade rejected. Do NOT uninstall or clear app data.'}
        Write-Output 'UPGRADE_INSTALLED_WITH_EXISTING_DATA_PRESERVED'
    }
} finally {
    Remove-Item Env:ASTERION_NEW_RELEASE_PASSWORD -ErrorAction SilentlyContinue
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
}
