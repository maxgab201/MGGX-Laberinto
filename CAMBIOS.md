# MGGX Laberinto 1.3.1

**Fecha:** 2026-09-05
**Version anterior:** 1.3.0

## Que se hizo

Un solo bug reportado por el jugador: el cartel de "Primeros pasos" no se
podia cerrar.

## Cambios esperados

Que el tutorial se pueda cerrar siempre, sin importar el tamano de pantalla.

## Cambios realizados

- El panel del tutorial ahora se limita al 86% del alto disponible, la lista
  de puntos scrollea adentro, y el boton de cerrar queda siempre fijo y
  visible abajo, sin importar cuanto texto tenga el tutorial.

## Bugs reparados

- **El carton de "Primeros pasos" no se podia cerrar**: el tutorial habia
  crecido a siete puntos y el panel no tenia limite de altura ni scroll. En
  una pantalla chica (el juego va apaisado) el texto se desbordaba y el
  boton "Entendido, a bajar" quedaba fuera de la vista — por eso parecia que
  el carton no se podia sacar, cuando en realidad el boton solo estaba fuera
  de pantalla.

## Tests

185 tests, todos en verde al cierre de esta version.
