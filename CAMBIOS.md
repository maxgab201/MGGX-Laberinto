# MGGX Laberinto 2.0.0

**Fecha:** 2026-09-14
**Version anterior:** 1.9.5

## Que se hizo

La 1.9.5 le dio un final al juego: el nivel 999 sale a un patio al aire
libre. Esta version es ESE patio, y nada mas que ese patio.

Y hacia falta, porque cuando se lo fotografio de verdad —parado en la boca del
tunel, que es el unico lugar desde donde se lo va a ver— lo que habia era una
losa gris de quince metros con unos yuyos adelante. Le faltaban las dos cosas
mas basicas de estar afuera: **no habia cielo y no habia pasto.**

---

## 1. El cielo

Mirar para arriba en el patio mostraba el color de borrado: un gris plano de
punta a punta. Adentro de una cueva eso no molesta —nunca se ve— pero afuera
es lo primero que mira cualquiera.

Ahora hay un domo con degrade de horizonte a cenit, sol con disco y halo,
nubes que se mueven y bruma de valle por debajo del horizonte. Es media tarde
a proposito: el sol bajo y calido es lo que hace que salir de la mina se
sienta un final y no una hora cualquiera.

Tecnicamente es un triangulo que tapa la pantalla, dibujado ANTES del mundo y
sin tocar el buffer de profundidad: todo lo demas se dibuja encima y el cielo
solo aparece donde de verdad hay agujero. Se saltea en los 998 niveles que
pasan bajo tierra. Y aparece con `luzDeAfuera`, la misma cuenta que ya mueve
la niebla y el ambiente, asi que no hay ningun momento en que la imagen cambie
de golpe.

### Como se pudo mirar

El cielo se dibuja en la GPU y en la JVM no hay OpenGL, asi que iba camino a
ser la unica cosa del juego que se manda al telefono sin haberla visto nunca.

`VisorDeCielo` (test) es una copia en Kotlin de la cuenta, que rasteriza el
cielo a un PNG. Para que las dos copias no se despeguen, los NUMEROS son uno
solo: viven en `gl/PaletaDelCielo.kt` y de ahi salen tanto los uniforms del
shader como el visor.

Y mirandolo salieron dos cosas de una: el cielo se veia NUBLADO de punta a
punta (el horizonte en 0.74 queda en un gris de 0.71 despues del mapeo de
tonos, y se comia el azul justo en la franja que mira el jugador al salir) y
mirando para arriba no habia ni una nube, porque la proyeccion del plano de
nubes leia una sola celda de ruido en el cenit.

## 2. El pasto

El piso de las casillas de cielo abierto usaba la misma capa de roca de cueva
que una galeria: salias a la superficie y el suelo seguia siendo el canto
rodado gris de trescientos metros mas abajo.

Ahora el atlas de texturas tiene una quinta capa de pasto —tres tamanos de
grumo y pelones de tierra— que se genera SOLO cuando el nivel sale afuera. No
se tine con el tema: afuera el pasto es verde aunque hayas salido de la sima
de obsidiana.

La primera version peinaba las hojas con una senoidal y en la muestra se veia
un cuadrille: un tejido, no un pasto. Cualquier patron regular sobre una
superficie organica se delata solo, por fino que sea, porque el ojo encuentra
la repeticion antes que la forma.

## 3. La casa

Desde la boca del tunel se leia como una pared de hormigon de quince metros.
Era literalmente eso: un PANEL plano repetido, una losa de revoque con un
zocalo. Le faltaba lo unico que hace que una casa se lea como casa de lejos,
que es el **techo**.

No se puede modelar la casa entera de una pieza: el pipeline de instancias
solo admite escala UNIFORME, asi que una casa de 4 m de alto mediria 4 m de
ancho. Entonces se modela UN METRO de casa —un corte transversal con zocalo,
pared, alero que vuela, dos aguas y cumbrera, extruido a lo largo— y se
encadenan. Como la extrusion trae sus dos tapas, el primero y el ultimo
cierran solos con su propio hastial.

Con tejas de media cana, chimenea (corrida del medio a proposito: centrada se
leeria como un adorno simetrico) y las ventanas colgadas del alero.

