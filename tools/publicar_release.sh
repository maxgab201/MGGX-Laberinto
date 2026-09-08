#!/usr/bin/env bash
# Publica una release nueva en GitHub con el APK firmado.
#
#   tools/publicar_release.sh <version> ["Nombre de la release"]
#
# Si no se pasa nombre, se toma el siguiente de la lista de zonas de la cueva
# que todavia no se haya usado, asi cada release tiene un nombre distinto.
set -euo pipefail

REPO="maxgab201/MGGX-Laberinto"
API="https://api.github.com"
UP="https://uploads.github.com"
TOKEN="${GITHUB_TOKEN:-${GH_TOKEN:-}}"
[ -n "$TOKEN" ] || { echo "Falta GITHUB_TOKEN"; exit 1; }

VERSION="${1:?Falta la version, por ejemplo 1.0.0}"
NOMBRE="${2:-}"

# Nombres de zona, en el orden en que aparecen en el juego.
ZONAS=(
  "Boca de la Cueva" "Galerias de Musgo" "Cavernas de Cuarzo" "Sima Helada"
  "Pozos de Azufre" "Venas de Magma" "Corredores de Obsidiana" "Corazon de Vetagris"
  "Grieta del Eco" "Pozo sin Fondo" "Sala de las Mil Columnas" "Rio Subterraneo"
)

api() {
  curl -sS \
    -H "Authorization: Bearer $TOKEN" \
    -H "Accept: application/vnd.github+json" \
    -H "Content-Type: application/json" \
    -H "X-GitHub-Api-Version: 2022-11-28" "$@"
}

USADOS="$(api "$API/repos/$REPO/releases?per_page=100" | grep -o '"name":"[^"]*"' | cut -d'"' -f4 || true)"

if [ -z "$NOMBRE" ]; then
  for z in "${ZONAS[@]}"; do
    if ! grep -qF "$z" <<<"$USADOS"; then NOMBRE="$z"; break; fi
  done
  [ -n "$NOMBRE" ] || NOMBRE="Descenso $VERSION"
fi

if grep -qF "$NOMBRE" <<<"$USADOS"; then
  echo "Ya existe una release llamada '$NOMBRE'. Pone otro nombre."; exit 1
fi

APK="app/build/outputs/apk/release/app-release.apk"
[ -f "$APK" ] || { echo "No esta el APK. Corre: ./gradlew :app:assembleRelease"; exit 1; }

DESTINO="MGGX-Laberinto-$VERSION.apk"
cp "$APK" "/tmp/$DESTINO"

SIGNER="${ANDROID_HOME:-$HOME/android-sdk}/build-tools/34.0.0/apksigner"
HUELLA="$("$SIGNER" verify --print-certs "$APK" 2>/dev/null | grep -i "SHA-256 digest" | head -1 | awk '{print $NF}')"
PESO="$(du -h "$APK" | cut -f1)"

CUERPO="## MGGX Laberinto $VERSION - $NOMBRE

Juego de laberintos en 3D, primera persona, para Android. Nativo, sin motores
de terceros y sin archivos de arte ni de audio: la roca, los brazos, los
iconos, la musica y los efectos se generan por codigo.

### Para instalarlo
1. Bajate el APK de aca abajo directo al celular.
2. Abrilo. Android te va a pedir permiso para instalar apps de origen
   desconocido: dale que si.
3. Listo. No hace falta desinstalar la version anterior: todas las
   publicaciones van firmadas con la misma clave.

### Datos
- Android 7.0 (API 24) o superior
- Necesita OpenGL ES 3.0 (lo tiene practicamente cualquier celular de 2014 en adelante)
- Pesa $PESO
- Firma SHA-256: \`$HUELLA\`"

python3 tools/release_json.py "$VERSION" "$NOMBRE" "$CUERPO" /tmp/release_body.json

echo "Creando release $VERSION - $NOMBRE ..."
api -X POST "$API/repos/$REPO/releases" --data-binary @/tmp/release_body.json > /tmp/release_resp.json

ID="$(python3 -c "
import json
d = json.load(open('/tmp/release_resp.json'))
print(d.get('id', ''))
")"
if [ -z "$ID" ]; then
  echo "No se pudo crear la release:"; head -c 600 /tmp/release_resp.json; echo; exit 1
fi

echo "Subiendo el APK ..."
curl -sS -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/vnd.android.package-archive" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  --data-binary @"/tmp/$DESTINO" \
  "$UP/repos/$REPO/releases/$ID/assets?name=$DESTINO" > /tmp/asset_resp.json

python3 -c "
import json
a = json.load(open('/tmp/asset_resp.json'))
r = json.load(open('/tmp/release_resp.json'))
print('APK subido:', a.get('name'), str(round(a.get('size', 0) / 1048576, 2)) + ' MB')
print('Descarga:', a.get('browser_download_url'))
print('Release:', r.get('html_url'))
"
