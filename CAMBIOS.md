# MGGX Laberinto 1.9.2

**Fecha:** 2026-09-13
**Version anterior:** 1.9.1

## Que se hizo

Una tanda entera de caceria de bugs. Ningun modelo nuevo, ninguna pantalla
nueva: se abrio el codigo a buscar cosas rotas y se arreglaron once, cada una
con su test que **falla contra el codigo viejo** (si el test pasa igual con y
sin el arreglo, el test no prueba nada).

Casi todos son bugs de los que no se ven de entrada. Son los peores: el juego
anda, nadie se queja, y mientras tanto el cronometro miente, el aguante no se
gasta y la memoria se va llenando.

---

## Lo que estaba roto

### El cronometro corria al 96%

El reloj sumaba `(dt * 1000).toLong()` en cada cuadro. A 60 cuadros por
segundo, cada `dt` vale 16,666 milisegundos y `toLong()` se queda con 16. O sea
que el reloj perdia **2,4 segundos por minuto**, y a los diez minutos ya se
habia comido 24 segundos.

Y peor: el error depende de a cuantos cuadros va el telefono. A 90 cuadros, cada
`dt` vale 11,11 ms y se trunca a 11: **se pierde el 10%**. Desde que en la 1.8.0
se agrego el cuadro adaptativo, el mismo recorrido daba un tiempo distinto
segun el brillo de la pantalla y la bateria. El record personal y el bonus de
ecos por terminar rapido dependian de eso, y en una carrera los dos telefonos
cronometraban distinto.

Ahora la cuenta se lleva en segundos de verdad y recien al final se pasa a
milisegundos.

De paso se saco una copia del tiempo congelado que vivia adentro de
`GameSession` y se descontaba sola: si el efecto "Congelar reloj" se cortaba de
golpe, el reloj seguia congelado los segundos que le quedaban a la copia
mientras el HUD —que mira el efecto, no la copia— ya lo mostraba corriendo.

### El aguante no se gastaba nunca

La condicion para correr era `stamina > 1f` a secas. En el fondo de la barra eso
se convierte en un interruptor a 60 hertz: un cuadro corres (gastando 22 por
segundo) y al siguiente no (recuperando 16), asi que volves a cruzar el umbral y
otra vez, y otra vez.

Dos cosas salian mal. Se veia y se escuchaba el tironeo, porque la velocidad y
el cabeceo cambiaban cuadro por medio. Y la barra **no llegaba nunca al cero**:
el equilibrio se para justo arriba del umbral. Con casi la mitad de los cuadros
corriendo, se cruzaba la cueva entera a paso de trote sin una gota de aguante y
sin ningun costo.

Ahora hay un escalon: cuando se acaba, se acaba, y no volves a correr hasta
recuperar casi media barra. Queda un ritmo de "corro un tramo, recupero el
aire", que es lo que el aguante tenia que hacer desde el principio.

### Los bichos te escuchaban correr aunque estuvieras caminando

Lo mismo pero del otro lado: el sigilo miraba si tenias **apretado** el boton de
correr, no si estabas corriendo. Con "correr siempre" prendido en los ajustes el
boton esta apretado toda la partida, asi que sin aguante caminabas... y los
bichos te seguian escuchando como si vinieras a los pedos.

### El Hilo de Ariadna se cortaba a mitad de camino

Dos bugs, y el primero es serio. El rastro guarda 900 puntos como mucho, y al
llegar al tope **dejaba de anotar**. Sin el hilo casi no se notaba, porque los
puntos vencen solos y hacen lugar; pero con el hilo puesto no vence ninguno, asi
que a los 675 metros caminados el hilo dejaba de dibujarse detras tuyo sin
ningun aviso — justo en un nivel grande, que es cuando se compra. Ahora es una
ventana que corre: entra el nuevo, sale el mas viejo.

El segundo: los puntos del hilo nacian con 9999 segundos de vida y nadie se los
recortaba al terminar el efecto. El objeto dice "dura 90 segundos" y en realidad
seguia dibujado **un cuarto de hora** despues.

