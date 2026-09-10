# MGGX Laberinto 1.7.2

**Fecha:** 2026-09-10
**Version anterior:** 1.7.1

## Que se hizo

El mapa, que estaba espejado y contaba de mas, y el nivel 1 convertido en
tutorial. Mas el arma, que se veia pero estaba agarrada de una forma que no
existe.

## Cambios esperados

Que el mapa diga la verdad: que la derecha del mapa sea la derecha del juego,
que solo muestre por donde caminaste y que no regale ni la salida ni los
cristales. Que el pico se vea agarrado y no cruzado entre los dedos. Y que
alguien que abre el juego por primera vez aprenda a jugar jugando.

## Cambios realizados

### El mapa estaba espejado

En el minimapa, la izquierda era la derecha del juego y al reves. No era un
problema de angulo: la cuenta que rota el mapa (`MinimapMath.rotarHaciaArriba`)
armaba la matriz

```
[  cos  -sin ]
[ -sin  -cos ]
```

cuyo determinante es **-1**: eso no es una rotacion, es una rotacion CON
espejo. Lo tramposo es que cumplia igual la unica cosa que se estaba
verificando —que el frente del jugador quedara arriba— porque un espejo sobre
el eje vertical tambien deja el frente arriba. Lo que da vuelta son los
costados.

Ahora `sx` lleva el signo cambiado, el determinante da +1 y las dos cosas
valen a la vez. Y el test dejo de mirar solo el frente: verifica que la
derecha del jugador en el juego —que es `frente x arriba`, la misma que arma
`Matrix.setLookAtM` para la vista en 3D— caiga a la derecha del mapa, para
cualquier angulo. Se agrego ademas un caso que mide el determinante directo,
que es la forma corta de decir "esto no puede ser un espejo".

### El mapa mostraba lugares por los que nunca pasaste

Caminando se llamaba a `revealAround(gx, gy, 2)`: un cuadrado de 5x5 casillas
centrado en el jugador. Como los tuneles son de una casilla de ancho, eso
descubria de yapa los dos o tres pasillos paralelos del otro lado de la roca.
El mapa no era un registro de por donde anduviste, era una radiografia.

Ahora se revela la casilla que pisas y nada mas que las paredes que la tocan
(`revelarLoPisado`). Las paredes si, y no es regalar nada: son las que tenes
pegadas a la cara mientras caminas por ahi, y sin ellas el tunel caminado
queda flotando en negro sin contorno. Las casillas ABIERTAS vecinas no se
tocan: esas son camino que todavia no recorriste.

El revelado del arranque tambien dejo de ser un 7x7 de regalo.

### La salida y los vetagris ya no se ven gratis

Se dibujaban en el minimapa apenas la casilla estuviera revelada, que es
justo lo que prometen dos objetos de la tienda que se pagan:

- **Rosa de los Vientos** (reliquia): "cada 45 segundos la salida parpadea un
  instante en el minimapa".
- **Ojo de la Veta** (poder): "los cristales de vetagris y los cofres del
  nivel aparecen marcados en el mapa desde que entras".

Ahora esas marcas SOLO existen si tenes el objeto. Sin comprarlos, el mapa no
dice donde esta la salida ni donde estan los cristales: el nivel es lo que hay
que resolver, no el minimapa. Y los dos objetos pasaron a valer lo que cuestan.

### El minimapa, rediseñado

- Es un radar **redondo** con el fondo mas claro en el centro que en el borde,
  en vez de un cuadrado negro.
- El piso se **pinta** y las paredes se dibujan como **lineas en el borde de
  la casilla**. Antes era una grilla de cuadrados de dos grises parecidos;
  ahora se ve el tunel, no un mosaico.
- Lo caminado va calido y lo que te revelo un poder queda apagado: se
  distingue de un vistazo.
- Lo que esta cerca del borde se **desvanece** en vez de cortarse de golpe.
- Las trampas descubiertas pasaron a ser una **cruz**, para que no se confundan
  con un companiero de sala.
- Las marcas que valen lejos (la salida con la Rosa, los tesoros con el Ojo,
  los companieros) **se pegan al borde** apuntando para donde estan en vez de
  desaparecer del radar. Pegadas se dibujan como un aro hueco, que se lee
  "esta para alla" y no "esta aca" (`MinimapMath.pegarAlBorde`, con su test de
  que el rumbo no cambia al pegarlas).
