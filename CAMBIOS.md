# MGGX Laberinto 1.2.0

**Fecha:** 2026-09-04
**Version anterior:** 1.1.0

## Que se hizo

La tanda mas grande de contenido nuevo hasta el momento: relieve real en la
cueva (agacharse, saltar, escaleras), ocho poderes permanentes, bichos con
inteligencia propia, cuatro biomas nuevos con sus propias texturas, seis
skins de jugador, un lobby en 3D de verdad, y el andamiaje completo (sin
conexion todavia) del modo de a varios.

## Cambios esperados

Dejar de ser un laberinto plano de una sola textura y empezar a sentirse
como un juego completo: movimiento con mas capas, contenido para progresar
(poderes, biomas, skins), amenazas reales (bichos con cabeza propia) y una
base solida para el multijugador que se iba a construir despues.

## Cambios realizados

**Relieve de cueva** (`Maze` + `ReliefGenerator`):
- cada casilla con altura de piso, alto libre y marca de escalera propias;
- el relieve se propaga por BFS de forma que el nivel sigue siendo siempre
  pasable (como mucho un escalon entre vecinas, o escalera en las dos
  puntas);
- tramos de techo bajo para agacharse o arrastrarse.

**Jugador**: tres posturas (de pie, agachado, arrastrandose) con su propia
altura de ojo, cuerpo y velocidad; gravedad y salto (0,59 m, nunca cae en un
pozo); agacharse no gasta aguante, solo correr; se agacha solo al entrar en
un tramo bajo.

**Ocho poderes permanentes**, todos caros y de una sola compra: Linterna de
Carburo (con estaciones de recarga), Reptador, Pies de Cabra, Ojo de la
Veta, Pico Eterno, Memoria de la Sima (el mapa explorado sobrevive al
reintento), Corazon de la Cueva (regeneracion continua) y Paso de Sombra.

**Bichos con cabeza propia** (`EnemyBrain`): persiguen por campo de
distancias BFS, asi que doblan las esquinas en vez de trabarse contra la
pared. Tres tipos (murcielago, rastrero ciego, guardian de roca), ninguno
nace cerca del inicio ni dentro de la roca.

**Cuatro biomas nuevos** (Mina Abandonada, Cisternas Anegadas, Bosque de
Esporas, Templo Sepultado), cada uno con textura de pared propia y su propia
poblacion de props. **Seis skins** de jugador, independientes de los
guantes, con su propio color y detalle en el shader.

**Lobby 3D de verdad**: detras de los paneles se ve, en vivo, la cueva a la
que vas a bajar, con un modo "vitrina" del renderer que no toca el estado
de la partida.

**Andamiaje del multijugador**: el protocolo de mensajes, el estado de sala,
un transporte de prueba en memoria (sin red todavia) y una pantalla honesta
de "proximamente" que explica los dos modos planeados sin ningun boton que
no haga nada.

Ademas: retoques de interfaz (mando, tutorial, pestanas de tienda), y un
freno al tamano de la malla del mundo (un test evita que suba tanto detalle
como para dejar sin memoria a un telefono modesto).

## Bugs reparados

- Las mallas instanciadas se quedaban sin capacidad con muchos objetos en
  pantalla a la vez (hongos gigantes, columnas, bichos), y algunos
  desaparecian sin ningun aviso.

## Tests

163 tests, todos en verde al cierre de esta version.
