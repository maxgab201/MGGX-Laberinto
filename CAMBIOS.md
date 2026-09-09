# MGGX Laberinto 1.6.0

**Fecha:** 2026-09-09
**Version anterior:** 1.5.3

## Que se hizo

Dos tandas de trabajo que se venian acumulando sin cerrar como version
formal (por eso quedaban como "1.6.0-alpha1" y "1.6.0-alpha2" en la rama de
desarrollo), mas la tanda de bugs y pedidos de diseño reportados jugando la
build en un dispositivo real. Se juntan las dos y se cierran de una vez como
1.6.0.

## Cambios esperados

Que el juego se vea notablemente mejor (materiales fisicos, minero con
modelo nuevo), que la red de multijugador sea mas estable y que no se
pierda al companero al terminar un nivel, y que se resuelvan los bugs de
interfaz reportados (manos al reves, minimapa congelado, botones con la
prioridad de tamaño invertida, elementos pegados al borde de la pantalla).

## Cambios realizados

### Graficos y materiales (venia de "1.6.0-alpha1")

Motor de materiales fisicamente basado: GGX, Fresnel Schlick y visibilidad
Smith para las luces de la cueva y del jugador, con rugosidad diferenciada
por material (piedra, madera, hierro, cristal, materia organica, tela).
Tonemapping filmico, tangentes derivadas de UV para los mapas normales,
desplazamiento de paralaje acotado en calidad alta, y grano en coordenadas
del objeto con desvanecimiento por distancia.

Se corrigieron normales invertidas en varias mallas (octaedro, conos
invertidos, tubos conicos, alas y cuerpos animados) que hacian que ciertas
caras se vieran transparentes o del reves en el telefono — un tipo de bug
que compila sin ningun sintoma y recien se nota mirando el resultado.

Ademas: buffers de instancias reutilizados en vez de crearse de nuevo en
cada frame, el cambio de calidad grafica ya invalida las texturas del
bioma actual, y las acciones del HUD/gamepad se encolan para el hilo del
juego en vez de tocar la partida desde la UI mientras OpenGL la esta
modificando.

### Red y multijugador (venia de "1.6.0-alpha1" y esta sesion)

Saltos y escaleras sin desplazamiento horizontal ya mandan su pose (antes
el filtro de "estoy quieto" los ignoraba). El movimiento continuo ya no
reinicia el latido de conexion, lo que permitia perder JOIN/START. Un
cliente nuevo recibe la pose de un jugador quieto cada tres segundos en vez
de nunca. Los errores asincronos de Firebase se muestran en la sala y en la
partida en vez de quedar en silencio. Valores NaN/Infinity recibidos por
red ya no pueden contaminar coordenadas.

**La sala de multijugador ya no se cierra sola al terminar el nivel**: si
sigue conectada (el companero no se desconecto), la pantalla de resultado
ofrece "Bajar con tu compañero" para volver a la sala y arrancar el
siguiente nivel juntos sin tener que compartir el codigo de nuevo. "Jugar
solo" sigue disponible para el que prefiera desconectarse.

**El companero de sala ya se ve en el minimapa** en modo cooperativo, en su
posicion actual.

### El minero: rediseño completo (esta sesion)

Antes era un apilado de 12 cajas redondeadas sin brazos. Ahora es un cuerpo
de revolucion armado con las mismas herramientas de modelado que ya se
usaban para los bichos y las estructuras: piernas con bota y pantalon,
abrigo con hombros marcados, y dos brazos que cuelgan a los costados. El
casco con visera se mantiene igual.

### Manos (esta sesion)

El pulgar estaba posicionado del lado del dedo meñique en vez del indice,
en las dos manos (el brazo izquierdo es un espejo geometrico exacto del
derecho, asi que un error en el modelo base se replicaba en el par).

### Minimapa: bug de congelamiento + rediseño (esta sesion)

El minimapa dejaba de redibujarse a partir de cierto punto del nivel,
quedando trabado como un cuadrado negro con el jugador quieto: Compose
salteaba la recomposicion de ese Canvas porque su unico parametro variable
(la sesion de juego) es siempre la MISMA referencia de principio a fin del
nivel, y Compose compara parametros por referencia para decidir si puede
saltear el redibujado de un hijo. Se agrego un parametro de "tick" que
cambia en cada frame para forzar el redibujado, igual que el resto del HUD.

De paso, rediseño de fondo: el minimapa ahora rota segun hacia donde mira
la camara (en vez de quedar siempre con el norte arriba), y tiene mas
contraste entre pared, piso y camino ya recorrido.

### Interfaz (esta sesion)

Boton de golpear mas grande (56dp -> 70dp), boton de usar objeto mas chico
(66dp -> 50dp): antes era al reves. El HUD del juego (minimapa, panel de
vida/aguante) ahora respeta los insets seguros del sistema en vez de
paddings fijos en dp, para que no quede nada cortado o pegado al borde en
telefonos con notch o barra de gestos.

## Bugs reparados

- El pulgar de las manos apuntaba para el lado equivocado.
- El minero no tenia brazos y se veia como un maniqui de cajas.
- El minimapa quedaba congelado (bug de recomposicion de Compose).
- La sala de multijugador se cerraba sola al terminar un nivel.
- El companero de sala no aparecia en el minimapa.
- Varias mallas con normales invertidas se veian transparentes o del reves.
- Saltos/escaleras sin desplazamiento horizontal no viajaban por la red.
- Errores de Firebase quedaban en silencio, sin avisar en pantalla.
- Botones de accion con la prioridad de tamaño invertida.
- Elementos del HUD podian quedar pegados al borde real de la pantalla.

## Tests

Suite completa en verde, con tests nuevos para cada arreglo con logica
comprobable: el pulgar (`ArmsMeshTest.kt`), la rotacion del minimapa
(`MinimapMathTest.kt`), el nuevo modelo del minero — orientacion de caras y
proporcion de persona (`MeshWindingTest.kt`) — y que la sala de
multijugador reparta un nivel nuevo sin confundirlo con el anterior
(`PartidaEnRedTest.kt`). El aspecto visual del minero, los tamaños de
botones y el minimapa quedan a confirmar jugando en un dispositivo real: no
hay infraestructura de testing de UI de Compose en el repo.
