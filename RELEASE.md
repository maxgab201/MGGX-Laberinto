# Para crear la release de GitHub

Listo para copiar y pegar tal cual en "Create a new release" con el tag
`v1.9.4`, adjuntando `apks/MGGX-Laberinto-1.9.4.apk`. También la puede
publicar sola el workflow "Publicar todas las releases" (`workflow_dispatch`,
escribiendo `SI`).

---

## Titulo

```
MGGX Laberinto 1.9.4 - Mirarles la Cara
```

## Descripcion

```markdown
## MGGX Laberinto 1.9.4 - Mirarles la Cara

Juego de laberintos en 3D, primera persona, para Android. Nativo (Kotlin +
OpenGL ES 3.0), sin motores de terceros y sin un solo archivo de arte o de
audio: la roca, los brazos, los iconos, la musica y los efectos se generan
por codigo.

Los bichos eran lo unico del juego que nunca se habia MIRADO. El visor de
mallas podia fotografiar sus piezas sueltas, pero el armado —que pieza va
donde, con que escala, a que altura— vivia adentro del renderer, y ahi estaban
los errores. Se saco afuera, se les saco la primera foto entera, y se los
midio.

### Lo que aparecio
**Los cinco se dibujaban entre un 30% y un 54% mas chicos de lo que el juego
los trata.** Y eso no es estetico: `alto` y `radio` deciden por que huecos
entra un bicho y a que distancia te muerde. Estabas esquivando una caja que no
podias ver.

- **El Guardian de roca era un busto.** Torso, cabeza, brazos y nada abajo de
  la cintura: una estatua serruchada al ombligo, flotando 10 cm, de 1,31 m
  cuando el juego la trata como de 1,85. Ahora tiene piernas, pisa el piso y
  desde el ojo del jugador te saca una cabeza.
- **El Topo era un chorizo aplastado** de 28 cm, flotando, con dos palas que
  salian de costado como aletas. Ahora es compacto, apoyado, y las palas
  rastrillan hacia abajo como las de un topo.
- **El Rastrero eran tres platos separados.** Ahora el caparazon es un domo y
  las tres placas se leen como un cuerpo.
- **La Arana y el Murcielago**, a su tamano real. El murcielago ademas recupero
  los ojos, que se habian perdido en el camino.
- **Los bichos caminaban sobre un piso que no existe** (el plano teorico del
  mapa, no la roca dibujada), asi que quedaban despegados de su propia sombra.

### Como se hizo
495 tests en verde (eran 482). El armado de cada bicho ahora se puede
fotografiar y medir en un test, que es lo que permitio encontrar todo esto. Y
se paso el cambio entero por una revision antes de cerrar: salieron seis cosas
mas, incluidos los ojos perdidos del murcielago y una tabla que quedaba vieja
al romper una pared con el pico.

### Para instalarlo
1. Bajate el APK de aca abajo directo al celular.
2. Abrilo. Android te va a pedir permiso para instalar apps de origen
   desconocido: dale que si.
3. Listo. No hace falta desinstalar la version anterior: todas las
   publicaciones van firmadas con la misma clave.

### Datos
- Android 7.0 (API 24) o superior
- Necesita OpenGL ES 3.0
- Anda sin internet. Lo unico que necesita conexion es el multijugador.
- Firma SHA-256: `A3:72:F9:5E:9E:EC:57:46:A2:66:8C:DA:F3:1C:56:6A:FC:97:39:B0:A4:49:A5:C1:6A:FA:66:86:52:4E:31:EE`
```
