# Para crear la release de GitHub

Listo para copiar y pegar tal cual en "Create a new release" con el tag
`v1.9.2`, adjuntando `apks/MGGX-Laberinto-1.9.2.apk`. También la puede
publicar sola el workflow "Publicar todas las releases" (`workflow_dispatch`,
escribiendo `SI`).

---

## Titulo

```
MGGX Laberinto 1.9.2 - Cazando Fantasmas
```

## Descripcion

```markdown
## MGGX Laberinto 1.9.2 - Cazando Fantasmas

Juego de laberintos en 3D, primera persona, para Android. Nativo (Kotlin +
OpenGL ES 3.0), sin motores de terceros y sin un solo archivo de arte o de
audio: la roca, los brazos, los iconos, la musica y los efectos se generan
por codigo.

Esta version no trae nada nuevo para ver. Trae once cosas que estaban rotas y
ya no lo estan — casi todas de las que no se notan jugando, que son las peores:
el juego anda, nadie se queja, y mientras tanto el cronometro miente y el
aguante no se gasta.

### Arreglado
- **El cronometro corria al 96%.** Perdia 2,4 segundos por minuto a 60 cuadros,
  y el 10% a 90. O sea que el mismo recorrido daba un tiempo distinto segun a
  cuantos cuadros iba el telefono: tu record personal y el bonus por terminar
  rapido dependian del brillo de la pantalla, y en una carrera los dos
  telefonos cronometraban distinto.
- **El aguante no se gastaba nunca.** En el fondo de la barra el sprint se
  prendia y apagaba sesenta veces por segundo, asi que la barra no llegaba al
  cero y se cruzaba la cueva entera a medio trote, gratis. Ahora cuando se
  acaba, se acaba.
- **Los bichos te escuchaban correr aunque estuvieras caminando.** El sigilo
  miraba el boton, no si estabas corriendo de verdad. Con "correr siempre"
  prendido en los ajustes eras ruidoso toda la partida.
- **El Hilo de Ariadna se cortaba a los 675 metros** sin ningun aviso, justo en
  los niveles grandes que son donde se compra. Y despues de los 90 segundos que
  promete, seguia dibujado un cuarto de hora.
- **La barrita de los efectos se pasaba del 100%** y mostraba el icono del
  objeto equivocado si usabas uno corto y despues uno largo.
- **La sala quedaba abierta al cerrar la app**, y los demas te veian de
  fantasma hasta que vencia el tiempo muerto.
- **El que entraba tarde a una partida veia monedas que el otro ya se habia
  llevado** y paredes enteras donde el companiero caminaba. En cooperativo esa
  moneda se pagaba dos veces.
- **El modo de juego volvia a Carrera solo** al terminar un nivel en
  Cooperativo y volver a la sala para bajar al siguiente.
- **El hilo del sonido quedaba vivo** al apagar y volver a prender el audio.
- **Cinco asignaciones de memoria por cuadro** en la ruta de dibujo: hasta 300
  KB por segundo de memoria nativa que el recolector devuelve tarde.

### Como se hizo
467 tests en verde (eran 416). Y cada arreglo se verifico al reves: se volvio a
poner el codigo viejo y se comprobo que el test nuevo falla. Un test que pasa
igual con y sin el arreglo no esta probando nada.

### Para instalarlo
1. Bajate el APK de aca abajo directo al celular.
2. Abrilo. Android te va a pedir permiso para instalar apps de origen
   desconocido: dale que si.
3. Listo. No hace falta desinstalar la version anterior: todas las
   publicaciones van firmadas con la misma clave.

### Datos
- Android 7.0 (API 24) o superior
- Necesita OpenGL ES 3.0
- Anda sin internet. Lo unico que necesita conexion es el multijugador.
- Firma SHA-256: `A3:72:F9:5E:9E:EC:57:46:A2:66:8C:DA:F3:1C:56:6A:FC:97:39:B0:A4:49:A5:C1:6A:FA:66:86:52:4E:31:EE`
```
