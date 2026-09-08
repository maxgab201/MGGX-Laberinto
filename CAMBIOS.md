# MGGX Laberinto 1.5.0

**Fecha:** 2026-09-06
**Version anterior:** 1.4.0

## Que se hizo

El modo de a varios pasa de andamiaje (preparado desde 1.2.0, con cartel de
"proximamente") a funcionar de punta a punta: se elige Firebase Realtime
Database como relay (en vez de Supabase, que limita a 2 proyectos gratis
por organizacion), se prueba contra una base real, y se completa la sala,
el puente con la partida y el dibujo del companero en la cueva.

## Cambios esperados

Terminar el multijugador que quedo pendiente: que dos telefonos puedan
jugar juntos de verdad, en Carrera o en Cooperativo, sin que el laberinto
tenga que viajar por la red (el nivel y la semilla ya alcanzan para que los
dos armen la misma cueva).

## Cambios realizados

**El relay, con Firebase**: `RelayFirebase.kt` (la parte sin SDK: path de
sala, saneo de codigo, poda de mensajes viejos) y `TransporteFirebase.kt`
(la implementacion real sobre Realtime Database, con `push`/`onChildAdded`
y poda propia cada 40 envios). Se probo contra una base real con
`tools/probar_relay.py`, que simula dos jugadores hablando el protocolo real
y verifica que los mensajes lleguen.

**El puente entre la partida y la red** (`MatchLink.kt`): manda la pose 10
veces por segundo, y solo si algo cambio (quieto no gasta mensajes); avisa
cada hecho del mundo en el momento en que pasa; late cada 3 segundos para
no ser dado de baja por inactividad.

**La sala de verdad** (`MultiplayerScreen.kt`), reemplazando el cartel de
proximamente: crear una sala con codigo de 4 letras (sin I, O, 0 ni 1, que
se confunden al dictarlas), entrar con un codigo, ver quien esta, y que el
anfitrion arranque la partida.

**El companero en la cueva** (`PlayerMeshes.kt`): cuerpo, casco y la
lucecita del casco que dice para donde mira. Se dibuja en una posicion que
persigue a la que llego (las poses llegan 10 veces por segundo, la pantalla
dibuja 60), para que no se vea a los saltos.

**Las reglas de cada modo**: en Carrera, el primero que sale gana y a los
demas se les termina la partida ahi. En Cooperativo, el que se queda sin
vida no pierde — queda tirado y un companero que se le acerque lo levanta
con media barra, pero recien despues de unos segundos en el piso (si no,
dos que van pegados no perderian nunca); se pierde solo si caen todos.

## Bugs reparados

Ninguno de version anterior — esta version es la que agrega el multijugador
funcional por primera vez, asi que no hay bugs de regresion que reparar,
mas alla de los que se encontraron y arreglaron durante el propio desarrollo
del relay (documentados en los commits de esta misma tanda: la copia de
`google-services.json` para la variante de debug, que el plugin de Google
esperaba un solo package registrado).

## Limitacion conocida

Los bichos NO viajan por la red: nacen en el mismo lugar en los dos
telefonos (la cueva es la misma), pero de ahi en mas cada uno corre su
propia cabeza y persigue a su propio jugador, asi que al rato pueden estar
en lugares distintos en cada pantalla. Documentado a proposito en
`docs/MULTIJUGADOR.md` como limitacion conocida, no como bug.

## Tests

266 tests, y el relay ademas probado contra una Realtime Database de
Firebase real con `tools/probar_relay.py`.
