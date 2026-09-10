# MGGX Laberinto 1.7.0

**Fecha:** 2026-09-10
**Version anterior:** 1.6.0

## Que se hizo

Una tanda larga de arreglos y mejoras salida de jugar la 1.6.0 en un
telefono real. Casi todo lo de aca abajo lo reporto el usuario jugando, no
salio de revisar codigo.

## Cambios esperados

Que la interfaz responda cuando se la toca, que el minimapa sirva, que no
queden cosas fuera de pantalla, que agacharse y pelear se sientan bien, y
que la cueva se vea mas cueva.

## Cambios realizados

### El bug de fondo de la interfaz: strong skipping de Compose

Tres sintomas distintos tenian una sola causa. Desde Kotlin 2.0 el
compilador de Compose usa "strong skipping", que compara los parametros de
un composable por IDENTIDAD. Todo lo que reciba un objeto que siempre es el
mismo —los ajustes, la sesion de juego— se saltea el redibujado para
siempre, aunque por dentro haya cambiado todo.

- **Los ajustes** pasan a ser estado observable de Compose (un
  `mutableStateOf` por campo). Cada pantalla se suscribe sola al ajuste que
  lee. Antes tocabas algo, no pasaba nada, y recien se veia al cambiar de
  pestana y volver.
- **El minimapa** lee su tick DENTRO de la lambda del Canvas y no en el
  cuerpo del composable. Compose memoriza esa lambda segun lo que captura:
  leyendolo afuera, la lambda nunca cambiaba y el Canvas se dibujaba UNA
  sola vez. Eso era el "cuadrado negro que no se desbloquea ni se mueve".

### La UI que se iba abajo de la pantalla

El menu lateral de Ajustes cortaba "Partida" y "Datos"; el del lobby
cortaba "Bajar acompanado". Ahora esas columnas tienen scroll propio, la
pantalla de resultado tambien, y todas las pantallas de menu respetan los
bordes seguros (notch, barra de gestos) sin perder el fondo a sangre.

### Agacharse

Dos causas reales de ver a traves de las paredes:

1. El alto libre de una casilla es TEORICO y plano, pero el techo que se
   dibuja se abolla hacia abajo. Un tramo "para agacharse" de 1,26 dejaba
   1,12 de hueco real y el cuerpo agachado mide 1,20: el juego dejaba pasar
   por donde la roca no daba. Ahora `Maze.altoLibreReal()` descuenta esa
   panza y sale de la misma cuenta que usa el armado de la malla, para que
   no puedan desincronizarse.
2. Mientras la altura del ojo bajaba suave, la camara pasaba medio segundo
   adentro de la roca. Ahora el ojo se recorta contra el techo real siempre.

Ademas las gateras son mas largas (3 a 5 casillas) y con altura elegida para
que el hueco real le entre a la postura que corresponde.

### Bichos

- Respetan el relieve: cada uno tiene su alto, no entra donde no le da el
  techo, y el que vuela baja cuando el hueco se achica. Antes cruzaban los
  techos bajos como si nada.
- **Golpearlos**: el alcance se medi­a de centro a centro sin contar el
  cuerpo del jugador. Ahora hay un margen de regalo, el cono es mas ancho y
  la tolerancia de altura mas generosa. A lo que esta a la espalda se le
  sigue sin pegar.
- **Mira nueva** en el centro de la pantalla, que sale de la MISMA cuenta
  que el golpe: si fueran dos cuentas parecidas pero distintas, mentiria
  justo en el limite.
- **Dos clases nuevas**: Topo de veta (rapido, a ras del piso, el unico que
  entra en las gateras) y Arana de sima (cuelga a media altura, pega fuerte,
  lenta).

### Trampas

Se dibujaban solo si estaban descubiertas, y descubrirlas dependia de un
poder comprado: para el que arranca, la primera noticia de una trampa era el
golpe. Ahora se ven de cerca a simple vista, y las del piso se saltan de
verdad (antes la trampa solo miraba la distancia en planta y te agarraba en
el aire igual).

### Escalera

Eran travesanos sueltos flotando contra la roca. Ahora tiene los dos
largueros, los travesanos montados por delante, y asoma por arriba del
escalon. Ademas se sube mas rapido: mientras dura la subida el cuerpo
atraviesa el escalon, y a la velocidad vieja se sentia como quedarse trabado.

### Companeros de sala

Caminan. El reloj de la animacion son los METROS caminados y no el tiempo,
asi que las piernas se mueven cuando avanzan y se quedan quietas cuando
estan parados, sin mandar un solo byte extra por la red.

### Cueva y presentacion

- Mas camaras y mas grandes, con las esquinas sin abrir (un rectangulo
  perfecto se lee como un cuarto), y casi el doble de erosion.
- Mas antorchas, estalagmitas, cristales, rocas, hongos y entibado.
- Vetagris: siempre hay al menos uno por nivel, y los descubiertos quedan
  marcados en el minimapa junto con los cofres.
- Texturas con manchones grandes de tono y mas resolucion en calidad alta.
- Botones del HUD en dos filas, con golpear ultimo y mas grande.
- Se saco la tiza entera del juego: no funcionaba.

## Bugs reparados

- Los ajustes no se actualizaban al tocarlos.
- El minimapa no se desbloqueaba ni se movia.
- Secciones de menu que quedaban fuera de pantalla y sin scroll.
- La camara entraba en la roca al agacharse.
- Los bichos atravesaban los techos bajos y volaban dentro de la piedra.
- Habia que estar encima de un bicho para poder pegarle.
- Las trampas eran invisibles hasta que te pegaban, y saltarlas no servia.
- Triangulos dados vuelta en las tiras que cierran los escalones de piso y
  de techo: se abollaban una cantidad fija aunque midieran un centimetro y
  se plegaban sobre si mismas. Estaba desde antes y aparecia en 9 de 54
  cuevas probadas.
- El boton de multijugador seguia diciendo "proximamente".

## Tests

Suite completa en verde, con tests nuevos para cada arreglo con logica
comprobable: alcance del golpe y acuerdo entre la mira y el golpe
(`CombateTest`), trampas que se saltan y que se descubren solas
(`GameSessionTest`), bichos nuevos que aparecen de verdad y gateras que
frenan al guardian pero no al topo (`EnemigosTest`), el reloj de la
caminata (`MultijugadorTest`), y orientacion y tamano de las mallas nuevas
(`MeshWindingTest`, `ModeladoTest`). El test de orientacion de la cueva
pasa a probar varias semillas por nivel: con una sola por nivel dejaba
pasar en verde el bug de los triangulos invertidos por pura suerte.

El aspecto visual (bichos nuevos, escalera, texturas, minimapa, mira y
distribucion de botones) queda a confirmar jugando en un dispositivo real:
no hay infraestructura de testing de UI de Compose ni de render en el repo.
