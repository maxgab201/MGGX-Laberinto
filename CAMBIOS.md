# MGGX Laberinto 1.3.0

**Fecha:** 2026-09-05
**Version anterior:** 1.2.0

## Que se hizo

Dos bugs reportados por el jugador, los dos reproducidos antes de tocar
nada, mas el sistema de combate completo (hasta ahora no se le podia pegar a
ningun bicho) y la iluminacion real de la cueva.

## Cambios esperados

Arreglar dos quejas concretas de uso (lo comprado que no aparecia, no poder
matar bichos) y sumar el combate como sistema, ya que los bichos de 1.2.0
solo perseguian y mordian sin que hubiera ninguna forma de defenderse mas
alla de esquivar.

## Cambios realizados

**Combate:**
- cada bicho tiene su propia vida y su recompensa en ecos al caer
  (murcielago 16, rastrero 44, guardian 120);
- se golpea hacia donde mires, en cono, y alcanza a varios bichos de una si
  te rodean;
- el golpe empuja y aturde (el aturdimiento es necesario: sin el, el empuje
  no alcanzaba porque un murcielago corre mas rapido de lo que el golpe lo
  tira);
- se pega tambien a mano limpia, a proposito: un murcielago cae en tres
  golpes sin haber comprado nada, para que nadie quede indefenso;
- cinco armas nuevas en la tienda (garrote, pico, aguijon, maza y hacha de
  vetagris), cada una con su dano, cadencia y alcance propios, con una
  pestana nueva en Equipo para elegirla;
- boton de golpear en el HUD, gatillo derecho en el mando, swing del brazo,
  destello rojo al recibir dano, y el bicho volteado desaparece.

**Iluminacion real**: las antorchas, los racimos de cristal, los hongos, las
estaciones de carburo y la salida pasan a ser luces puntuales de verdad que
alumbran la roca (antes eran manchas que brillaban). Se le pasan al shader
las mas cercanas al jugador (ocho en calidad ultra, cuatro en baja, ninguna
en la mas baja), elegidas con un barrido por frame.

## Bugs reparados

- **Lo comprado no aparecia nunca para usar**: con las ranuras rapidas
  llenas, comprar un consumible lo sumaba al stock pero no lo cargaba en
  ninguna ranura, y la partida solo mostraba las ranuras — quedaba pago y
  sin forma de usarlo. Se arreglo haciendo que las ranuras decidan el ORDEN
  del bolso, no si algo existe: la barra de la partida ahora muestra todo lo
  que tengas, con las elegidas primero. De paso las ranuras de base pasan de
  dos a tres.
- **No se podia matar a ningun bicho con nada**: no habia sistema de
  combate; se agrego entero (ver arriba).

## Tests

24 tests nuevos; 186 en total, todos en verde. Un test cuida que el tope de
luces de Kotlin y el de los shaders no se desincronicen, porque eso apagaria
la cueva sin dar ningun error.
