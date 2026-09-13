# MGGX Laberinto 1.9.3

**Fecha:** 2026-09-13
**Version anterior:** 1.9.2

## Que se hizo

Todo lo de esta version es lo MISMO bug, repetido en cinco lugares distintos:
**las cosas no estaban donde se ve que esta la roca.**

La cueva no es un pasillo de cajas. Cada cara de roca se subdivide y se corre
con ruido, y encima lleva un perfil fijo que le da panza a las paredes y arco al
techo. Eso es lo que la hace parecer una cueva. Pero todo lo que se APOYA en esa
roca —las antorchas, las monedas, los hongos, las piedras, las trampas— se
ubicaba contra el borde TEORICO de la casilla, que es un plano que no existe en
ninguna parte de la pantalla.

El resultado era un nivel entero levemente despegado de si mismo.

---

## Lo que estaba roto

### Antorchas flotando a medio metro de la pared

El plano nominal de una pared es el borde de la casilla. La roca que se dibuja
esta desplazada: hasta 30 cm para cualquiera de los dos lados por el ruido, y 22
cm mas hacia afuera por la panza del tunel — y las dos cosas tienen su maximo a
MEDIA ALTURA, que es justo donde va colgada una antorcha.

Se colgaban a un corrimiento fijo desde el centro de la casilla. Medido contra
la malla que se dibuja de verdad, la peor quedaba a **31 cm de la roca**,
colgada del aire.

Ahora se pregunta donde cae la pared de verdad a la altura de la placa de
anclaje y se apoya ahi, con un par de centimetros metidos en la roca para que no
se vea luz por atras.

### Antorchas metidas en el techo

El otro caso, y mas feo. La antorcha se clavaba siempre a 1,75 m del piso. En
una gatera de un metro de alto eso queda del OTRO LADO de la roca: se veia la
punta de la llama saliendo del techo y la luz alumbrando desde adentro de la
pared.

Ahora baja lo necesario para entrar, y si no entra ni bajandola no se dibuja
(mejor ninguna antorcha que media antorcha adentro de la piedra). Y el generador
ya ni las pone ahi: elige casillas con techo, y las bajas quedan de reserva para
un nivel que sea todo gateras.

Tambien: una antorcha cuya pared rompiste con el pico ya no se dibuja colgada de
la nada.

### Todo lo del piso, flotando o medio enterrado

El mismo bug abajo. `floorY` da la altura teorica y plana de la casilla, pero la
roca del piso se abolla hasta 13 cm para arriba o para abajo, **con el maximo
justo en el centro de la casilla** — o sea exactamente donde se apoya cada
objeto. No era un error al azar: caia siempre donde mas se nota.

Monedas colgadas de un palmo de aire, hongos saliendo de la nada, piedras a
medio hundir, cofres flotando, pinches de trampa sin piso.

Ahora hay una tabla con la altura real de apoyo de cada casilla, armada una sola
vez al cargar el nivel, y todo se apoya en ella: monedas, cristales, hongos,
piedras, estalagmitas, cofres, trampas, estaciones de carburo, vigas y la salida.

### El pico parecia un mastil

El arma se modela apuntando para adelante y se levantaba 55 grados. Pero apunta
casi de frente a la camara, asi que el escorzo se come el angulo: en pantalla
quedaba PARADA, un palo vertical en medio del cuadro, con la cabeza rozando el
borde de arriba y cerca de la mira.

Y no se arreglaba bajandole el cabeceo — se probo, y en pantalla no cambia nada.
Lo que lo cambia es LADEARLA. Ahora el arma se acuesta hacia afuera, cruza el
cuadro en diagonal, deja la mira limpia y la cabeza entra entera. Se ve como un
pico agarrado y no como un trofeo.

El objeto de la mano izquierda va ladeado para el otro lado, que ademas lo saca
de atras del puno y lo deja verse.

---

## Lo que se agrego

### Sombras de contacto

La mejora visual mas grande de la tanda, y la que hace que el arreglo de las
alturas se note. Una mancha oscura y blanda abajo de cada cosa.

Sin ella el ojo no tiene con que juzgar a que altura esta algo, y todo parece
flotar un poco por mas que los numeros esten bien. Va en las estalagmitas, las
piedras, los hongos, los cristales, las estaciones, las vigas, las trampas, la
salida, los bichos y el companiero.

La del bicho ademas dice a que ALTURA esta: un murcielago volando alto deja una
mancha ancha y palida, y al bajar a morderte se le achica y se le oscurece. Es
la unica pista de altura que hay en una cueva sin sol.

Las fijas se arman una sola vez por nivel (son cientos y no se mueven) y se
apagan en calidad baja, que es donde el relleno de pantalla duele.

### Una vista nueva en el visor de mallas

El visor que se construyo en la 1.9.1 orbita alrededor del modelo y lo encuadra
solo, que sirve para juzgar la forma de una pieza suelta. Pero un arma no se
juzga por su forma: se juzga por DONDE CAE EN LA PANTALLA, con la camara en el
ojo y la apertura del juego.

Ahora el visor tambien saca esa foto. Es la que mostro que el pico estaba
parado, algo que ninguna de las otras cuatro vistas podia mostrar.

---

## Los tests que estaban mal

Dos tests daban verde sobre codigo roto, y los dos por el mismo motivo: median
contra el plano teorico en vez de contra la roca.

- `elBrazoDeLaAntorchaLlegaHastaLaPared` comparaba el largo del brazo contra lo
  que faltaba hasta el BORDE DE LA CASILLA. Daba verde mientras las antorchas se
  veian flotando.
- `elMangoPasaPorDondeSeCierranLosDedos` buscaba los vertices del palo filtrando
  por |x| chico. Dejo de encontrar nada al ladear el arma — y ademas medir
  vertices no sirve para un tubo, que solo tiene anillos cada diez o quince
  centimetros: ahora mide contra las ARISTAS de la malla.

---

## Numeros

- **482 tests** en verde (eran 467 en la 1.9.2).
- Tres archivos de test nuevos: `AntorchasPegadasTest` (mide contra la malla que
  se dibuja de verdad), `ArmaEnPantallaTest` (proyecta el arma como la ve el
  jugador) y `PropsApoyadosTest`.
- Cada arreglo verificado al reves: con el codigo viejo, `AntorchasPegadasTest`
  falla en 3 de 7 y `ArmaEnPantallaTest` denuncia al garrote como mastil
  (4,7 de alto por ancho).
