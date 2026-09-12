# MGGX Laberinto 1.9.1

**Fecha:** 2026-09-12
**Version anterior:** 1.9.0

## Que se hizo

Se dejo de modelar a ciegas.

Todos los modelos del juego se escriben a mano, en codigo, y hasta ahora se
verificaban solo por NUMEROS: que las caras miren para afuera, que las
proporciones entren en un rango, que el arma cruce el puno. Eso agarra los
errores graves, pero no alcanza para decir si algo se VE bien. Un pico puede
tener la geometria impecable y estar puesto como una cola — y eso paso de
verdad en la 1.9.0.

Asi que primero se construyo la herramienta que faltaba, y despues se
rehicieron los modelos mirandolos.

## La herramienta: VisorDeMallas

Un rasterizador por software, en los tests, que le saca una foto a cualquier
malla y la deja en `build/modelos/`. No necesita Android, ni OpenGL, ni el
telefono: es un triangulo a la vez, con buffer de profundidad, escrito sobre un
array de bytes. Cuatro angulos por modelo, porque una pieza puede estar
perfecta de frente y ser un desastre de costado — que es justo el error que
esto tiene que agarrar.

`./gradlew :app:testDebugUnitTest --tests '*RetratosTest*'` y a mirar.

**La primera version del visor tenia el vector "derecha" invertido** (`f x up`
da `(-fz, 0, fx)` y estaba escrito con el signo cambiado), asi que toda la base
quedaba espejada y los modelos salian cabeza abajo: el casco se veia como un
cono y el minero con las piernas para arriba. Como el visor existe justamente
para confiar en lo que se ve, un error ahi envenena todo lo demas. Quedo
anotado en el codigo.

## Lo que se encontro mirando

### El companiero era un barril con patas

**La causa de fondo, que vale para todo el archivo: un `lathe` da seccion
CIRCULAR.** Un cuerpo de revolucion que mide 60 cm de hombro a hombro mide
tambien 60 cm de pecho a espalda, y eso no es una persona. Una persona es ancha
y CHATA. Ahora todas las piezas del cuerpo pasan por un achatado en Z: el torno
da la silueta y el achatado la convierte en un cuerpo.

Ademas:

- **El casco era un cono de 52 cm.** Dos errores juntos: el perfil bajaba de
  0.30 de radio a 0.00 casi en linea recta (un cono, no un domo), y el renderer
  lo escalaba a 0.30 de la altura del cuerpo, o sea medio metro de casco en un
  minero de 1,72 m. Se tragaba la cabeza entera.

  Lo segundo tiene una trampa que quedo escrita: **la malla mide 1 de ALTO y se
  escala por esa altura, asi que el ancho se escribe en unidades de altura.** Un
  radio de 0.47 da un casco tan ancho como alto. Un casco de minero mide 28 cm
  de ancho por 16 de alto: el radio tiene que ser ~0.85. Ahora es un domo ancho
  y bajo, con ala, cresta, visera y la carcasa del farol.
- **La cabeza era un huevo en punta**, porque el perfil saltaba de 0.25 de radio
  a 0.00 de una. Ahora la coronilla se cierra en tres anillos y es redonda. Se
  le agregaron orejas y la nariz paso de ladrillo a cuna.
- **Los brazos estaban metidos adentro del torso** y no se veian.

### El pico era una T y el garrote un megafono

Las cinco armas estaban mal, cada una a su manera:

- **Garrote**: engordaba hacia la punta y NO CERRABA. Era un cono hueco, o sea
  un megafono. Un garrote termina en un bollo, no en boca abierta.
- **Pico**: la cabeza era una barra recta cruzando el cabo, y el conjunto se
  leia como un signo mas. Lo que hace a un pico es que la cabeza se ARQUEA. Se
  arma con tres tramos a los que se les va subiendo el `roll`, que los hace
  girar alrededor del eje del cabo y dibuja el arco.
- **Hacha**: la hoja era una caja gorda al costado, o sea un martillo de un
  lado. Una hoja de hacha es una LAMINA, y ademas grande: con 10 cm de alto
  seguia leyendose como bloque, con 22 se ve el abanico del filo.
- **Maza**: la cabeza era una caja. Una caja en la punta de un palo es un
  martillo; una maza de piedra es un bulto redondeado con caras saltadas.
- **Aguijon**: era la unica que ya se leia bien. Se le sumo el pomo.

### Los objetos de mano: cinco de ocho no se reconocian

- **La antorcha era una lanza**: el trapo era un cono que se afinaba y la llama
  una aguja. Ahora la cabeza es mucho mas gorda que el palo y la llama es una
  gota ancha y corta. Y medía **medio metro**: lo agarro el test de tamano, no
  el ojo, porque en la foto —sola y centrada— se veia bien.
- **El mapa era un cano.** Se le puso la hoja que asoma, que es lo unico que
  distingue un rollo de papel de un tubo. Al primer intento quedo **flotando al
  lado**: el rollo mide 0.032 de radio y la lamina estaba centrada en 0.044, o
  sea 6 mm de aire.
- **La brujula era una lata de conserva.** Ahora tiene la tapa levantada sobre
  su bisagra, que es lo unico que la distingue.
- **El ovillo era una piedra**, y al agregarle las vueltas del hilo quedo como
  un **regalo envuelto**: las hice finas en UN eje, asi que eran placas del
  tamano de la bola. Finas en dos ejes son hebras.
- **La venda era una cascara partida al medio**, y eso destapo un bug de fondo.

### Bug: `taperedTube` no tapaba las puntas

Los tubos del juego se encadenan uno atras del otro y las puntas quedan tapadas
por el siguiente, asi que nunca se noto. Pero un tubo SUELTO sin tapas es un
cano hueco: se ve el interior y la pieza se lee como una cascara.

Ahora `taperedTube` acepta `tapas`, que cierra las dos puntas con un abanico de
triangulos con la normal a lo largo del eje. El sentido del anillo depende de
para donde mire la tapa y de si la pieza esta espejada; con el orden al reves la
tapa se ve desde adentro, o sea que no se ve.

Que las tapas quedaron bien orientadas lo confirma el test de orientacion de
caras, que ya existia: por numeros el modelo viejo estaba bien —las caras que
habia miraban todas para afuera—, **el problema eran las que faltaban**. Es
exactamente el hueco que el visor vino a tapar.

### Lo que estaba bien

Los bichos (murcielago, rastrero, guardian, topo, arana) y las estructuras
(antorcha de pared, cristal, cofre, hongo, pincho, estacion, obelisco) se
miraron uno por uno y estan bien. El frasco y la piedra tambien. Se anotan
porque "lo revise y esta bien" es informacion igual que "lo arregle".

## Verificacion

- `./gradlew :app:testDebugUnitTest`: 416 tests en verde.
- Los tests de geometria que ya existian atajaron dos errores DURANTE este
  trabajo (la antorcha de medio metro y la nariz metida adentro del craneo).
  Eso es lo que tienen que hacer: los numeros y el ojo agarran cosas distintas,
  y hacen falta los dos.
- `./gradlew :app:assembleRelease`: APK firmado con la misma clave de siempre
  (SHA-256 `A3:72:...:31:EE`).
- Las fotos de los modelos estan en `build/modelos/` despues de correr
  `RetratosTest`. No se commitean: son carpeta de build.
- Lo que sigue sin poder verificarse desde aca es como se ve todo esto CON las
  texturas y la luz del juego puestas. El visor muestra la forma, no el
  material.
