# Para crear la release de GitHub

Listo para copiar y pegar tal cual en "Create a new release" con el tag
`v1.7.0`, adjuntando `apks/MGGX-Laberinto-1.7.0.apk`. También la puede
publicar sola el workflow "Publicar todas las releases" (`workflow_dispatch`,
escribiendo `SI`).

---

## Titulo

```
MGGX Laberinto 1.7.0 - Cisternas Anegadas
```

## Descripcion

```markdown
## MGGX Laberinto 1.7.0 - Cisternas Anegadas

Juego de laberintos en 3D, primera persona, para Android. Nativo (Kotlin +
OpenGL ES 3.0), sin motores de terceros y sin un solo archivo de arte o de
audio: la roca, los brazos, los iconos, la musica y los efectos se generan
por codigo.

Version de arreglos grandes: casi todo lo de esta lista salio de jugar la
version anterior en un telefono y anotar lo que molestaba.

### Lo que estaba roto y ya no
- **Los ajustes ahora se ven al tocarlos.** Antes cambiabas algo y no pasaba
  nada hasta que salias de la pestana y volvias.
- **El minimapa funciona.** Se desbloquea y se mueve: antes se dibujaba una
  sola vez y quedaba congelado como un cuadrado negro.
- **Nada queda abajo de la pantalla.** El menu de ajustes cortaba "Partida"
  y "Datos", y el del lobby cortaba "Bajar acompanado". Ahora todo se
  desliza, y las pantallas respetan el notch y la barra de gestos.
- **Agacharse ya no te deja ver a traves de las paredes**, y los tramos
  bajos son mas largos y con altura de verdad: pasar por ellos dejo de ser
  cuestion de acertarle a un huequito.
- **Los bichos ya no atraviesan los techos bajos** ni vuelan metidos en la
  roca.
- **A los bichos se les puede pegar** sin tener que estar literalmente
  encima, y hay una mira que se prende cuando uno esta al alcance de verdad.
- **Las trampas se ven** antes de pisarlas, sin necesidad de ningun poder
  comprado, y las del piso se saltan.
- **La escalera parece una escalera**: tiene largueros, no travesanos
  sueltos flotando contra la pared.
- Se saco la tiza, que no funcionaba.

### Lo que hay de nuevo
- **Dos bichos mas**: el Topo de veta, que corre mas que vos y es el unico
  que entra en las gateras (meterse en un tramo bajo te saca al guardian de
  encima, pero no al topo), y la Arana de sima, que cuelga a media altura.
- **Los companeros de sala caminan de verdad**: mueven las piernas y los
  brazos cuando avanzan, y se quedan quietos cuando estan parados.
- **Mas Vetagris**: antes era uno cada cinco niveles y casi no aparecia.
  Ahora siempre hay al menos uno, y los que ya descubriste quedan marcados
  en el minimapa.
- **Cuevas menos cuadriculadas** y con mas ambiente: mas camaras y mas
  grandes, mas erosion, y mas antorchas, cristales, hongos y entibado.
- **Texturas mas finas**, con manchones de tono para que una pared larga
  deje de leerse como una lamina repetida.
- Botones del HUD repartidos en dos filas, con golpear mas grande y contra
  la esquina.

### Para instalarlo
1. Bajate el APK de aca abajo directo al celular.
2. Abrilo. Android te va a pedir permiso para instalar apps de origen
   desconocido: dale que si.
3. Listo. No hace falta desinstalar la version anterior: todas las
   publicaciones van firmadas con la misma clave.

### Datos
- Android 7.0 (API 24) o superior
- Necesita OpenGL ES 3.0
- Pesa 1.9 MB
- Firma SHA-256: `A3:72:F9:5E:9E:EC:57:46:A2:66:8C:DA:F3:1C:56:6A:FC:97:39:B0:A4:49:A5:C1:6A:FA:66:86:52:4E:31:EE`
```
