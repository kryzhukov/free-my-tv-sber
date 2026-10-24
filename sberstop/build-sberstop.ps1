# Builds SberStop.apk without Gradle (aapt2 -> javac -> d8 -> zipalign -> apksigner).
$ErrorActionPreference = 'Continue'
# Builds SberStop.apk without Gradle. Needs JDK 17+ (JAVA_HOME) and Android SDK (ANDROID_HOME): build-tools 34 + platform 34.
$repo = $PSScriptRoot
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { throw "Set ANDROID_HOME (Android SDK)" }
if (-not $env:JAVA_HOME) { throw "Set JAVA_HOME (JDK 17+)" }
$BT = "$sdk\build-tools\34.0.0"
$AJ = "$sdk\platforms\android-34\android.jar"
$JAVAC = "$env:JAVA_HOME\bin\javac.exe"
$JAVA = "$env:JAVA_HOME\bin\java.exe"
$KEYTOOL = "$env:JAVA_HOME\bin\keytool.exe"
$JAR = "$env:JAVA_HOME\bin\jar.exe"

Set-Location $repo
if (Test-Path (Join-Path $repo 'out')) { Remove-Item (Join-Path $repo 'out') -Recurse -Force }
New-Item -ItemType Directory -Force -Path (Join-Path $repo 'out\gen'), (Join-Path $repo 'out\classes') | Out-Null

& "$BT\aapt2.exe" compile --dir (Join-Path $repo 'res') -o (Join-Path $repo 'out\res.zip')
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }
& "$BT\aapt2.exe" link -o (Join-Path $repo 'out\base.apk') -I $AJ --manifest (Join-Path $repo 'AndroidManifest.xml') `
    --java (Join-Path $repo 'out\gen') (Join-Path $repo 'out\res.zip') --min-sdk-version 26 --target-sdk-version 33
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

$sources = @()
$sources += Get-ChildItem (Join-Path $repo 'src') -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$sources += Get-ChildItem (Join-Path $repo 'out\gen') -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$argList = @('-nowarn', '-Xlint:-options', '-source', '8', '-target', '8',
    '-bootclasspath', "$AJ;$BT\core-lambda-stubs.jar", '-classpath', $AJ,
    '-encoding', 'UTF-8', '-d', (Join-Path $repo 'out\classes')) + $sources
& $JAVAC @argList
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

$classFiles = Get-ChildItem (Join-Path $repo 'out\classes') -Recurse -Filter *.class | ForEach-Object { $_.FullName }
$d8args = @('--release', '--min-api', '26', '--lib', $AJ, '--output', (Join-Path $repo 'out')) + $classFiles
& "$BT\d8.bat" @d8args
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

Push-Location (Join-Path $repo 'out')
try { & $JAR -u -M -f (Join-Path $repo 'out\base.apk') classes.dex } finally { Pop-Location }

& "$BT\zipalign.exe" -f -p 4 (Join-Path $repo 'out\base.apk') (Join-Path $repo 'out\aligned.apk')
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }
$ks = Join-Path $repo 'sberstop.keystore'
if (-not (Test-Path $ks)) {
    & $KEYTOOL -genkeypair -keystore $ks -alias sberstop -keyalg RSA -keysize 2048 -validity 20000 `
        -storepass sberstop -keypass sberstop -dname "CN=Sber Stop" 2>&1 | Out-Null
}
& "$BT\apksigner.bat" sign --ks $ks --ks-pass pass:sberstop --key-pass pass:sberstop `
    --out (Join-Path $repo 'SberStop.apk') (Join-Path $repo 'out\aligned.apk')
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }
$apk = Join-Path $repo 'SberStop.apk'
"BUILT: $apk ({0:N2} MB)" -f ((Get-Item $apk).Length / 1MB)
