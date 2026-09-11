# Para crear la release de GitHub

Listo para copiar y pegar tal cual en "Create a new release" con el tag
`v1.8.0`, adjuntando `apks/MGGX-Laberinto-1.8.0.apk`. También la puede
publicar sola el workflow "Publicar todas las releases" (`workflow_dispatch`,
escribiendo `SI`).

---

## Titulo

```
MGGX Laberinto 1.8.0 - Aguas Negras
```

## Descripcion

```markdown
## MGGX Laberinto 1.8.0 - Aguas Negras

Juego de laberintos en 3D, primera persona, para Android. Nativo (Kotlin +
OpenGL ES 3.0), sin motores de terceros y sin un solo archivo de arte o de
audio: la roca, los brazos, los iconos, la musica y los efectos se generan
por codigo.

Una tanda de fondo: bugs de verdad, bateria, y una vuelta grande a los
graficos.

### Nuevo
- **Agua en la cueva.** Desde el nivel 4, y mas seguido cuanto mas hondo
  bajas. Funciona como una napa: hay una altura y se llena todo lo que quede
  por debajo, asi que los charcos caen solos en los pozos. Nunca tapa el paso
  (30 cm, menos que un escalon: se vadea caminando) y nunca moja el arranque
  ni la salida.
- **El agua refleja.** No con una segunda pasada de camara —en un telefono eso
  es duplicar el costo de dibujar la cueva— sino con lo que de verdad se
  refleja en un charco a oscuras: las luces. Tu antorcha, las de la pared y
  los cristales aparecen estirados en una columna temblorosa sobre el agua.
- **Chapotear hace ruido.** Tanto como correr. Un tramo inundado es un tramo
  donde el sigilo no te sirve, y eso cambia por donde elegis ir. El agua
  ademas frena un poco y se ve en el minimapa.

### Cambiado
- **La cueva es mas oscura.** La antorcha alumbraba como un reflector y se
  veia hasta el fondo del pasillo. Ahora hay un circulo de luz alrededor tuyo
  y despues penumbra, que es lo que hace que una antorcha de la pared o un
  cristal a lo lejos signifiquen algo.
- **La roca dejo de repetirse.** La textura se proyecta en coordenadas del
  mundo, asi que la misma baldosa de 3 m se repetia por toda la cueva y de
  lejos se leia como una lamina fotocopiada. Ahora se mezclan dos muestras de
  la misma textura, una girada, con una mascara muy estirada: el ojo deja de
  encontrar el patron.
- **Mas detalle pegado a la pared** y relieve casi el doble de hondo. En
  calidad ultra, ademas, los bultos de la roca se hacen sombra entre si.

### Arreglado
- **La pantalla se quedaba prendida para siempre.** La bandera se ponia al
  arrancar la app y no se sacaba nunca: dejabas el juego en la tienda y la
  pantalla seguia encendida hasta que se acababa la bateria.
- **El multijugador se colgaba sin internet, sin decir nada.** Firebase sin red
  no falla: encola lo que le pediste y promete mandarlo despues. La sala se
  quedaba esperando para siempre, sin cartel y sin error. Ahora se pregunta si
  hay red antes y se avisa (aclarando que el resto del juego anda igual sin
  conexion).
- **Menos bateria.** El renderer le preguntaba al driver 3.700 veces por
  segundo la posicion de unos uniforms que no cambian nunca; en pausa dibujaba
  60 veces por segundo una escena congelada; y tiraba dos objetos por cuadro al
  recolector. Las texturas, ademas, se generan ahora en cuatro hilos: el
  freezon al bajar a un bioma nuevo se redujo a menos de la mitad.

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
- Anda sin internet. Lo unico que necesita conexion es el multijugador.
- Firma SHA-256: `A3:72:F9:5E:9E:EC:57:46:A2:66:8C:DA:F3:1C:56:6A:FC:97:39:B0:A4:49:A5:C1:6A:FA:66:86:52:4E:31:EE`
```