Y no ocupa el lado entero: veintiun metros de largo por cuatro de alto se leen
como un galpon, no como una casa.

## 4. El mundo del otro lado del cerco

Al otro lado del cerco no habia NADA: el mundo se terminaba en la ultima
tabla. Y el problema no era solo que se viera el vacio. Sin nada lejos, el
patio **no tiene escala**: todo lo que se ve esta a la misma distancia y el
ojo no tiene con que medir cuan grande es la casa.

Ahora hay tres anillos de cerros a distintas distancias y una treintena de
arboles, pintados con perspectiva aerea (desaturados y tirando al azul del
cielo). Que haya CAPAS es lo que da hondura: con todo a la misma distancia se
ve un telon pintado, por bien dibujado que este.

Para poder verlos hubo que bajar el borde del patio. Las casillas de cielo
tenian el "techo" en 7,5 m, y aunque no se dibuja techo, ese numero es la
altura de la pared de roca que rodea al patio: era una cantera de veintiun
metros con el cielo arriba. Ahora mide 2,45, apenas por encima de los ojos.

## 5. El patio, mas grande y mejor compuesto

Siete casillas de lado en vez de cinco (21 m). Con quince, al salir del tunel
la casa te caia encima y no habia lugar para que el camino se leyera como un
camino ni para que el arbol estuviera "lejos".

Y los modelos, rehechos uno por uno mirandolos:

| Que era | Que es |
|---|---|
| El pasto: siete lenguetas de 3 cm x 20, tiesas — un yuyal de puas | Hojas que suben por una parabola y caen de punta. La proporcion estaba mal por un orden de magnitud: una hoja de verdad es 1 a 50, no 1 a 5 |
| La copa del arbol: tres esferas — una roca sobre un palo | Diez masas parecidas y encimadas: lo que hace follaje es que el ojo deje de contarlas |
| El camino: una fila de losas de 74 cm | Ancho, dos losas por escalon, losas de siete lados desparejos |
| Las matas: una cada 62 cm en toda la superficie | En manchones, mas grandes, con pelado entre medio |
| El cerco: tablas simetricas identicas | Tablas con la punta corrida y los cantos comidos |

Mas el **farol de la puerta**: lo unico del patio que emite luz, y vale por
diez adornos. Una casa con la luz de la entrada prendida dice "te estaban
esperando" sin una sola palabra, y ademas ancla el ojo — el camino te lleva a
la puerta y el farol te dice cual es la puerta.

---

## Dos bugs que no daban ningun error

Aparecieron los dos al escribir el test del presupuesto de triangulos, que es
lo ultimo que se hizo:

- **Ochenta travesanos de cerco no se dibujaban.** El renderer reserva un cupo
  de instancias por malla y lo que se pasa se descarta EN SILENCIO: el cerco
  generaba 280 tramos y el cupo era 200. En el telefono se habrian visto
  tramos de cerco sin travesano, sin una linea en el log. El cupo ahora vive
  al lado del armado y el mismo numero lo comprueba un test.
- **El cerco solo dibujaba 84.000 triangulos**, casi la mitad del patio
  entero. El travesano era un `roundedBox` con bisel fino —300 triangulos—
  repetido cada 34 cm a lo largo de todo el perimetro, para redondear unos
  cantos que a esa escala no se ven. Pelado cuesta 12.

El patio entero baja de 189.000 triangulos a 106.000.

---

## Como se verifico

535 tests en verde (eran 526 en la 1.9.5).

Herramientas nuevas, que es de donde salio casi todo:

- `VisorDeCielo` + `CieloTest`: para poder mirar un shader desde la JVM.
- El test del presupuesto de triangulos del patio y el del cupo de instancias.
- `gl/PaletaDelCielo.kt` y `ArmadoDelPatio.cupo`: numeros compartidos entre lo
  que dibuja y lo que mide, que es lo unico que evita que las dos cuentas se
  despeguen.

## Lo que queda anotado para la proxima

El **borde del agua**: la hondura se calcula una vez por casilla con el piso
teorico, asi que el charco termina en un cuadrado de 3 m en vez de seguir la
piedra. Ahora que las cuevas tienen terreno coherente y los charcos son lagos,
se nota mas.
