# Persistent owner-controlled Asterion Art release signing (PowerShell).
# Never create, rotate, upload, or publish signing credentials.
param([Parameter(Mandatory=$true)][string]$UnsignedApk,[switch]$Install)
$ErrorActionPreference='Stop'
$source=(Resolve-Path -LiteralPath $UnsignedApk).Path
if(-not $source.ToLowerInvariant().EndsWith('.apk')){throw 'Input must be an APK'}
$root=Join-Path $env:LOCALAPPDATA 'AsterionArt'
$keystore=Join-Path $root 'release-keystore.p12'
$credential=Join-Path $root 'signing-credentials.dpapi'
if(-not(Test-Path $keystore) -or -not(Test-Path $credential)){
    throw 'Existing persistent signing identity missing; refusing rotation'
}
$tools=Get-ChildItem -LiteralPath (Join-Path $env:ANDROID_HOME 'build-tools') -Directory |
    Sort-Object {[version]$_.Name} -Descending | Select-Object -First 1
if(-not $tools){throw 'Android build-tools missing'}
$apksigner=Join-Path $tools.FullName 'apksigner.bat'
if(-not(Test-Path $apksigner)){throw 'apksigner missing'}
$protected=(Get-Content -LiteralPath $credential -Raw) | ConvertTo-SecureString
$ptr=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($protected)
$output=Join-Path (Split-Path -Parent $source) 'asterion-art-release-signed.apk'
if($source -eq $output){throw 'Refusing in-place overwrite'}
try {
    $env:ASTERION_RELEASE_PASSWORD=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    & $apksigner sign --ks $keystore --ks-type PKCS12 --ks-key-alias asterion-art-release --ks-pass env:ASTERION_RELEASE_PASSWORD --key-pass env:ASTERION_RELEASE_PASSWORD --out $output $source
    if($LASTEXITCODE -ne 0){throw 'Signing failed'}
    & $apksigner verify --verbose --print-certs $output
    if($LASTEXITCODE -ne 0){throw 'Signature verification failed'}
    Write-Output ('Signed APK: '+$output)
    Write-Output ('SHA-256: '+(Get-FileHash -LiteralPath $output -Algorithm SHA256).Hash)
    if($Install){
        $attached=@(& adb devices | Select-Object -Skip 1 | Where-Object {$_ -match '^\S+\s+device\b'})
        if($attached.Count -ne 1){
            throw 'Exactly one authorized ADB phone must be connected; no installation attempted'
        }
        # Android rejects an incompatible upgrade; never uninstall or clear
        # app data and never delete the user's downloaded model weights.
        & adb install -r $output
        if($LASTEXITCODE -ne 0){throw 'Upgrade rejected: previous installation remains untouched'}
        Write-Output 'Signed update installed without removing app data'
    }
}finally{
    Remove-Item Env:ASTERION_RELEASE_PASSWORD -ErrorAction SilentlyContinue
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
}
