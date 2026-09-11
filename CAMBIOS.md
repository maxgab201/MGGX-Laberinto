# MGGX Laberinto 1.9.0

**Fecha:** 2026-09-11
**Version anterior:** 1.8.0

## Que se hizo

El objeto que llevas en la mano izquierda, el companiero rehecho, texturas con
vida (liquen y humedad por bioma), polvo en el aire, y la progresion de niveles
verificada de punta a punta.

## El objeto en la mano izquierda

Ahora se ve lo que tenes elegido en la barra: el pan, el mapa, el frasco, la
antorcha, el ovillo del hilo de Ariadna, la brujula, la piedra o el vendaje.

Son **ocho familias y no veinticuatro modelos**, y es a proposito: en primera
persona, de reojo y en movimiento, un tonico y un elixir son el mismo objeto.
Lo que importa es reconocer de un vistazo que llevas un frasco; el color lo da
la familia, asi que dos frascos distintos igual no se ven iguales.

Lo interesante de como esta hecho: el objeto viaja en la **misma malla que los
brazos**, marcado con `TAG_OBJETO = -2`. El signo hace las dos cosas de una: el
vertex shader elige la matriz del brazo con `aSide < 0.0`, asi que un tag
negativo ya va con la mano izquierda sin tocar nada; y el fragment shader lo
reconoce por ser menor que -1.5 para darle su propio material. Sin programa
aparte, sin otro buffer y sin otra llamada de dibujo, y acompana solo el
balanceo del brazo al caminar.

Cada familia tiene su material en el shader: el frasco tiene el liquido adentro
y el cuello transparente, la antorcha tiene llama viva, el mapa tiene las lineas
de tinta, el pan tiene corteza y miga, el ovillo tiene las vueltas del hilo, la
brujula tiene el cristal de la tapa.

Y no puede quedar ningun consumible sin modelo: uno que no este en la lista cae
en la forma generica. Un objeto sin modelo se veria como una mano vacia, y eso
se lee como que el juego perdio el objeto. Hay un test que recorre el catalogo
entero y lo verifica.

## El companiero, rehecho

- **Tiene cabeza**, con nariz, ceja y barba. Va en malla aparte del cuerpo
  porque el renderer pinta cada instancia de UN color: metida adentro del
  cuerpo, la cara saldria del color del abrigo y el companiero se veria como un
  traje vacio con un casco encima.
- **Carga cosas**: mochila con la tapa y el rollo de soga, cinturon con hebilla,
  y el pico colgado a la espalda. De todo lo que se le puede agregar a un modelo
  que se ve de lejos, esto es lo que mas rinde: a diez metros en un pasillo
  oscuro no se ve ni la cara ni la ropa, se ve la SILUETA.
- **El casco tiene la lampara montada al frente**, para que la luz salga de algo
  y no de la nada.

**Un error propio, encontrado y arreglado:** la primera version del pico
apuntaba hacia atras y medía 46 cm de saliente en un cuerpo de 1,72 m. De
costado se veia como si el companiero arrastrara una cola. Un pico se lleva
ATRAVESADO sobre los hombros, y asi quedo.

## Texturas: liquen y humedad, por bioma

Dos capas nuevas de detalle en la pared, con fuerza propia por bioma:

- **Chorreado**: regueros verticales de agua. Es la unica marca de la roca que
  tiene una direccion obligada, porque la hace la gravedad; una chorreadura
  horizontal se lee mal aunque nadie sepa decir por que. El agua **pule** la
  roca: la deja mas lisa y mas baja, y como el shader saca la rugosidad de la
  altura, la zona mojada se ve mas oscura y ademas devuelve mas brillo sin
  tocar una linea del shader.
- **Liquen**: manchones esponjosos que **suman** altura (si solo cambiaran el
  color quedarian como pintura sobre piedra lisa) y que crecen SOLO donde la
  roca esta hundida: en las grietas y las juntas, que es donde se junta la
  humedad. Repartido al azar se veria como salpicadura.

Los valores por bioma son una descripcion del lugar, no un numero decorativo:
las Cisternas Anegadas estan al maximo de humedad, las Galerias de Musgo al
maximo de verdin, y en las Venas de Magma los dos valen **cero** — no crece nada
ahi, y poner verdin seria contar una mentira sobre donde estas parado.

El liquen ademas tira al color de la veta del bioma, no a un verde generico: asi
el verdin de la mina y el del bosque de esporas no son la misma mancha pegada
sobre dos paredes distintas.

## Polvo en el aire

Es lo unico que le faltaba a la cueva para dejar de parecer un decorado: el aire
vacio no existe bajo tierra. Y encima es lo que deja **ver el haz** de la
antorcha, que hasta ahora solo se notaba cuando pegaba en una pared.

Las motas no tienen color propio: solo se ven si les pega una luz. Eso es lo que
hace que el polvo dibuje el haz en vez de flotar como puntitos blancos.

Lo que lo hace barato es que **no se guardan ni se simulan**. Hay una cantidad
fija, cada una tiene una posicion que es funcion del numero de mota y del
tiempo, y esa posicion se envuelve en una caja centrada en el jugador. Camines
lo que camines, siempre tenes las mismas motas alrededor, sin crear ni destruir
ninguna y sin estado de un cuadro al otro. Se dibujan con mezcla aditiva, que
ademas evita tener que ordenarlas.

Detalle que importa: la envoltura usa `mod` y no el resto de `%`. Con `%`, una
mota detras del origen saltaba al otro extremo de la caja, y se veia como si el
polvo desapareciera de un lado de la galeria.

## Los niveles, verificados

La progresion ya estaba bien armada; lo que faltaba era que estuviera
**cuidada**. `ProgresionTest` la fija entera:

- La cueva crece al bajar, y el crecimiento tiene techo (sin el, el nivel 400
  seria un laberinto que no entra en memoria).
- Doce biomas repartidos hasta el nivel 52, ninguno de un solo nivel, y despues
  siguen rotando. El Vetagris cae en los niveles redondos y nunca en la bajada
  inicial: si saliera en cualquier lado dejaria de ser un premio.
- Cada cosa nueva entra en su momento y no todas juntas: primero caminar,
  despues las trampas, despues los bichos, despues el agua. Asi cada tanda de
  niveles ensena una cosa.
- Mas abajo hay mas peligro y el camino es mas largo.
- **Y todo nivel del 1 al 70 se puede terminar**, con el relieve, el agua y el
  tramo bajo del tutorial encima. Es la promesa que no se rompe nunca: si un
  nivel no tiene salida, el juego se termina ahi para ese jugador.

## Verificacion

- `./gradlew :app:testDebugUnitTest`: 410 tests en verde, con 43 nuevos
  (`ObjetoEnManoTest`, `PersonajeTest`, `MotasTest`, `ProgresionTest` y dos mas
  en `TexturasTest`).
- `python3 tools/check_shaders.py`: 14 shaders, 0 con error.
- `./gradlew :app:assembleRelease`: APK firmado con la misma clave de siempre
  (SHA-256 `A3:72:...:31:EE`).
- Lo visual queda a confirmar en un dispositivo real: no hay forma de mirar un
  shader ni un modelo desde un test de JVM. Lo que si se verifica es la
  geometria (proporciones, que las caras miren para afuera, que el objeto cruce
  el puno) y la aritmetica del polvo.
