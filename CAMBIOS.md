# MGGX Laberinto 1.5.2

**Fecha:** 2026-09-07
**Version anterior:** 1.5.1

## Que se hizo

Se encontro y se corrigio la causa de fondo de por que el multijugador
publicado no andaba: el `google-services.json` esta en `.gitignore` (es la
configuracion de Firebase de cada uno, no algo que se suba al repo), asi que
el workflow de GitHub Actions nunca lo tenia al compilar. El APK que salia
de una release publicada arrancaba sin Firebase, mostrando el mismo cartel
de "no se pudo abrir la sala" que ya se habia hecho mas honesto en 1.5.1,
pero cuya causa real seguia sin resolverse.

## Cambios esperados

Que una release compilada y publicada por GitHub Actions tenga el
multijugador funcional de verdad, no solo en las builds locales de
desarrollo (donde el archivo si estaba presente).

## Cambios realizados

- El workflow de release ahora escribe `google-services.json` a partir de
  un secreto del repositorio (`GOOGLE_SERVICES_JSON`) antes de compilar, y
  valida que sea un JSON correcto con el paquete de la app registrado.
- Un control nuevo, justo antes de publicar: abre el APK ya compilado y
  confirma que la configuracion de Firebase realmente viajo adentro. Que el
  archivo estuviera presente al compilar no alcanza para asegurar que
  llego al APK: el limpiador de recursos (`isShrinkResources`) no ve que el
  SDK de Firebase busca esos `<string>` por nombre en tiempo de ejecucion,
  asi que se los puede llevar puestos sin avisar. Se agregan `keep.xml` y
  reglas de R8 para blindarlos.
- El cartel de la pantalla de multijugador dejo de asumir siempre "se
  compilo sin la configuracion": ahora mira si la configuracion esta
  presente en el APK y distingue dos casos distintos, que tienen arreglos
  distintos (falta el archivo vs. el archivo esta pero el SDK no arranco
  igual).

## Bugs reparados

- **La release publicada en GitHub salia sin Firebase**, asi que el
  multijugador nunca podia andar para quien la bajara — solo funcionaba en
  builds compiladas a mano con el archivo presente localmente.
- El mensaje de diagnostico de la pantalla de multijugador afirmaba una
  causa ("se compilo sin la configuracion") sin haberla comprobado.

## Tests

Verificado localmente: suite completa en verde, y un chequeo manual
confirmando que el APK compilado localmente (con `google-services.json`
presente) lleva la configuracion de Firebase en sus recursos.
