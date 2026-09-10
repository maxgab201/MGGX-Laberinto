# Para crear la release de GitHub

Listo para copiar y pegar tal cual en "Create a new release" con el tag
`v1.7.2`, adjuntando `apks/MGGX-Laberinto-1.7.2.apk`. También la puede
publicar sola el workflow "Publicar todas las releases" (`workflow_dispatch`,
escribiendo `SI`).

---

## Titulo

```
MGGX Laberinto 1.7.2 - El Primer Descenso
```

## Descripcion

```markdown
## MGGX Laberinto 1.7.2 - El Primer Descenso

Juego de laberintos en 3D, primera persona, para Android. Nativo (Kotlin +
OpenGL ES 3.0), sin motores de terceros y sin un solo archivo de arte o de
audio: la roca, los brazos, los iconos, la musica y los efectos se generan
por codigo.

El mapa, que mentia de tres formas distintas, y el nivel 1 convertido en
tutorial.

### Arreglado
- **El mapa estaba espejado.** La izquierda del mapa era la derecha del
  juego, literal. No era un problema de angulo: la cuenta que lo rota tenia
  determinante -1, o sea que no era una rotacion sino una rotacion CON
  espejo. Cumplia igual lo unico que se estaba verificando (que el frente
  quedara arriba), porque un espejo tambien deja el frente arriba. Lo que da
  vuelta son los costados.
- **El mapa mostraba lugares por los que nunca pasaste.** Descubria un
  cuadrado de 5x5 casillas alrededor tuyo, y como los tuneles son de una
  casilla de ancho, eso te regalaba los pasillos paralelos del otro lado de
  la roca. Ahora se dibuja la casilla que pisas y las paredes que la tocan, y
  nada mas.
- **El pico ya no cruza los dedos.** Estaba modelado paralelo al antebrazo,
  saliendo derecho para adelante; pero los dedos se cierran a lo ancho de la
  palma, y desde que la mano se puso vertical ese hueco quedo en vertical.
  Ahora el arma va levantada y el mango pasa por el puno, con un cacho
  asomando por abajo de la mano.

### Cambiado
- **La salida y los vetagris ya no se ven gratis en el mapa.** Eso es lo que
  prometen dos objetos de la tienda que se pagan: la Rosa de los Vientos y el
  Ojo de la Veta. Ahora esas marcas solo existen si los tenes, y el nivel es
  lo que hay que resolver, no el minimapa.
- **Minimapa rediseñado.** Radar redondo con el fondo iluminado al centro; el
  piso pintado y las paredes como lineas en el borde de la casilla, asi se ve
  el tunel y no un mosaico; lo caminado calido y lo revelado por un poder
  apagado; lo que esta lejos se desvanece contra el borde; las trampas pasan
  a ser una cruz para no confundirse con un companiero; las marcas que valen
  lejos se pegan al borde apuntando para donde estan; cuatro marcas
  cardinales con el norte mas claro; y el jugador es una punta de flecha, no
  un punto.

### Nuevo
- **El nivel 1 es un tutorial.** Mirar, caminar, correr, agacharse, saltar,
  pegar, juntar un eco, la linterna, el mapa y la salida. Cada paso se
  aprueba HACIENDO la cosa, no tocando "siguiente"; y ninguno te deja
  trabado: si no lo sacas en un rato, el tutorial sigue solo.
- El nivel 1 ahora tiene un tramo bajo sobre el camino a la salida, para que
  el paso "agachate" ensene algo de verdad en vez de hacer apretar un boton
  al pedo.

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
