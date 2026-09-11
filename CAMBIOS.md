# MGGX Laberinto 1.8.0

**Fecha:** 2026-09-11
**Version anterior:** 1.7.2

## Que se hizo

Una tanda de fondo: bugs de verdad, bateria, el corte entre lo que anda sin
internet y lo que no, y una vuelta grande a los graficos (texturas, agua e
iluminacion).

## Cambios esperados

Que el telefono dure mas jugando, que la app no se quede prendida sola en la
tienda, que el multijugador avise en vez de colgarse cuando no hay red, y que
la cueva se vea como una cueva: oscura, con roca que no se repite y con agua
donde corresponde.

## Bugs encontrados y arreglados

### La pantalla se quedaba prendida para siempre

`FLAG_KEEP_SCREEN_ON` se ponia una sola vez al arrancar la app y no se sacaba
NUNCA. Dejabas el juego abierto en la tienda o en los ajustes y la pantalla
seguia encendida hasta que se acababa la bateria.

Ahora la bandera la maneja `AhorroDeEnergia` segun donde estes: jugando si
(podes estar un rato largo caminando sin tocar nada) y en pausa tambien (leer
el mapa no es estar inactivo), pero en los menus el telefono se apaga solo,
como con cualquier otra app.

### 3.700 busquedas por segundo al driver, para preguntar siempre lo mismo

El renderer llamaba a `glGetUniformLocation` **62 veces por cuadro**. Cada una
cruza a JNI y le hace al driver una busqueda por NOMBRE (string) dentro de la
tabla del programa. A 60 fps son casi 3.700 por segundo, y la respuesta no
cambia nunca: la posicion de un uniform es fija mientras el programa siga
enlazado.

Ahora se pregunta una vez por nombre (`UniformCache`) y despues sale de un
HashMap. La cache se limpia al perder el contexto de GL, porque ahi los
programas se vuelven a crear con ids nuevos y una posicion vieja escribiria en
cualquier lado (pantalla negra o colores rotos, sin ningun error).

### Dos objetos nuevos por cuadro, tirados al recolector

`elegirLuces()` creaba `arrayOfNulls<Farol>(cupo)` y `FloatArray(cupo)` en cada
cuadro: 120 objetos por segundo que no sobrevivian al cuadro siguiente. Basura
asi no se nota en cuadros por segundo, se nota en el recolector corriendo todo
el tiempo. Ahora son campos y se reusan.

### El multijugador se colgaba sin internet, sin decir nada

Este era el peor. Firebase Realtime Database, sin red, **no falla**: guarda lo
que le pediste en una cola local y promete mandarlo "cuando se pueda". Para el
jugador eso se veia como una sala que se quedaba esperando para siempre, sin
cartel y sin error. Peor que un mensaje de error es un juego que no contesta.

Ahora se pregunta si hay red ANTES de abrir la sala (`PuedeJugarEnRed`), y el
orden de los motivos importa: a alguien sin datos hay que decirle que prenda
los datos, no mandarlo a recompilar el APK. El cartel ademas aclara que el
resto del juego anda igual sin conexion.

### El ambiente oscurecido en un solo shader (bug que casi se me escapa)

Al bajar la iluminacion, el primer intento oscurecio el ambiente dentro del
shader del mundo. Pero el mundo, los objetos, los bichos y los brazos son
cinco programas distintos que leen el mismo `uAmbient`: oscurecer uno solo
dejaba a los objetos MAS claros que la roca sobre la que estan apoyados, y eso
se ve como si flotaran recortados encima de la cueva.

Ahora el factor se aplica una sola vez, al subir el uniform (`AMBIENTE`), asi
los cinco programas no pueden dejar de estar de acuerdo.

### Normales giradas en la mezcla de variantes (otro que casi se escapa)

Las variantes de roca mezclan dos muestras de la textura, una de ellas girada
0,6 radianes. La segunda muestra trae su mapa de normales **tambien girado**:
apunta 0,6 radianes al costado de donde deberia. Sin desgirarlo antes de
mezclar, el relieve queda iluminado desde una direccion que no existe, justo
en las zonas de mezcla.

### Los permisos de red entraban escondidos

El manifest del proyecto no declaraba `INTERNET` ni `ACCESS_NETWORK_STATE`:
llegaban mezclados desde el manifest de la libreria de Firebase. Un permiso que
entra escondido por una dependencia es un permiso que nadie sabe que esta, y el
dia que se cambie de servicio se rompe sin que nadie entienda por que. Ahora
estan escritos, con el comentario de para que se piden.

## Bateria

- **Cuadros por segundo segun donde estes.** Con el menu de pausa abierto la
  escena de atras esta CONGELADA: dibujarla 60 veces por segundo es pintar 60
  veces la misma imagen. Ahora va a 10. La vitrina del lobby, que es un paseo
  lento de camara, a 30. Jugando, lo que hayas elegido en Ajustes. Nunca se
  sube por arriba de eso: si pusiste 30 porque el telefono se calienta, nada te
  mete mas.
- **El HUD se refresca mas lento en pausa** (8 veces por segundo en vez de 30).
  El minimapa no es barato: recorre 169 casillas y dibuja los bordes de cada
  una, y con la partida congelada no cambia ni una barra.
- **Las texturas se generan en paralelo.** Las cuatro capas son independientes
  entre si, asi que se calculan en cuatro hilos. Medido en JVM de escritorio:
  358 ms -> 168 ms en calidad alta y 722 ms -> 258 ms en ultra. Eso corre en el
  hilo de OpenGL al preparar el nivel, o sea que era pantalla congelada al bajar
  a una cueva de bioma nuevo.

