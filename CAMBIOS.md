# MGGX Laberinto 1.9.5

**Fecha:** 2026-09-14
**Version anterior:** 1.9.4

## Que se hizo

Tres cosas grandes, y las tres salieron del mismo metodo que viene usando el
proyecto desde la 1.9.1: **construir la herramienta que falta, mirar, medir, y
recien ahi tocar.**

1. **El juego ahora termina.** El nivel 999 es una subida corta que sale al
   patio de tu casa.
2. **La cueva pasa a tener terreno.** El relieve dejo de viajar por los
   pasillos y pasa a viajar por el espacio, y el techo dejo de ser una
   constante.
3. **Se termino de modelar todo por separado.** Las trampas, la salida, la via
   de la mina y las columnas de las ruinas eran los ultimos armados hechos con
   primitivas genericas.

---

## 1. El nivel 999: la salida

Hasta ahora el juego no terminaba: bajabas para siempre. El nivel 999 es la
ultima galeria y es **corta a proposito** — un zigzag que sube por escalones y
escaleras hasta que se te abre el techo y salis al aire libre.

`maze/ElAscenso.kt` lo genera aparte del generador normal, porque no es un
laberinto: es una escalera. `Maze` gana una marca `cielo` por casilla y
`WorldMesh` deja de dibujar techo ahi.

Lo que mas se siente es la luz. `GameSession` calcula por BFS a cuantas
casillas estas del cielo abierto y devuelve `luzDeAfuera()`; con eso el
renderer va abriendo el color de fondo, la niebla y la luz ambiente. Los
ultimos metros del tunel se aclaran **antes** de que veas la boca, que es
exactamente como se siente salir de una cueva de verdad.

## 2. El patio de casa

Al otro lado de la boca hay un patio, y esta disenado de verdad: 17 modelos
propios (`gl/PatioMeshes.kt`) y un armado puro y medible (`gl/ArmadoDelPatio.kt`).

Pasto en matas de siete hojas curvadas, un camino de losas de piedra desde la
boca de la mina hasta la puerta, la casa con su puerta de tablones, su ventana
con postigos abiertos y su alero de tejas, el cerco de madera, un arbol con
mesa y sillas abajo, un banco contra la pared, macetas, un balde de mina y la
ropa tendida.

Tres cosas se encontraron **mirando** el patio, y ninguna se habria visto de
otra forma:

- Los **travesanos del cerco eran losas negras gigantes**. El pipeline de
  instancias solo admite escala UNIFORME, asi que un travesano "estirado" a
  14,2 m tambien medía 14,2 m de alto y de ancho. Ahora son cadenas de tramos
  de 34 cm.
- La **pared de la casa se dibujaba tres veces encima de si misma**: los
  paneles avanzaban 1,05 m y cada panel mide 3,1 m de ancho.
- El **cerco del fondo tapaba la vista** justo al salir. Ahora ahi va la boca
  de la mina, que ademas es lo honesto: atras tuyo hay un cerro, no un vecino.

## 3. El relieve: la cueva tenia fideos, no terreno

Esta es la que mas cambia como se siente jugar.

La altura de cada casilla se propagaba por BFS desde el inicio tirando una
moneda en cada paso: quedate igual, subi un escalon, baja uno. Eso garantizaba
que el nivel fuera pasable, y en eso funcionaba bien. El problema es lo que
producia, y **nunca se habia podido mirar**.

Se construyo `VisorDeRelieve`, que dibuja la planta del nivel con la altura del
piso en colores y el corte a lo largo del camino a la salida. En la primera
foto estaba todo a la vista:

**La altura viajaba por los PASILLOS, no por el espacio.** En planta se veian
fideos de colores: cada corredor con su altura, sin ninguna relacion con el de
al lado. Dos galerias separadas por una sola pared podian quedar a dos metros
de desnivel, porque el relieve habia llegado a cada una por un camino distinto.
Adentro eso se siente como que la cueva no tiene forma: subis, bajas, volves a
subir, y nunca estas "en la parte alta" de ningun lado, porque no hay parte
alta.

Ahora la altura sale de un **campo de ruido suave sobre la grilla**, como un
mapa de terreno. Dos casillas cercanas en el espacio leen el mismo campo,
aunque haya una pared en el medio. Aparecen zonas: un ala alta, una hondonada,
una pendiente larga.

| | Antes | Ahora |
|---|---|---|
| Desnivel medio entre casillas pegadas por la pared | 1,58 - 2,36 escalones | **0,48 - 0,77** |

Y hay una consecuencia que no estaba buscada: el agua se llena hasta una cota,
asi que con terreno coherente **los charcos sueltos pasan a ser un lago** en la
parte baja del nivel.

Las barrancas (mesetas y hondonadas de borde cortado, con escalera en las dos
puntas) se agregan aparte, porque el campo suave nunca hace un escalon que no
se suba caminando y una cueva sin ningun corte es una loma sin sorpresas.

### Todas las galerias median exactamente lo mismo

