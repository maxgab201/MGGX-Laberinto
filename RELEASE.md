# Para crear la release de GitHub

Listo para copiar y pegar tal cual en "Create a new release" con el tag
`v1.9.5`, adjuntando `apks/MGGX-Laberinto-1.9.5.apk`. También la puede
publicar sola el workflow "Publicar todas las releases" (`workflow_dispatch`,
escribiendo `SI`).

---

## Titulo

```
MGGX Laberinto 1.9.5 - La Salida
```

## Descripcion

```markdown
## MGGX Laberinto 1.9.5 - La Salida

Juego de laberintos en 3D, primera persona, para Android. Nativo (Kotlin +
OpenGL ES 3.0), sin motores de terceros y sin un solo archivo de arte o de
audio: la roca, los brazos, los iconos, la musica y los efectos se generan
por codigo.

Esta version hace tres cosas: le da un final al juego, le da forma a la cueva,
y termina de modelar lo ultimo que quedaba hecho con cubos y cilindros
genericos.

### El juego ahora termina
El **nivel 999** es la ultima galeria, y es corta a proposito: un zigzag que
sube por escalones y escaleras hasta que se te abre el techo y salis al aire
libre, al **patio de tu casa**. Pasto, camino de losas, la casa con su puerta y
su ventana de postigos, el cerco, un arbol con mesa y sillas, el banco, las
macetas, la ropa tendida.

Lo que mas se siente es la luz: los ultimos metros del tunel se van aclarando
**antes** de que veas la boca, que es exactamente como se sale de una cueva de
verdad.

### La cueva tenia fideos, no terreno
La altura de cada casilla se propagaba por los PASILLOS, no por el espacio: dos
galerias separadas por una sola pared podian quedar a dos metros de desnivel
porque el relieve habia llegado a cada una por un camino distinto. Se veia
—recien ahora, con un visor nuevo que dibuja la planta del nivel en colores—
como fideos de colores.

Ahora la altura sale de un campo de terreno. Aparecen zonas: un ala alta, una
hondonada, una pendiente larga. Y como el agua se llena hasta una cota, los
charcos sueltos pasan a ser un **lago** en la parte baja del nivel.

**Y todas las galerias median exactamente lo mismo.** El techo era una
constante de 3,40 m en toda la cueva. Ahora hay salones de 5,40 y galerias de
2,70. De paso, lo que te obligaba a agacharse bajo de casi un tercio del nivel
a poco mas de uno de cada diez.

### Las trampas, miradas por primera vez
- **El pozo era un baul**: una caja negra apoyada en el piso, 39 cm de cubo a
  la vista. Ahora es un agujero con brocal de piedras y tablas podridas.
- **La tapa de los pinches era un durmiente de vias**. Ahora es una losa
  partida, con una mitad hundida.
- **El vapor salia de un lavarropas** y las bocanadas eran diamantes
  facetados. Ahora es una raja en la roca con costra mineral y vapor blando.

### Y lo ultimo que quedaba con cubos
La via de la vagoneta (que la mitad de las veces cruzaba el pasillo de lado a
lado en vez de correr a lo largo), las columnas de las ruinas (que median hasta
4,76 m y **salian por el techo**) y el marco de entibado de la mina. Con esto,
el cubo y el cilindro genericos ya no los usa nadie en toda la cueva.

### Como se hizo
526 tests en verde (eran 495). Todo lo de arriba salio del mismo metodo:
construir la herramienta que falta, mirar, medir, y recien ahi tocar. Cada
test nuevo se verifico al reves — con la geometria vieja tiene que fallar.

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
