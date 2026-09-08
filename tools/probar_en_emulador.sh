#!/usr/bin/env bash
# Instala el APK de release en un emulador ya arrancado, entra al primer nivel
# y deja capturas en /tmp/shots.
#
#   tools/probar_en_emulador.sh
#
# El emulador por software (sin KVM) es lento y se cae seguido: el script
# reintenta la instalacion y avisa si el sistema se murio, para no confundir
# una caida del emulador con un crash del juego.
set -uo pipefail

ADB="${ANDROID_HOME:-$HOME/android-sdk}/platform-tools/adb"
PKG="com.mggx.laberinto"
APK="app/build/outputs/apk/release/app-release.apk"
SHOTS="${1:-/tmp/shots}"
mkdir -p "$SHOTS"

[ -f "$APK" ] || { echo "No esta el APK. Corre: ./gradlew :app:assembleRelease"; exit 1; }

echo "Esperando a que el sistema levante..."
for _ in $(seq 1 60); do
  "$ADB" shell service check mount 2>/dev/null | grep -q found && break
  sleep 15
done

"$ADB" shell settings put global hide_error_dialogs 1 >/dev/null 2>&1 || true

echo "Instalando..."
ok=0
for i in $(seq 1 25); do
  if "$ADB" install -r "$APK" 2>&1 | grep -q Success; then ok=1; break; fi
  sleep 15
done
[ "$ok" = 1 ] || { echo "No se pudo instalar"; exit 1; }

"$ADB" logcat -c
"$ADB" shell am start -n "$PKG/.MainActivity" >/dev/null 2>&1

echo "Esperando al lobby (el emulador por software tarda un par de minutos)..."
sleep 140
"$ADB" exec-out screencap -p > "$SHOTS/lobby.png" 2>/dev/null

# El boton DESCENDER esta en el tercio inferior de la mitad izquierda.
ancho=$("$ADB" shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1 | cut -dx -f1)
alto=$("$ADB" shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1 | cut -dx -f2)
# En horizontal el ancho real es el lado mayor.
if [ "$alto" -gt "$ancho" ]; then w=$alto; h=$ancho; else w=$ancho; h=$alto; fi
"$ADB" shell input tap $((w * 27 / 100)) $((h * 77 / 100))

echo "Entrando al nivel..."
sleep 170
"$ADB" exec-out screencap -p > "$SHOTS/juego.png" 2>/dev/null

echo
if "$ADB" logcat -d 2>/dev/null | grep -q "DeadSystemException"; then
  echo "OJO: se cayo el sistema del emulador. Las capturas no sirven."
fi
crash=$("$ADB" logcat -d 2>/dev/null | grep -A2 "FATAL EXCEPTION" | grep -c "$PKG" || true)
if [ "${crash:-0}" -gt 0 ]; then
  echo "EL JUEGO CRASHEO:"
  "$ADB" logcat -d 2>/dev/null | grep -A 25 "FATAL EXCEPTION" | head -30
  exit 1
fi
echo "Sin crashes del juego. Capturas en $SHOTS"
