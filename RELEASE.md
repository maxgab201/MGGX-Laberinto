# Para crear la release de GitHub

Listo para copiar y pegar tal cual en "Create a new release" con el tag
`v1.7.1`, adjuntando `apks/MGGX-Laberinto-1.7.1.apk`. También la puede
publicar sola el workflow "Publicar todas las releases" (`workflow_dispatch`,
escribiendo `SI`).

---

## Titulo

```
MGGX Laberinto 1.7.1 - Bosque de Esporas
```

## Descripcion

```markdown
## MGGX Laberinto 1.7.1 - Bosque de Esporas

Juego de laberintos en 3D, primera persona, para Android. Nativo (Kotlin +
OpenGL ES 3.0), sin motores de terceros y sin un solo archivo de arte o de
audio: la roca, los brazos, los iconos, la musica y los efectos se generan
por codigo.

Cuatro cosas salidas de seguir jugando la 1.7.0.

### Arreglado
- **La escalera ya no te mete adentro de la roca.** Yendo de frente y
  siguiendo para adelante, entrabas en el piso de arriba ANTES de haber
  subido, con el cuerpo dentro del escalon, y se traspasaba el piso de
  atras. La escalera no es un permiso para atravesar el escalon: ahora el
  escalon frena, empujas contra el y subis en el lugar, y recien arriba
  entras caminando.
- **Se saco la piedra rara que hacia dano** (la trampa de derrumbe): se
  leia como una roca puesta ahi sin que se entendiera por que te lastimaba.
- **Las manos van derechas.** Estaban de palma para abajo; ahora van
  verticales, con la palma hacia el centro y el pulgar arriba, que es como
  uno lleva la mano.

### Nuevo
- **El arma que llevas se ve en la mano.** Las cinco tienen su forma y su
  material: el garrote de roble, el pico de hierro, el aguijon de cristal,
  la maza de basalto y el hacha de vetagris. Acompanan el envion del golpe.

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
- Firma SHA-256: `A3:72:F9:5E:9E:EC:57:46:A2:66:8C:DA:F3:1C:56:6A:FC:97:39:B0:A4:49:A5:C1:6A:FA:66:86:52:4E:31:EE`
```
