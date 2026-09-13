# Para crear la release de GitHub

Listo para copiar y pegar tal cual en "Create a new release" con el tag
`v1.9.3`, adjuntando `apks/MGGX-Laberinto-1.9.3.apk`. También la puede
publicar sola el workflow "Publicar todas las releases" (`workflow_dispatch`,
escribiendo `SI`).

---

## Titulo

```
MGGX Laberinto 1.9.3 - Apoyado en la Roca
```

## Descripcion

```markdown
## MGGX Laberinto 1.9.3 - Apoyado en la Roca

Juego de laberintos en 3D, primera persona, para Android. Nativo (Kotlin +
OpenGL ES 3.0), sin motores de terceros y sin un solo archivo de arte o de
audio: la roca, los brazos, los iconos, la musica y los efectos se generan
por codigo.

Todo lo de esta version es el MISMO bug repetido en cinco lugares: **las cosas
no estaban donde se ve que esta la roca.**

La cueva no es un pasillo de cajas. Cada cara se subdivide y se corre con ruido,
y encima lleva un perfil que le da panza a las paredes y arco al techo. Pero
todo lo que se apoyaba en esa roca se ubicaba contra el borde TEORICO de la
casilla — un plano que no existe en ninguna parte de la pantalla. El nivel
entero quedaba levemente despegado de si mismo.

### Arreglado
- **Antorchas flotando a medio metro de la pared.** Medida contra la malla que
  se dibuja, la peor quedaba a 31 cm de la roca, colgada del aire. Ahora se
  apoyan en la pared de verdad.
- **Antorchas metidas en el techo.** Clavadas siempre a 1,75 m del piso, en una
  gatera de un metro quedaban del otro lado de la roca: se veia la punta de la
  llama saliendo del techo y la luz alumbrando desde adentro de la pared.
- **Todo lo del piso, flotando o medio enterrado.** La roca del piso se abolla
  hasta 13 cm, y el maximo cae justo en el centro de la casilla, que es donde se
  apoya cada cosa. Monedas colgadas de un palmo de aire, hongos saliendo de la
  nada, piedras a medio hundir.
- **El pico parecia un mastil.** Un palo vertical en medio del cuadro, con la
  cabeza rozando el borde de arriba. Ahora el arma se ladea, cruza el cuadro en
  diagonal, deja la mira limpia y se ve agarrada.
- **El objeto de la mano izquierda** estaba escondido atras del puno.
- **Una antorcha cuya pared rompiste con el pico** seguia dibujada colgada de la
  nada.

### Nuevo
- **Sombras de contacto.** Una mancha oscura y blanda abajo de cada cosa: es lo
  que hace que algo se vea APOYADO y no pegado con cinta. La del bicho ademas
  dice a que altura esta — un murcielago volando alto deja una mancha ancha y
  palida, y al bajar a morderte se le achica y se le oscurece.

### Como se hizo
482 tests en verde (eran 467). Y se encontro que dos tests viejos daban verde
sobre codigo roto, los dos por el mismo motivo: median contra el plano teorico
en vez de contra la roca que se dibuja.

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
