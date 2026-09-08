# Para crear la release de GitHub

**Esta version nunca se publico como release** (requiere cargar antes el
secreto `GOOGLE_SERVICES_JSON` en Settings > Secrets and variables > Actions
del repositorio, o el workflow corta el build a proposito). "Grieta del Eco"
es la tercera zona no usada por ninguna otra release real.

Listo para copiar y pegar tal cual en "Create a new release" con el tag
`v1.5.2`, adjuntando `apks/MGGX-Laberinto-1.5.2.apk`.

---

## Titulo

```
MGGX Laberinto 1.5.2 - Grieta del Eco
```

## Descripcion

```markdown
## MGGX Laberinto 1.5.2 - Grieta del Eco

Juego de laberintos en 3D, primera persona, para Android. Nativo (Kotlin +
OpenGL ES 3.0), sin motores de terceros y sin un solo archivo de arte o de
audio: la roca, los brazos, los iconos, la musica y los efectos se generan
por codigo.

### Arreglado
- **El multijugador ahora anda de verdad en el APK publicado.** Las
  releases anteriores (1.5.0 y 1.5.1) salian sin la configuracion de
  Firebase por un detalle del proceso de compilacion en GitHub Actions, asi
  que el modo de a varios nunca podia conectar para quien las bajara.

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
