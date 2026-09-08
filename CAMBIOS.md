# MGGX Laberinto 1.3.2

**Fecha:** 2026-09-05
**Version anterior:** 1.3.1

## Que se hizo

Una revision objeto por objeto (visual y funcional) pedida por el usuario
antes de seguir sumando contenido: combate, tiza, proporciones de props 3D,
la animacion del golpe, y una limpieza de interfaz. Cada punto se investigo
leyendo el codigo real (no por suposiciones) antes de tocar nada.

## Cambios esperados

Corregir seis problemas concretos reportados jugando la build: combate sin
sensacion de peligro, marcas de tiza que se ven enterradas, monedas
desproporcionadas, estalactitas con forma rara, antorchas con la llama mal
apoyada, el golpe con un arco de movimiento raro, y una interfaz
inconsistente entre pantallas.

## Cambios realizados

- **Combate**: se saco un bug de scope real donde el efecto "Trampas sin
  efecto" (`INMUNE_TRAMPAS`) tambien anulaba las mordidas de los bichos, no
  solo el dano de trampas. Se bajo el empuje del golpe cuerpo a cuerpo para
  que el aturdimiento no deje al bicho siempre fuera de rango de mordida
  mientras el jugador pelea.
- **Tiza**: `WorldMesh.realFloorHeight()` calcula la altura real del piso
  (con la misma abolladura de ruido que dibuja la roca) para las marcas de
  tiza y el rastro de pisadas, que antes se posicionaban sobre un piso
  teorico plano y quedaban tapadas por la roca real.
- **Proporciones 3D**: las monedas (Eco, Eco grande, Vetagris) se achicaron a
  un tamano razonable frente al jugador — antes el `scale` que se les pasaba
  era directamente su radio en metros, y el Vetagris llegaba a medir lo
  mismo que el cuerpo entero del jugador. Las estalactitas y estalagmitas
  pasan de una proporcion de "gorro de fiesta" (2,4:1) a una punta fina
  (7,7:1). La llama de la antorcha se achica y se reubica para apoyarse en
  la punta del poste en vez de penetrarlo.
- **El golpe del brazo**: se separaron los numeros de la pose del brazo
  (`BrazoPose.kt`) de las llamadas a `android.opengl.Matrix` (que no
  calculan nada real en los tests de JVM, asi que el bug no se veia en la
  suite). Esto permitio confirmar con calculo que en el pico de la animacion
  el puno se ACERCABA a la camara en vez de alejarse hacia el objetivo, y
  que el pivote de rotacion estaba en la muneca en vez del codo (haciendo
  que el antebrazo describiera un arco de mas de medio metro). Se corrigio
  el signo y se movio el pivote al codo.
- **Interfaz**: la pestana "Arma" de Equipo usaba rojo "peligro" para marcar
  lo equipado, distinto al verde que usa el resto de la pantalla — se
  alineo al mismo patron visual. `CaveTabs` decia en un comentario que las
  pestanas "se achican solas" pero el codigo solo hacia scroll de texto sin
  reducir la fuente; se implemento el auto-achique real. Se redujeron los
  tamanos base de varios elementos del HUD (panel de vida, botones de
  accion) que se sentian grandes, respetando el slider de escala de botones
  que ya existia en Ajustes.

## Bugs reparados

- `INMUNE_TRAMPAS` anulaba tambien las mordidas de los bichos, no solo el
  dano de trampas (bug de scope).
- Las marcas de tiza y el rastro de pisadas se enterraban en el piso real en
  buena parte de cada casilla.
- Las monedas median hasta 3 veces mas de lo esperado.
- La llama de la antorcha penetraba el poste en vez de apoyarse en la punta.
- El golpe del brazo se veia mal: el puno se acercaba a la camara en el pico
  del golpe en vez de alejarse hacia el objetivo (error de signo), y el
  antebrazo describia un arco de mas de medio metro por el pivote mal puesto.

## Tests

198 tests, todos en verde al cierre de esta version. Los cambios de
interfaz (E) no tienen test automatizado posible: no hay infraestructura de
Compose UI testing en el repo, y su verificacion visual quedo pendiente de
confirmar en un dispositivo real.
