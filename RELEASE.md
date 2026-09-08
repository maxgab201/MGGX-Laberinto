# Para crear la release de GitHub

**Esta version nunca se publico como release.** El nombre de zona
"Corredores de Obsidiana" es el primero que no esta usado por ninguna otra
release real (v1.0, v1.1.0, v1.2.0, v1.3.0, v1.3.1, v1.5.0 y v1.5.1 ya usaron
las seis primeras zonas de la lista) — si se publica en algun momento futuro,
usar este nombre evita que choque con una zona ya usada.

Listo para copiar y pegar tal cual en "Create a new release" con el tag
`v1.3.2`, adjuntando `apks/MGGX-Laberinto-1.3.2.apk`.

---

## Titulo

```
MGGX Laberinto 1.3.2 - Corredores de Obsidiana
```

## Descripcion

```markdown
## MGGX Laberinto 1.3.2 - Corredores de Obsidiana

Juego de laberintos en 3D, primera persona, para Android. Nativo (Kotlin +
OpenGL ES 3.0), sin motores de terceros y sin un solo archivo de arte o de
audio: la roca, los brazos, los iconos, la musica y los efectos se generan
por codigo.

### Arreglado
- Los bichos volvieron a poder morder de verdad (un bug de scope hacia que
  un efecto de "trampas sin efecto" tambien anulara sus mordidas).
- Las marcas de tiza ya no quedan enterradas en el piso real de la cueva.
- Las monedas, estalactitas, estalagmitas y la llama de las antorchas
  quedaron con proporciones correctas.
- El golpe del brazo se ve como un golpe de verdad: antes el puno se
  acercaba a la camara en vez de alejarse hacia el objetivo.
- La pestana "Arma" de Equipo, las pestanas que se achicaban de texto sin
  reducir la fuente, y varios tamanos del HUD quedaron mas consistentes.

### Para instalarlo
1. Bajate el APK de aca abajo directo al celular.
2. Abrilo. Android te va a pedir permiso para instalar apps de origen
   desconocido: dale que si.
3. Listo. No hace falta desinstalar la version anterior: todas las
   publicaciones van firmadas con la misma clave.

### Datos
- Android 7.0 (API 24) o superior
- Necesita OpenGL ES 3.0
- Pesa 1.4 MB
- Firma SHA-256: `A3:72:F9:5E:9E:EC:57:46:A2:66:8C:DA:F3:1C:56:6A:FC:97:39:B0:A4:49:A5:C1:6A:FA:66:86:52:4E:31:EE`
```
