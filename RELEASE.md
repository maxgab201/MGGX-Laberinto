# Para crear la release de GitHub

Listo para copiar y pegar tal cual en "Create a new release" con el tag
`v1.9.1`, adjuntando `apks/MGGX-Laberinto-1.9.1.apk`. También la puede
publicar sola el workflow "Publicar todas las releases" (`workflow_dispatch`,
escribiendo `SI`).

---

## Titulo

```
MGGX Laberinto 1.9.1 - Taller de Modelado
```

## Descripcion

```markdown
## MGGX Laberinto 1.9.1 - Taller de Modelado

Juego de laberintos en 3D, primera persona, para Android. Nativo (Kotlin +
OpenGL ES 3.0), sin motores de terceros y sin un solo archivo de arte o de
audio: la roca, los brazos, los iconos, la musica y los efectos se generan
por codigo.

Esta version es una sola cosa: **se dejo de modelar a ciegas**.

Todos los modelos se escriben a mano, en codigo, y se verificaban solo por
numeros (que las caras miren para afuera, que las proporciones entren en un
rango). Eso no alcanza para decir si algo se VE bien. Asi que primero se
construyo un visor —un rasterizador por software que le saca fotos a las
mallas— y despues se rehicieron los modelos mirandolos.

### Arreglado
- **El companiero era un barril con patas.** Un cuerpo de revolucion que mide
  60 cm de hombro a hombro mide tambien 60 cm de pecho a espalda. Ahora el
  cuerpo esta achatado y tiene forma de persona.
- **El casco era un cono de 52 cm** que se tragaba la cabeza entera. Ahora es
  un domo ancho y bajo, con ala, cresta, visera y el farol.
- **La cabeza era un huevo terminado en punta.** Ahora es redonda, con
  orejas y una nariz que de verdad sobresale del perfil.
- **Las cinco armas estaban mal.** El garrote era un megafono (se abria y no
  cerraba), el pico y el hacha eran una T. Ahora el pico ARQUEA la cabeza como
  un pico de verdad, el hacha tiene una hoja de lamina y el garrote termina en
  un bollo.
- **Cinco de los ocho objetos de mano no se reconocian.** La antorcha era una
  lanza (y medía medio metro), el mapa un cano, la brujula una lata, el ovillo
  una piedra y la venda una cascara partida al medio.
- **Bug de fondo: los tubos no tapaban las puntas.** Nunca se noto porque se
  encadenan uno atras del otro, pero un tubo suelto sin tapas es un cano hueco.

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
