# Para crear la release de GitHub

**Esta version ya tiene una release publicada** (`v1.5.0`, nombre real
"MGGX Laberinto 1.5.0 - Pozos de Azufre"). Lo de abajo es por si se quiere
re-crear.

Listo para copiar y pegar tal cual en "Create a new release" con el tag
`v1.5.0`, adjuntando `apks/MGGX-Laberinto-1.5.0.apk`.

---

## Titulo

```
MGGX Laberinto 1.5.0 - Pozos de Azufre
```

## Descripcion

```markdown
## MGGX Laberinto 1.5.0 - Pozos de Azufre

Juego de laberintos en 3D, primera persona, para Android. Nativo (Kotlin +
OpenGL ES 3.0), sin motores de terceros y sin un solo archivo de arte o de
audio: la roca, los brazos, los iconos, la musica y los efectos se generan
por codigo.

### Multijugador terminado
El laberinto no viaja por internet: con el nivel y una semilla, los dos
telefonos arman exactamente la misma cueva. Por la red solo va donde esta
cada uno y los hechos que cambian el mundo.

- **Bajar acompanado**: armar una sala (codigo de 4 letras para dictar) o
  entrar con uno.
- **Carrera**: el primero que sale gana.
- **Cooperativo**: se comparten los ecos y te podes levantar entre ustedes.
- Requiere que cada uno tenga su propio proyecto de Firebase configurado
  (ver `docs/MULTIJUGADOR.md`); sin eso, el modo solitario sigue andando
  igual.

**Limitacion conocida**: los bichos no viajan por la red, cada telefono los
mueve por su cuenta.

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