- Hay cuatro marcas cardinales en el aro, con la del norte mas larga y clara:
  un radar que rota sin ninguna referencia fija te deja sin saber para donde
  vas en el nivel.
- El jugador es una **punta de flecha**, no un punto: se ve para donde mira.

### El pico estaba agarrado de una forma que no existe

El arma se modelaba a lo largo de -Z, o sea paralela al antebrazo, saliendo
derecho para adelante. Pero los dedos se cierran a lo ANCHO de la palma, y
desde que la mano se pone vertical (1.7.1) ese tunel quedo en vertical: un
palo horizontal no esta agarrado, esta atravesando los dedos por el medio.

El arma se sigue modelando igual —comodo: la cabeza en la punta, el mango en
el origen— y despues se levanta entera 55 grados y se calza en el puno, con el
punto de agarre justo en el hueco entre la palma y las falanges. Un cacho de
mango asoma por abajo de la mano, como cuando agarras un pico de verdad.

De regalo, la pose mejoro: con el arma levantada, el envion del golpe (que
gira el brazo hacia abajo y adelante) la baja de arriba hacia el objetivo. Es
un hachazo, no un empujon.

Cuatro tests nuevos cuidan la pose: que el arma se estire mas para arriba que
para adelante, que el mango asome por abajo del puno, y que el eje del mango
cruce el hueco de los dedos —medido interpolando, porque el mango no tiene
vertices justo a la altura de la muneca.

### El nivel 1 es un tutorial

`Tutorial.kt` es el guion, y es una maquina de estados de aritmetica pura:
`GameSession` le cuenta que paso en el cuadro y el HUD lee que paso mostrar.
Sin nada de Android ni de OpenGL adentro, asi que se verifica entero en un
test de JVM, igual que `BrazoPose` o `MinimapMath`.

Los pasos: mirar alrededor, caminar, correr, agacharse, saltar, pegar, juntar
un eco, prender la linterna (solo si la tenes), leer el mapa y buscar la
salida.

Dos decisiones que valen mas que la lista:

- **Cada paso se aprueba HACIENDO la cosa**, no tocando "siguiente": girar
  tantos grados, caminar tantos metros, estar agachado tanto tiempo. Un
  tutorial que se pasa apretando un boton no ensena nada, y el test verifica
  paso por paso que cada uno mire SU accion y no cualquiera.
- **Ningun paso te deja trabado.** Cada uno tiene un limite de segundos: si no
  lo sacas, el tutorial sigue con un mensaje mas suave. Un tutorial que te
  encierra porque no encontras el boton es peor que no tener tutorial. (El
  ultimo, "busca la salida", no vence: no hay nada despues.)

El nivel 1 ademas gano **un tramo bajo puesto a mano sobre el camino a la
salida**. El nivel 1 es plano a proposito, pero el tutorial dice "agachate,
hay tramos bajos donde es la unica forma de pasar": sin un tramo asi esa frase
seria mentira y el jugador aprenderia a apretar un boton sin entender para
que. Va sobre el camino justamente para que haya que usarlo.

El cartel del tutorial va arriba al medio, que es el unico lugar del HUD que
no pelea ni con el minimapa ni con los botones, y trae una barrita de
progreso: sin ella, girar despacio parece que no cuenta para nada.

## Verificacion

- `./gradlew :app:testDebugUnitTest`: 332 tests en verde, con 15 nuevos.
- Cada arreglo se verifico al reves: se volvio a poner el codigo viejo y se
  confirmo que los tests nuevos fallan (4 en el espejo del mapa, 1 en el
  revelado, 3 en la pose del arma). Un test que pasa con y sin el arreglo no
  esta cuidando nada.
- `./gradlew :app:assembleRelease`: APK firmado con la misma clave de siempre
  (SHA-256 `A3:72:...:31:EE`).
- Lo visual (el minimapa nuevo, el arma en la mano, el cartel del tutorial)
  queda a confirmar en un dispositivo real: no hay tests de UI de Compose en
  el repo.
