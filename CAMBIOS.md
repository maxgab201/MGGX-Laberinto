# MGGX Laberinto 1.5.3

**Fecha:** 2026-09-07
**Version anterior:** 1.5.2

## Que se hizo

Dos limitaciones del multijugador que el usuario reporto jugando: el salto
no se veia del lado del companero, y los bichos corrian cada uno por su
cuenta en cada telefono (podian terminar en lugares distintos, y no habia
forma de que persiguieran de forma coordinada a mas de un jugador). Se
resolvieron las dos, y se dejaron los APK versionados directo en el
repositorio (`apks/`) porque esta sesion de trabajo tenia bloqueada la
publicacion de releases por la API de GitHub.

## Cambios esperados

Que saltar se vea del otro lado en tiempo real, y que los bichos sean
consistentes entre los telefonos de la sala: el mismo bicho en el mismo
lugar para todos, persiguiendo al jugador que tenga mas cerca y soltandolo
si se le escapa lejos para ir por otro.

## Cambios realizados

**El salto ahora viaja por la red**: la altura (`y`) del jugador ya se
mandaba en cada mensaje de pose, pero el filtro que evita repetir mensajes
cuando no cambia nada ("estoy quieto") miraba x, z, el angulo y la postura
— y no la altura. Saltar en el lugar no cambia ninguna de esas cuatro cosas,
asi que el juego decidia que estabas quieto y no mandaba el mensaje. Ahora
la altura tambien entra en la cuenta.

**Los bichos los mueve el anfitrion**: se agrega un tipo de mensaje nuevo
(`BICHOS`) que reparte donde esta cada uno, en tandas de a doce y unas siete
veces por segundo, y solo de los que estan cerca de algun jugador (26 m) —
para no gastar mensajes en bichos que nadie ve. El resto de la sala copia
esas posiciones en vez de simular su propio movimiento. La vida de cada
bicho sigue resolviendose en cada telefono por separado, a proposito: asi
pegar y recibir dano no dependen de que llegue ningun mensaje de red.

**Los bichos ahora eligen a quien perseguir** cuando hay mas de un jugador
cerca: se quedan con el que tengan mas cerca, y no lo sueltan mientras lo
tengan a un alcance razonable (para que dos jugadores que corren juntos no
les hagan girar la cabeza sin avanzar hacia ninguno); recien cuando el
perseguido se les escapa de verdad, miran quien les quedo mas cerca y van
por ese. A un jugador caido (en Cooperativo) no lo persiguen.

**Los APK quedan versionados en el repositorio** (`apks/`), con un
`README.md` explicando que es y como verificar la firma, mientras la
publicacion de releases por API siga bloqueada para esta sesion de trabajo.

## Bugs reparados

- El salto no se veia del lado del companero en una partida de a varios.
- Los bichos podian terminar en posiciones distintas en cada telefono de la
  sala, en vez de ser el mismo bicho en el mismo lugar para todos.

## Tests

15 tests nuevos (`BichosEnRedTest.kt`), 281 en total. Los dos que cubren el
arreglo del salto y el de "soltar al perseguido cuando se escapa lejos" se
verificaron al reves: sacando el arreglo a proposito, el test correspondiente
falla; con el arreglo puesto, pasa.
