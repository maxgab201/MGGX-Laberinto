# MGGX Laberinto 1.0.0

**Fecha:** 2026-09-04
**Version anterior:** ninguna — es la primera version del juego.

## Que se hizo

El lanzamiento inicial del juego: un laberinto en 3D, primera persona, para
Android. Motor propio en Kotlin + OpenGL ES 3.0, sin motores de terceros y
sin un solo archivo de arte o de audio — la roca, los brazos, los iconos, la
musica y los efectos se generan todos por codigo.

Despues de la primera tanda de codigo se probo el juego de verdad en un
emulador, y esa prueba encontro una serie de bugs de puesta en marcha
(geometria dada vuelta, controles invertidos) que se arreglaron antes de
cerrar la version.

## Cambios esperados

- Generador de laberintos siempre resoluble que crece con el nivel.
- Motor 3D de cueva con texturas procedurales, normal mapping, niebla y
  antorcha con titileo.
- Brazos en primera persona modelados por codigo, con cinco dedos.
- 61 objetos de tienda con efectos unicos y dos monedas propias.
- Musica y efectos sintetizados en tiempo real con AudioTrack.
- Controles tactiles y soporte completo de mando, tambien en los menus.
- Menu de ajustes extenso, en lenguaje llano.
- 94 iconos vectoriales dibujados a mano en Compose.

## Cambios realizados

Todo lo de arriba, mas lo que salio de probarlo en un emulador de verdad:

- Cinco ajustes que se guardaban pero no hacian nada quedaron funcionando:
  cuadros por segundo objetivo, resolucion interna, modo para daltonismo,
  tamano de los textos y el cartel de primeros pasos.
- Soporte para la perdida del contexto de OpenGL (pantalla apagada, cambio
  de app): se vuelven a armar texturas y malla en vez de quedar en negro.
- Barra de carga indeterminada (antes parecia estar siempre al 100%).
- Ajustes de aspecto visual con el juego a la vista: vetas de mineral menos
  densas, brazos con mejor angulo de entrada, manos menos quemadas de luz.
- Texturas de roca con juntas entre placas, grietas finas, estratos de
  sedimento y brillo de humedad en las hondonadas.
- Script y workflows de GitHub Actions para tests y publicacion de release.

## Bugs reparados

Todos encontrados probando el juego de verdad en un emulador, antes de que
llegaran a un dispositivo:

- **Geometria dada vuelta**: toda la malla del laberinto tenia el orden de
  vertices invertido, asi que las paredes se descartaban por back-face
  culling y se veia a traves de ellas (igual pasaba con los tubos de los
  brazos, los conos y los cilindros).
- **Eje X invertido en los dos lados del control**: caminar de costado iba
  para el lado contrario, y arrastrar la camara a la derecha giraba a la
  izquierda. Los dos bugs venian de confundir el signo en las formulas de
  vector lateral y de yaw.
- **El jugador arrancaba mirando a la pared**: el mapeo de angulos no
  coincidia con la convencion de la camara.
- **Las manos quedaban fuera del campo de vision**, y el color de piel y de
  guante estaba invertido.
- **El fondo de los paneles de piedra estiraba el panel entero** y empujaba
  contenido fuera de la pantalla.
- **Un shader se rompia en tiempo de ejecucion** por un salto de linea mal
  escapado, sin dar ningun error al compilar.
- **El reinicio de nivel no reiniciaba el mismo nivel**: sin una semilla fija
  por nivel, "Reintentar" tiraba a un laberinto distinto cada vez. Ademas,
  un nivel pendiente viejo en la cola podia ganarle al nuevo y dejar la
  partida sin poder terminarse nunca.
- **La textura de roca se repetia cada 3 metros y cantaba**; se resolvio con
  una capa de modulacion de brillo a gran escala mas un grano fino de cerca.
- **`smoothstep` con los bordes al reves** (indefinido en GLSL ES): en
  algunos drivers el brillo de humedad desaparecia sin ningun error visible.

## Tests

72 tests de logica y validacion de los 10 shaders GLSL, todos en verde al
cierre de esta version.
