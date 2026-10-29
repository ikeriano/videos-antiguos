set -e
cd "$(dirname "$0")"
export JAVA_HOME=/home/box/jdk17 PATH=/home/box/jdk17/bin:$PATH
BT=/home/box/android-sdk/build-tools/34.0.0; AJ=/home/box/android-sdk/platforms/android-34/android.jar
KS=${KS:-../keystore.jks}
: "${KS_PASS:?Define KS_PASS con la contraseña del keystore}"
rm -rf out; mkdir -p out/c out/dex
$BT/aapt2 compile --dir res -o out/res.zip
$BT/aapt2 link -o out/base.apk -I $AJ --manifest AndroidManifest.xml out/res.zip
javac -Xlint:-options --release 8 -cp $AJ -d out/c $(find src -name '*.java')
$BT/d8 --min-api 21 --release --lib $AJ --output out/dex $(find out/c -name '*.class')
cp out/base.apk out/u.apk; python3 -c "import zipfile;zipfile.ZipFile('out/u.apk','a').write('out/dex/classes.dex','classes.dex')"
$BT/zipalign -f 4 out/u.apk out/a.apk
[ -f $KS ] || keytool -genkeypair -keystore $KS -alias va -keyalg RSA -keysize 2048 -validity 10000 -storepass "$KS_PASS" -keypass "$KS_PASS" -dname "CN=Videos Antiguos, C=ES"
$BT/apksigner sign --ks $KS --ks-pass pass:$KS_PASS --key-pass pass:$KS_PASS --out ../VideosAntiguos.apk out/a.apk
$BT/apksigner verify -v ../VideosAntiguos.apk | head -4
rm -rf out
