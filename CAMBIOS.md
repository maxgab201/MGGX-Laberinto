# MGGX Laberinto 1.5.1

**Fecha:** 2026-09-06
**Version anterior:** 1.5.0

## Que se hizo

Un solo cambio, enfocado en poder diagnosticar problemas reales del
multijugador: cuando no se podia abrir una sala, el mensaje siempre decia
"fijate que tengas internet", una conclusion apurada que mandaba a buscar el
problema en el lugar equivocado.

## Cambios esperados

Que el mensaje de error, cuando el multijugador falla, diga el motivo real
en vez de una suposicion generica.

## Cambios realizados

- `abrirSala()` deja de usar un `runCatching { ... }.getOrNull()` ciego y
  devuelve el motivo real por el que no se pudo conectar: si la app no
  encuentra la configuracion de Firebase, dice exactamente eso (que no
  tiene nada que ver con la senal del telefono).

## Bugs reparados

- El mensaje de error del multijugador culpaba siempre a la senal de
  internet, incluso cuando el problema real era que la conexion todavia no
  se habia intentado siquiera (por ejemplo, si la app no encontraba la
  configuracion de Firebase). Este cambio en si mismo no resuelve el
  problema de fondo (eso llego recien en 1.5.2), pero es lo que permitio
  diagnosticarlo con precision.

## Tests

Sin tests nuevos propios: es un cambio de un mensaje de diagnostico. La
suite completa (185 tests a esta altura) siguio en verde.