`ceilClearance` era la constante `ALTO_NORMAL` (3,40 m) en **toda** la cueva
salvo los tramos bajos. La foto lo mostro de una: en el corte, el techo era una
linea recta de punta a punta. Eso es lo que mas la hacia sentir un pasillo de
oficina en vez de una cueva.

Ahora el techo sale de su propio campo y cae en una lista corta de alturas
(2,70 / 3,40 / 4,35 / 5,40): salones altos y galerias bajas. La lista es corta
a proposito — `WorldMesh` solo puede continuar el techo entre dos casillas si
comparten EXACTAMENTE el alto libre, asi que un techo continuo partiria la
cueva en parches de tres metros con un canto duro en cada borde.

### Y un tercio de la cueva era gatera

Con `probTechoBajo` en 0,17 y corridas de 3 a 5 casillas, entre el **22% y el
29%** de cada nivel obligaba a agacharse. Un tramo bajo se siente porque es
raro; si agacharse es la forma normal de caminar, deja de ser un momento. Baja
a **3-12%**.

## 4. Las trampas, miradas por primera vez

`gl/ArmadoDeEstructuras.kt` saca el armado de las trampas y de la salida de
adentro del renderer, igual que se hizo con los bichos en la 1.9.4. Recien con
eso se pudieron fotografiar armadas. Las tres estaban mal:

- **El pozo era un baul.** Una CAJA negra de escala 1,35 puesta bajo el piso.
  Como la escala es uniforme, al agrandarla para tapar la casilla tambien
  crecia para arriba: quedaban **39 cm de cubo negro apoyados en el suelo**.
  Ahora es un disco oscuro ancho y chato, con brocal de piedras rotas y tablas
  podridas cruzandolo.
- **La tapa de los pinches era un durmiente de vias.** Dos barras cruzadas en
  equis. Ahora es una losa partida en zigzag, con una mitad hundida y ladeada.
- **El vapor salia de un lavarropas.** Un cilindro con una caja encima —un cubo
  blanco de tres cuartos de metro— y las bocanadas dibujadas con la misma gema
  facetada que los cristales: una torre de diamantes saliendo del piso. Ahora
  es una raja larga con costra mineral a los costados y bocanadas blandas.

El disco del pozo se levanta 20 cm sobre la altura de apoyo, y no es un
capricho: la roca del piso esta abollada hasta 13 cm con el maximo en el centro
de la casilla, asi que a ras quedaria medio enterrado y se veria una media luna
negra. Es el mismo bug que dejaba las antorchas flotando, del otro lado.

## 5. Lo ultimo que quedaba con primitivas genericas

- **La via de la vagoneta** eran dos tablones cruzados en equis, y el giro
  salia de la paridad de la casilla: la mitad de las veces la "via" cruzaba el
  pasillo de lado a lado. Ahora trae cinco durmientes y los DOS rieles
  paralelos, apuntada a lo largo del corredor.
- **El fuste de columna** de las ruinas y el templo era una caja con un cilindro
  encima, y el cilindro se escalaba a `alto * 2.6`: **hasta 4,76 m en galerias
  de 3,40**. La columna salia por el techo. Ahora es un modelo propio —basa
  escalonada, fuste conico con estrias, el quiebre de arriba en un arco de
  medio giro— y se recorta contra el alto libre real de la casilla.
- **El marco de entibado de la mina** se cortaba en 3,40 m, que era el alto que
  tenian todas las galerias antes de que el techo variara; en un salon de 5,40
  quedaba flotando a media altura.

Con esto **el cubo generico y el cilindro generico ya no los usa nadie en toda
la cueva**, y se sacaron del renderer.

---

## Como se verifico

526 tests en verde (eran 495 en la 1.9.4).

Herramientas nuevas para mirar, que es de donde salio todo:

- `VisorDeRelieve` (planta en colores + corte a lo largo del camino).
- `ArmadoDeEstructuras` + `EstructurasArmadasTest` (las trampas armadas).
- `ArmadoDelPatio` + `PatioArmadoTest` y `RetratosDelPatioTest`.
- `VisorDeMallas.guardar`, para que cualquier visor use el mismo escritor de PNG.

Y la disciplina de siempre: **cada test nuevo se verifico al reves.** Con la
geometria vieja del pozo fallan los tres tests del pozo; con el generador de
relieve viejo fallan los tres del relieve.

---

## Archivos nuevos

| Archivo | Que es |
|---|---|
| `maze/ElAscenso.kt` | El nivel 999: la subida y el patio |
| `gl/PatioMeshes.kt` | Los 17 modelos del patio |
| `gl/ArmadoDelPatio.kt` | Como se arma el patio (puro, medible) |
| `gl/ArmadoDeEstructuras.kt` | Como se arman las trampas y la salida |
| `test/VisorDeRelieve.kt` | Para mirar el relieve de un nivel entero |

## Lo que queda anotado para la proxima

El **borde del agua**: la hondura se calcula una vez por casilla con el piso
teorico, asi que el charco termina en un cuadrado de 3 m en vez de seguir la
piedra. Esta identificado y no entro en esta version.
