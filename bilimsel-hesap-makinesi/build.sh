#!/bin/bash
# Ajantik derleme sablonu. Yeni proje: bu klasoru kopyala, package adini,
# kodu ve manifesti degistir. Gradle YOK: aapt2 + javac + d8 + apksigner hatti.
set -e

USR="/data/user/0/com.ajantik.ajantik/files/usr"
SDK_JAR="$USR/share/android-sdk/platforms/android-34/android.jar"
export PATH="$USR/bin:$PATH"
export LD_LIBRARY_PATH="$USR/lib"

rm -rf out gen obj
mkdir -p out gen obj

echo "[1/5] Kaynaklar derleniyor (aapt2 compile)..."
aapt2 compile --dir res -o obj/res.zip

echo "[2/5] Baglaniyor (aapt2 link)..."
aapt2 link -o out/app.unaligned.apk -I "$SDK_JAR" \
  --manifest AndroidManifest.xml -R obj/res.zip --java gen --auto-add-overlay

echo "[3/5] Java derleniyor (javac)..."
find src gen -name "*.java" > obj/sources.txt
javac -source 8 -target 8 -nowarn -classpath "$SDK_JAR" -d obj/classes @obj/sources.txt

echo "[4/5] Dex uretiliyor (d8)..."
jar cf obj/classes.jar -C obj/classes .
d8 --release --lib "$SDK_JAR" --output out obj/classes.jar
(cd out && aapt add app.unaligned.apk classes.dex > /dev/null)

echo "[5/5] Imzalanıyor (apksigner)..."
if [ ! -f debug.keystore ]; then
  keytool -genkeypair -keystore debug.keystore \
    -storepass ajantik -keypass ajantik -alias ajantik \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Ajantik,O=Ajantik,C=TR" 2>/dev/null
fi
apksigner sign --ks debug.keystore --ks-pass pass:ajantik \
  --key-pass pass:ajantik --out out/uygulama.apk out/app.unaligned.apk

echo "TAMAM: out/uygulama.apk"