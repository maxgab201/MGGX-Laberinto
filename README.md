# MGGX Laberinto

Juego de laberintos en 3D, primera persona, para Android. Nativo (Kotlin +
OpenGL ES 3.0), sin motores de terceros y sin un solo archivo de arte o de
audio: la roca, los brazos, los iconos, la musica y los efectos se generan
por codigo.

## Que es

Bajas a una cueva. Cada nivel es un laberinto mas grande y mas largo que el
anterior, generado en el momento y **siempre resoluble**. Juntas ecos, esquivas
trampas, encontras la salida, y con lo que juntaste te equipas mejor para el
proximo descenso.

## Lo que tiene

- **3D en primera persona con brazos**: manos con cinco dedos modeladas por
  codigo, que se balancean al caminar y cambian segun los guantes que lleves.
- **Laberintos infinitos y siempre pasables**: se parte de un laberinto perfecto
  (recursive backtracker) y todo lo que se hace despues solo abre roca, nunca la
  cierra. Ademas se verifica con BFS antes de entregarlo.
- **8 ambientes de cueva** (caliza, musgo, cuarzo, hielo, azufre, magma,
  obsidiana y Vetagris), cada uno con su roca, su niebla y su luz.
- **61 objetos unicos** en la tienda: 25 consumibles, 15 mejoras permanentes,
  12 reliquias pasivas y 9 aspectos. Ninguno repite efecto.
- **Dos monedas propias**: Ecos (comunes) y Vetagris (raros).
- **Musica y efectos sintetizados en vivo**: drone, pad, melodia pentatonica,
  goteras y un eco largo de caverna. Ni un mp3.
- **Controles tactiles y mando**: joystick flotante, camara por arrastre y
  soporte completo de gamepad, tambien para navegar los menus.
- **Ajustes extensos y en criollo**: seis secciones, sin jerga tecnica.
- **Iconos propios**: los 94 iconos estan dibujados con vectores en el codigo.
  No hay ni un emoji.

## Como se compila

```bash
export ANDROID_HOME=/ruta/al/sdk
./gradlew :app:assembleRelease
```

El APK sale en `app/build/outputs/apk/release/` firmado con
`keystore/mggx-release.jks`, que se versiona junto al proyecto **a proposito**:
asi todas las versiones que se publican mantienen la misma firma y el telefono
puede actualizar la app sin desinstalarla.

Huella SHA-256 de la firma:
`A3:72:F9:5E:9E:EC:57:46:A2:66:8C:DA:F3:1C:56:6A:FC:97:39:B0:A4:49:A5:C1:6A:FA:66:86:52:4E:31:EE`

## Como se prueba

```bash
./gradlew :app:testDebugUnitTest     # 67 tests de logica
python3 tools/check_shaders.py       # valida los 10 shaders GLSL
```

Los tests cubren, entre otras cosas: que todos los niveles del 1 al 120 tengan
solucion, que el recorrido se alargue con el nivel, que la economia no se rompa,
que se pueda llegar caminando hasta la salida, que ningun objeto repita efecto
y que no queden iconos, ajustes ni efectos declarados que no hagan nada.

## Como esta armado

```
app/src/main/java/com/mggx/laberinto/
  maze/      generacion del laberinto y temas de cueva
  game/      partida, objetos, economia y estadisticas del jugador
  gl/        motor OpenGL ES 3.0 (mundo, props, brazos, shaders, texturas)
  core/      guardado, audio sintetizado y vibracion
  input/     mando fisico
  ui/        Compose: lobby, tienda, equipo, ajustes, HUD e iconos
```
