# MGGX Laberinto 1.9.4

**Fecha:** 2026-09-13
**Version anterior:** 1.9.3

## Que se hizo

Los bichos eran lo unico del juego que nunca se habia MIRADO.

Desde la 1.9.1 existe un visor: un rasterizador por software que le saca fotos
a las mallas en los tests. Con el se rehicieron el minero, las armas y los
objetos de mano, y se encontro que el pico estaba parado como un mastil en
medio de la pantalla. Pero a los bichos el visor solo les podia sacar fotos de
sus PIEZAS SUELTAS, porque el armado —que pieza va donde, con que escala, con
que giro, a que altura— vivia en 130 lineas adentro del renderer.

Es exactamente el mismo agujero que dejo pasar al pico-mastil: la malla
impecable y el error en donde se la ponia.

Se saco el armado a un archivo propio, se fotografio cada bicho ENTERO por
primera vez, y despues se lo midio contra los numeros con los que el juego lo
trata. No cerraba ni uno.

---

## Lo que aparecio al medirlos

**Los cinco bichos se dibujaban entre un 30% y un 54% mas chicos de lo que el
juego los trata.**

| Bicho | Alto que usa el juego | Alto que se dibujaba |
|---|---|---|
| Guardian de roca | 1,85 m | **1,31 m** (y flotando 9,5 cm) |
| Rastrero ciego | 0,75 m | **0,38 m** |
| Arana de sima | 0,70 m | **0,42 m** |
| Topo de veta | 0,62 m | **0,28 m** (y flotando 4,8 cm) |
| Murcielago de sima | 0,55 m | **0,26 m** |

Y no es un problema de gusto. `alto` y `radio` son lo que decide **por que
huecos entra un bicho y a que distancia te muerde**: el techo se calcula como
`clearance - alto`, y la mordida como `radio + radio del jugador + 0,18`. Si lo
que se dibuja no coincide con lo que se declara, estas esquivando una caja que
no podes ver. (Hay una tarea vieja del proyecto que dice "hitbox de los bichos
muy chica para pegarles": en su momento se agrando el alcance del golpe en vez
de arreglar esto, que era la causa.)

### El guardian era un busto

El peor de todos, y el mas grande. Torso, cabeza y dos brazos — y **nada abajo
de la cintura**. Se veia una estatua serruchada a la altura del ombligo,
flotando diez centimetros sobre el piso, mas baja que el propio jugador aunque
el juego la trate como de 1,85 m.

Ahora tiene piernas (una malla nueva), llega a su altura declarada con los pies
apoyados, y desde el ojo del jugador te saca una cabeza, que es como se tiene
que ver algo que pega 30 de dano.

### El topo era un chorizo aplastado

Se dibujaba de 28 cm de alto y 68 de largo, flotando, con dos palas que salian
para el costado como aletas. Ahora es compacto, apoyado, y las palas rastrillan
hacia abajo y hacia afuera, que es como cava un topo.

Su `alto` declarado bajo de 0,62 a 0,50 m, que es lo que de verdad mide un
bicho asi. No cambia por donde entra: la gatera mas baja que genera el relieve
mide 0,88 m, asi que entraba y sigue entrando en todas.

### El rastrero era tres platos separados

El caparazon estaba achatado a 0,62 y estirado a 1,15: un plato. Ahora es un
domo, las tres placas se leen como un cuerpo, y las patas estan a escala.

### El murcielago y la arana

Los dos al tamano que el juego les da. La arana, ademas, con las patas mas
abiertas: colgada a 1,75 m se veia un bulto con hilos.

---

## Los bichos, apoyados en la roca de verdad

Bug que se destapo solo en la 1.9.3. `maze.floorY` da la altura teorica y plana
de una casilla, pero la roca que se dibuja se abolla hasta 13 cm. Los bichos
caminaban sobre el plano teorico, o sea sobre algo que no existe.

Antes casi no se notaba. Ahora si, porque en la 1.9.3 se les puso **sombra de
contacto**, y la sombra si va sobre la roca real: el bicho y su propia sombra
quedaban separados en vertical. Un bicho despegado de su sombra se ve peor que
uno sin sombra.

Ahora los dos leen la MISMA tabla, la que el renderer ya arma para apoyar todo
lo demas. No pueden discrepar porque no hay dos numeros.

---

## Lo que salio de la revision del diff

Se repaso el cambio entero con `/code-review` antes de cerrar, y salieron seis
cosas. Todas arregladas:

- **Al murcielago se le habian perdido los dos ojos** reacomodando las piezas.
  Volaba ciego y sin el destello que avisa que te vio.
- **Al romper una pared con el pico**, la tabla de alturas y las sombras no se
  rehacian. La altura de apoyo de una casilla depende de que vecinas estan
  abiertas, asi que quedaban viejas — y ahora eso no solo movia los objetos:
  movia a los bichos.
- **La sombra del bicho se calculaba en el punto exacto y el bicho en el centro
  de la casilla.** Adentro de una casilla la roca se mueve hasta 17 cm, o sea
  mas que el bug que este cambio venia a cerrar. Ahora los dos usan el mismo
  numero.
- **Armar un bicho asignaba memoria en la ruta de dibujo**: hasta 16 objetos
  por bicho por cuadro, unas diez mil asignaciones por segundo. Ahora se reusa
  una bolsa de piezas.
- **Una sesion vieja se quedaba con la tabla del nivel nuevo.** Se suelta al
  cambiar de nivel.
- **Un comentario que mentia** sobre como se inclina la pala del topo. Decia que
  el giro en Y no afectaba la inclinacion; en realidad la espeja (que resulta
  ser lo correcto, pero por otro motivo). Un comentario asi es una trampa para
  el que lo toque despues.

---

## Numeros

- **495 tests** en verde (eran 482 en la 1.9.3).
- Cuatro archivos nuevos de test: `BichosArmadosTest` (mide el bicho armado
  contra sus numeros de juego), `RetratosDeBichosTest` (las fotos),
  `BichoArmado` (compone el bicho entero) y `BichosEnElPisoTest`.
- Verificado al reves: con el armado viejo, `BichosArmadosTest` denuncia al
  guardian por 29% de diferencia de altura y por flotar 9,5 cm.
