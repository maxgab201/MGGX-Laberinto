# Para crear la release de GitHub

**Esta version ya tiene una release publicada** (`v1.1.0`, nombre real "MGGX Laberinto 1.1.0 - Boca de la Cueva"). Lo de abajo es por si se quiere re-crear.

Listo para copiar y pegar tal cual en "Create a new release" con el tag
`v1.1.0`, adjuntando `apks/MGGX-Laberinto-1.1.0.apk`.

---

## Titulo

```
MGGX Laberinto 1.1.0 - Boca de la Cueva
```

## Descripcion

```markdown
## MGGX Laberinto 1.1.0 - Boca de la Cueva

Juego de laberintos en 3D, primera persona, para Android. Nativo (Kotlin +
OpenGL ES 3.0), sin motores de terceros y sin un solo archivo de arte o de
audio: la roca, los brazos, los iconos, la musica y los efectos se generan
por codigo.

### Novedades desde 1.0.0
- Controles arreglados: el eje X ya no estaba invertido (caminar de costado
  y girar la camara respondian al lado contrario).
- El reinicio de nivel es estable: cada nivel tiene semilla fija, asi que
  "Reintentar" repite el mismo laberinto en vez de tirar a otro.
- La cueva aguanta la perdida del contexto de OpenGL (pantalla apagada,
  cambio de app) sin quedar en negro.
- Ajustes visuales con el juego a la vista: vetas de mineral, brazos y
  manos con mejor terminacion.
- Workflows de GitHub Actions para tests y publicacion de releases.

### Para instalarlo
1. Bajate el APK de aca abajo directo al celular.
2. Abrilo. Android te va a pedir permiso para instalar apps de origen
   desconocido: dale que si.
3. Listo. No hace falta desinstalar la version anterior: todas las
   publicaciones van firmadas con la misma clave.

### Datos
- Android 7.0 (API 24) o superior
- Necesita OpenGL ES 3.0
- Pesa 1.3 MB
- Firma SHA-256: `A3:72:F9:5E:9E:EC:57:46:A2:66:8C:DA:F3:1C:56:6A:FC:97:39:B0:A4:49:A5:C1:6A:FA:66:86:52:4E:31:EE`
```
