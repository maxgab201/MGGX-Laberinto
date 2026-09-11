# Para crear la release de GitHub

Listo para copiar y pegar tal cual en "Create a new release" con el tag
`v1.9.0`, adjuntando `apks/MGGX-Laberinto-1.9.0.apk`. También la puede
publicar sola el workflow "Publicar todas las releases" (`workflow_dispatch`,
escribiendo `SI`).

---

## Titulo

```
MGGX Laberinto 1.9.0 - Polvo en el Aire
```

## Descripcion

```markdown
## MGGX Laberinto 1.9.0 - Polvo en el Aire

Juego de laberintos en 3D, primera persona, para Android. Nativo (Kotlin +
OpenGL ES 3.0), sin motores de terceros y sin un solo archivo de arte o de
audio: la roca, los brazos, los iconos, la musica y los efectos se generan
por codigo.

### Nuevo
- **Se ve lo que llevas en la mano izquierda.** El pan, el mapa, el frasco, la
  antorcha, el ovillo del hilo, la brujula, la piedra, el vendaje. Cada uno con
  su forma y su material: el frasco tiene el liquido adentro y el cuello
  transparente, la antorcha tiene llama viva, el pan tiene corteza y miga.
- **Polvo flotando en el aire.** Es lo unico que le faltaba a la cueva para
  dejar de parecer un decorado, y encima es lo que deja VER el haz de la
  antorcha: hasta ahora solo se notaba cuando pegaba en una pared. Las motas no
  tienen color propio, solo se ven si les pega una luz.
- **Liquen y humedad en las paredes.** Regueros de agua que caen (y que pulen
  la roca, asi que la zona mojada se ve mas oscura y brilla mas) y manchones de
  verdin que crecen en las grietas, que es donde se junta la humedad. Cada
  bioma tiene lo suyo: las Cisternas Anegadas chorrean, las Galerias de Musgo
  estan tapizadas, y en las Venas de Magma no crece nada.

### Cambiado
- **El companiero de sala, rehecho.** Ahora tiene cabeza —con nariz, ceja y
  barba— y carga sus cosas: mochila con soga, cinturon y el pico colgado a la
  espalda. A diez metros en un pasillo oscuro no se ve ni la cara ni la ropa: se
  ve la silueta, y eso es lo que lo convierte en un minero y no en un maniqui.
- El casco tiene la lampara montada al frente, para que la luz salga de algo.

### Para instalarlo
1. Bajate el APK de aca abajo directo al celular.
2. Abrilo. Android te va a pedir permiso para instalar apps de origen
   desconocido: dale que si.
3. Listo. No hace falta desinstalar la version anterior: todas las
   publicaciones van firmadas con la misma clave.

### Datos
- Android 7.0 (API 24) o superior
- Necesita OpenGL ES 3.0
- Pesa 1.9 MB
- Anda sin internet. Lo unico que necesita conexion es el multijugador.
- Firma SHA-256: `A3:72:F9:5E:9E:EC:57:46:A2:66:8C:DA:F3:1C:56:6A:FC:97:39:B0:A4:49:A5:C1:6A:FA:66:86:52:4E:31:EE`
```