## Offline y online

El juego entero anda **sin internet**: la cueva, las texturas, los modelos, la
musica y los efectos se generan por codigo, y el perfil vive en el telefono. Lo
unico que necesita red es el multijugador, y ahora eso esta dicho en el codigo
(`PuedeJugarEnRed`), en el manifest y en el cartel que ve el jugador.

## Graficos

### Texturas: variantes, detalle de cerca y mas relieve

- **Variantes contra la repeticion.** La textura se proyecta en coordenadas del
  mundo, asi que la MISMA baldosa de 3 m se repetia por toda la cueva, y de
  lejos se leia como una lamina fotocopiada. La solucion no es una textura mas
  grande (mas carga, mas memoria, y la repeticion sigue): son DOS muestras de
  la misma textura, una girada 0,6 radianes y corrida, mezcladas con una
  mascara de frecuencia muy baja. El ojo deja de encontrar el patron. El giro
  no es de 90 grados a proposito: con un cuarto de vuelta la veta seguiria
  alineada con la grilla.
- **Detalle de cerca.** Pegado a la pared, la baldosa de 3 m se estira tanto
  que la roca queda lisa. Una segunda muestra ocho veces mas chica devuelve el
  grano fino SOLO en el primer metro y medio, que es donde el ojo lo busca. No
  cuesta ni un pixel de textura ni un milisegundo de carga: sale de la misma
  imagen leida mas de cerca.
- **Relieve mas hondo.** El parallax paso de 0,018 a 0,032: con el valor viejo
  el relieve estaba, pero tan sutil que la pared se seguia leyendo plana.
- **Sombra propia del relieve** (calidad ultra). Lo que termina de convencer de
  que una superficie tiene bultos no es el mapa de normales: es que los bultos
  SE TAPEN ENTRE SI. Se camina el mapa de altura cuatro pasos en la direccion
  de la luz; si hay algo mas alto en el camino, el pixel esta a la sombra de
  ese bulto. Como la luz principal es la antorcha que llevas, la sombra se
  mueve con vos al girar, igual que con una linterna de verdad.

### Agua con reflejo

Hay agua desde el nivel 4, y mas seguido cuanto mas hondo bajas. Funciona como
una napa: hay una altura y se llena todo lo que quede por debajo, asi que los
charcos caen solos en los pozos y en los tramos hondos, como pasaria de verdad.

Dos reglas que no se negocian y por eso el nivel se calcula, no se elige a ojo:

- **Nunca tapa el paso.** El agua sube 30 cm sobre el piso mas hondo y nada
  mas: menos que un escalon. Se vadea caminando, no hay que nadar ni saltar y
  no se puede ahogar nadie.
- **Nunca moja el arranque ni la salida.** Aparecer con los pies en el agua se
  lee como un bug aunque sea a proposito.

El reflejo no usa una segunda pasada de camara (en un telefono eso es duplicar
el costo de dibujar la cueva entera). Usa lo que de verdad se refleja en un
charco a oscuras: las LUCES. En una cueva sin cielo ni paisaje, lo que aparece
en el agua es tu antorcha, las antorchas de la pared y los cristales, estirados
en una columna temblorosa. El Fresnel hace el resto: de frente el agua es casi
transparente y se ve el fondo, de costado es un espejo.

Y no es solo decorado: **chapotear hace tanto ruido como correr**. Un tramo
inundado es un tramo donde el sigilo no te sirve, y eso cambia por donde elegis
ir. El agua ademas frena un poco (nunca por debajo del 75% de tu velocidad: si
frenara de verdad, cruzar un charco con un bicho atras seria una muerte
cantada) y se ve en el minimapa, en frio, para distinguirla del piso seco.

### Iluminacion mas baja

- La antorcha del jugador baja de 2,45 a 1,55. Con 2,45 alumbraba como un
  reflector: se veia la cueva entera hasta el fondo del pasillo.
- La caida de la luz pasa de cuadratica a cuarta potencia: concentra la luz en
  un circulo alrededor tuyo y deja el resto en penumbra.
- El ambiente baja al 52%. Es la luz "que hay porque si", sin fuente: mientras
  estuvo alta, ningun rincon quedaba negro y la linterna no se extranaba nunca.

El resultado buscado es que una antorcha de la pared o un cristal a lo lejos
signifiquen algo, que es lo que no pasaba antes.

## Verificacion

- `./gradlew :app:testDebugUnitTest`: 367 tests en verde, con 35 nuevos
  (`AguaTest`, `UniformCacheTest`, `AhorroDeEnergiaTest`, `PuedeJugarEnRedTest`
  y dos de determinismo en `TexturasTest`).
- `python3 tools/check_shaders.py`: 12 shaders, 0 con error.
- Las texturas en paralelo se verifican comparando corridas byte por byte: una
  carrera entre hilos casi nunca falla en el primer intento, asi que el test lo
  repite.
- `./gradlew :app:assembleRelease`: APK firmado con la misma clave de siempre
  (SHA-256 `A3:72:...:31:EE`).
- Lo visual (el agua, la cueva mas oscura, las variantes de roca) queda a
  confirmar en un dispositivo real: no hay forma de mirar un shader desde un
  test de JVM. En particular, la iluminacion es un cambio de gusto tanto como
  tecnico y puede necesitar otra vuelta.
