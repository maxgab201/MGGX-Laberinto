# Para crear la release de GitHub

Listo para copiar y pegar tal cual en "Create a new release" con el tag
`v1.6.0`, adjuntando `apks/MGGX-Laberinto-1.6.0.apk`. También la puede
publicar sola el workflow "Publicar todas las releases" (`workflow_dispatch`,
escribiendo `SI`).

---

## Titulo

```
MGGX Laberinto 1.6.0 - Mina Abandonada
```

## Descripcion

```markdown
## MGGX Laberinto 1.6.0 - Mina Abandonada

Juego de laberintos en 3D, primera persona, para Android. Nativo (Kotlin +
OpenGL ES 3.0), sin motores de terceros y sin un solo archivo de arte o de
audio: la roca, los brazos, los iconos, la musica y los efectos se generan
por codigo.

La version mas grande hasta ahora: materiales y modelos nuevos en todo el
juego, mas una tanda de correcciones jugando en un dispositivo real.

### Graficos y materiales
- Iluminacion fisica (GGX, Fresnel Schlick, visibilidad Smith) para las
  luces de la cueva y la del jugador, con rugosidad propia para piedra,
  madera, hierro, cristal, materia organica y tela.
- Tonemapping filmico, mapas normales orientados por UV, desplazamiento de
  paralaje en calidad alta y grano de pelicula con desvanecimiento por
  distancia.
- El minero (vos y tus companeros de sala) tiene modelo nuevo: antes era
  un apilado de cajas sin brazos, ahora tiene piernas, abrigo y brazos
  colgando a los costados, con el mismo nivel de detalle que los bichos y
  las estructuras.
- Manos, alas y estructuras con mallas y normales corregidas (habia varias
  invertidas: se veian "del reves" o transparentes en ciertos angulos).

### Multijugador y estabilidad
- La sala ya no se cierra sola al terminar un nivel: si sigue conectada,
  te ofrece bajar el siguiente nivel con tu companero sin volver a
  compartir el codigo.
- En modo cooperativo, el minimapa ahora muestra la posicion de tu
  companero.
- Se arreglaron varios cuelgues y desincronizaciones de red: poses que no
  se mandaban (saltos y escaleras sin desplazamiento horizontal), el
  latido que se reiniciaba sin querer, errores de Firebase que quedaban
  en silencio, y valores invalidos que podian contaminar coordenadas.

### Interfaz
- El minimapa dejaba de dibujarse (quedaba congelado en un cuadrado negro)
  por un bug de Compose; ya esta arreglado, y de paso se le hizo un
  rediseño: rota con la camara y tiene mas contraste entre pared, piso y
  camino recorrido.
- Boton de golpear mas grande, el de usar objeto mas chico (antes era al
  reves).
- El HUD respeta los bordes seguros del telefono (notch, barra de gestos)
  en vez de paddings fijos.

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
