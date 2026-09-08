# MGGX Laberinto 1.1.0

**Fecha:** 2026-09-04
**Version anterior:** 1.0.0

## Que se hizo

Esta version es, en el codigo, un bump de numeracion: el commit que la cierra
(`Sube a la version 1.1.0`) sube el `versionCode` de 1 a 2 sin tocar nada
mas. Android no deja instalar una version encima de otra con el mismo
`versionCode`, asi que sin este paso la 1.1 no se hubiera podido instalar
sobre la 1.0 en el mismo telefono.

Todos los arreglos de fondo que se hicieron en el mismo tramo de tiempo (el
eje X invertido de los controles, la estabilidad del reinicio de nivel, los
ajustes visuales de la roca y los brazos, los workflows de CI) ya habian
quedado adentro del `versionCode` 1, o sea que ya estan documentados en
`CAMBIOS.md` de la rama `1.0.0`. Esta version los hereda tal cual.

## Cambios esperados

Poder publicar una version nueva instalable encima de la 1.0.0.

## Cambios realizados

- `versionCode`: 1 -> 2. `versionName` se mantiene en `1.1.0`.

## Bugs reparados

Ninguno nuevo en este paso puntual (ver `1.0.0` para los bugs reparados
antes del bump).

## Tests

72 tests, todos en verde al cierre de esta version.
