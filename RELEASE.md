# Para crear la release de GitHub

Listo para copiar y pegar tal cual en "Create a new release" con el tag
`v2.0.0`, adjuntando `apks/MGGX-Laberinto-2.0.0.apk`. También la puede
publicar sola el workflow "Publicar todas las releases" (`workflow_dispatch`,
escribiendo `SI`).

---

## Titulo

```
MGGX Laberinto 2.0.0 - El Patio
```

## Descripcion

```markdown
## MGGX Laberinto 2.0.0 - El Patio

Juego de laberintos en 3D, primera persona, para Android. Nativo (Kotlin +
OpenGL ES 3.0), sin motores de terceros y sin un solo archivo de arte o de
audio: la roca, los brazos, los iconos, la musica y los efectos se generan
por codigo.

La version pasada le dio un final al juego: el nivel 999 sale a un patio al
aire libre. **Esta version es ese patio, y nada mas que ese patio.**

Hacia falta. Cuando se lo fotografio parado en la boca del tunel —el unico
lugar desde donde se lo va a ver— lo que habia era una losa gris de quince
metros con unos yuyos adelante. Le faltaban las dos cosas mas basicas de estar
afuera.

### No habia cielo
Mirar para arriba mostraba el color de borrado: un gris plano. Ahora hay un
domo con degrade de horizonte a cenit, sol con disco y halo, y nubes que se
mueven. Es media tarde a proposito: el sol bajo y calido es lo que hace que
salir de la mina se sienta un final y no una hora cualquiera.

### No habia pasto
El suelo del patio usaba la misma textura de roca que una galeria: salias a
la superficie y pisabas el mismo canto rodado gris de trescientos metros mas
abajo. Ahora hay pasto, y se genera solo en el nivel que sale afuera.

### La casa era una pared
Un panel plano repetido. Le faltaba lo unico que hace que una casa se lea como
casa de lejos: el **techo**. Ahora tiene techo a dos aguas con tejas,
chimenea, alero que vuela y las ventanas colgadas de el.

### Y el mundo se terminaba en el cerco
Del otro lado no habia nada. Ahora hay tres anillos de cerros a distintas
distancias y arboles, con perspectiva aerea. No es solo que se veia el vacio:
sin nada lejos el patio no tiene ESCALA, y el ojo no tiene con que medir cuan
grande es la casa.

### Ademas
El patio pasa de 15 a 21 metros de lado. El pasto, la copa del arbol, el
camino de losas y las tablas del cerco, rehechos uno por uno mirandolos. Y un
farol prendido al lado de la puerta, que es lo unico del patio que emite luz
y dice "te estaban esperando" sin una sola palabra.

### Como se hizo
535 tests en verde (eran 526). Para poder mirar el cielo —que se dibuja en la
GPU, donde un test no llega— se escribio un visor que rasteriza la misma
cuenta desde la JVM. Y el ultimo test que se escribio, el del presupuesto de
triangulos, encontro dos bugs que no daban ningun error: ochenta travesanos de
cerco que no se dibujaban por pasarse del cupo de instancias, y un cerco que
se comia 84.000 triangulos para redondear cantos que a esa escala no se ven.

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