Todo el rastro se saco de `GameSession` a una clase propia (`Rastro`), que es lo
que permite probarlo sin levantar una partida entera.

### La barrita de efectos se pasaba del 100%

Al refrescar un efecto con otro mas largo se actualizaba el tiempo restante pero
no la duracion total ni el objeto. Entonces la barra dividia el tiempo NUEVO
(largo) por el total VIEJO (corto) y se iba por arriba del tope, y el icono
seguia siendo el de la pocion vieja: el efecto era el de la larga, pero el
cartel mentia sobre cual era.

### La sala quedaba abierta al cerrar la app

`cerrar()` se llamaba en cada camino de navegacion que sale del multijugador,
pero no habia ninguno para el caso mas comun: que el jugador cierre la app, o
que Android se lleve la actividad puesta. El enlace quedaba vivo con su escucha
de Firebase enganchada, y los demas te veian de fantasma en la sala hasta que
vencia el tiempo muerto.

### El que entraba tarde veia monedas que el otro ya se habia llevado

Entre que el anfitrion reparte el ARRANQUE y que al companiero le termina de
generar la cueva y armarse la malla pasan varios segundos, y en el medio el
anfitrion ya bajo y esta jugando. Las monedas que levanto, las paredes que
rompio y las trampas que piso viajan igual, pero llegaban cuando todavia no
habia ningun nivel donde aplicarlas.

`MatchState` ya anotaba esos hechos en tres listas... que **nadie leia nunca**.
Ahora la partida se pone al dia con ellas apenas se engancha a la sala. En
cooperativo eso ademas evitaba que la misma moneda se pagara dos veces.

De paso, esas tres listas se escribian desde dos hilos sin candado, al lado de
un comentario que explica por que el resto del estado de la sala si lo tiene.

### El modo de juego se reseteaba a Carrera

Desde la 1.9.1 la sala sobrevive al fin del nivel: terminas y volves a la sala
con el mismo companero. Pero el modo elegido no sobrevivia: volvia a Carrera
solo. Jugabas un nivel en Cooperativo, volvias para bajar al siguiente, y el
anfitrion lo repartia como carrera sin que nadie tocara nada.

### El hilo del sonido quedaba vivo despues de soltarlo

Al apagar y volver a prender el audio (cambiar de pantalla, minimizar) podian
quedar dos hilos escribiendo en el mismo `AudioTrack`, y uno de ellos sobre uno
ya liberado. Ahora cada hilo es duenio de su sesion y la suelta el que la usa.

### Cinco asignaciones de memoria por cuadro

`ByteBuffer.allocateDirect` no es una asignacion comun: esa memoria vive FUERA
del monton de Java y el recolector la devuelve tarde. El renderer pedia una por
cuadro en dos lugares de la ruta de dibujo (el polvo del aire y las
calcomanias del piso): con el polvo en calidad ultra, **300 KB por segundo** de
memoria nativa que nadie devuelve hasta que el sistema empieza a apretar.
Ahora se reusa un buffer que crece solo hasta el tamano que hace falta y ahi se
queda.

En el mismo camino se saco el calculo del tinte de piel (se rehacia por
companiero y por cuadro; ahora se guarda), los colores de las armas y de los
objetos (se armaban objetos de color nuevos en cada cuadro) y una copia
completa del arreglo de calcomanias.

---

## Numeros

- **467 tests** en verde (eran 416 en la 1.9.1).
- Siete archivos de test nuevos: `RastroTest`, `RelojTest`, `AguanteTest`,
  `EfectosTest`, `BufferDirectoTest`, `EstresTest`, mas los casos nuevos de
  `PartidaEnRedTest`.
- Cada arreglo se verifico al reves: se volvio a poner el codigo viejo y se
  comprobo que el test nuevo falla. Un test que pasa igual con y sin el
  arreglo no esta probando nada.
