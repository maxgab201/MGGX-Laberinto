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

api() { curl -sS -H "Authorization: Bearer $TOKEN" -H "Accept: application/vnd.github+json" "$@"; }

USADOS="$(api "$API/repos/$REPO/releases?per_page=100" | grep -o '"name": *"[^"]*"' | cut -d'"' -f4 || true)"

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

HUELLA="$("${ANDROID_HOME:-$HOME/android-sdk}/build-tools/34.0.0/apksigner" verify --print-certs "$APK" 2>/dev/null \
  | grep -i "SHA-256 digest" | head -1 | awk '{print $NF}')"

CUERPO=$(cat <<EOF
## MGGX Laberinto $VERSION - $NOMBRE

Juego de laberintos en 3D, primera persona, para Android. Nativo, sin motores
de terceros y sin archivos de arte ni de audio: todo se genera por codigo.

### Para instalarlo
1. Bajate el APK de aca abajo al celular.
2. Abrilo. Android te va a pedir permiso para instalar apps de origen
   desconocido: dale que si.
3. Listo. No hace falta desinstalar la version anterior: todas las
   publicaciones van firmadas con la misma clave.

### Datos
- Android 7.0 (API 24) o superior
- Necesita OpenGL ES 3.0 (lo tiene practicamente cualquier celular de 2014 en adelante)
- Pesa poco mas de 1 MB
- Firma SHA-256: \`$HUELLA\`
EOF
)

echo "Creando release $VERSION - $NOMBRE ..."
RESP="$(api -X POST "$API/repos/$REPO/releases" -d "$(python3 - "$VERSION" "$NOMBRE" "$CUERPO" <<'PY'
import json,sys
print(json.dumps({
  "tag_name": "v"+sys.argv[1],
  "name": f"MGGX Laberinto {sys.argv[1]} - {sys.argv[2]}",
  "body": sys.argv[3],
  "draft": False,
  "prerelease": False
}))
PY
)")"

ID="$(python3 -c "import json,sys; print(json.load(sys.stdin).get('id',''))" <<<"$RESP")"
[ -n "$ID" ] || { echo "No se pudo crear la release:"; echo "$RESP" | head -20; exit 1; }

echo "Subiendo el APK ..."
curl -sS -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/vnd.android.package-archive" \
  --data-binary @"/tmp/$DESTINO" \
  "$UP/repos/$REPO/releases/$ID/assets?name=$DESTINO" > /tmp/asset.json

python3 -c "
import json
d=json.load(open('/tmp/asset.json'))
print('APK subido:', d.get('name'), str(round(d.get('size',0)/1048576,2))+' MB')
"
python3 -c "import json,sys; print('Release:', json.loads(sys.stdin.read())['html_url'])" <<<"$RESP"
