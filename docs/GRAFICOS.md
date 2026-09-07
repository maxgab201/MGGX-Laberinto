# Graficos y estabilidad — 1.6.0-alpha1

## Alcance

Evolucion del motor Kotlin/OpenGL ES 3.0 existente. Modelos procedurales
originales optimizados para instancing: roca erosionada, cristales facetados,
estalactitas por anillos, piezas biseladas, manos redondeadas, minero con
extremidades separadas, membranas reforzadas y estructuras con perfiles mas
suaves. ModelExport exporta 24 mallas OBJ del mismo codigo que dibuja el juego.

No son escaneos fotogrametricos ni una sustitucion ultrarrealista de todos los
assets. Los OBJ no incluyen los materiales GLSL, rig ni animaciones del juego.
Los objetos de inventario sin representacion fisica conservan sus iconos.

## Materiales

- GGX, Fresnel Schlick y visibilidad Smith para las luces de cueva y del jugador.
- Rugosidad diferenciada para piedra, madera, hierro, cristal, materia organica y tela.
- Tonemapping filmico en mundo y props; relieve y humedad ligados al campo de altura.
- Tangentes derivadas de UV para orientar correctamente los mapas normales.
- Desplazamiento UV de paralaje acotado en calidad alta; no es POM con ray marching.
- Grano en coordenadas del objeto, con desvanecimiento por distancia y huella del pixel.
- Calidad baja conserva iluminacion difusa y evita microdetalle y paralaje.

No incluye sombras de oclusion entre objetos, ray tracing, reflejos de entorno
ni SSAO. La iluminacion puntual no prueba visibilidad a traves de las paredes.

## Correcciones

- Caras del octaedro apuntaban hacia adentro; la prueba anterior comparaba la
  normal calculada con ella misma. Ahora se verifica el volumen firmado.
- Normales de conos invertidos y tubos conicos incorrectas.
- smoothstep con limites invertidos en guantes y marcas de suelo.
- Niebla extrapolada por emision mayor que uno.
- Normales de alas y cuerpos no acompanaban su deformacion animada.
- Texturas de manos se movian con las coordenadas de vista.
- Liberar IDs del contexto GL perdido podia borrar buffers nuevos con IDs reutilizados.
- Cambiar calidad no invalidaba las texturas del mismo bioma.
- Buffer nativo y copia del array de instancias nuevos en cada draw: ahora se reutiliza.
- Acciones de HUD/gamepad se ejecutaban desde UI mientras GL modificaba la partida:
  ahora se encolan para el hilo de juego, asociadas a su sesion.
- Saltos y escaleras sin desplazamiento horizontal no enviaban una pose nueva.
- Movimiento continuo reiniciaba el latido, impidiendo retransmitir JOIN/START.
- Un cliente nuevo no recibia la pose del jugador quieto: se refresca cada tres segundos.
- NaN/Infinity recibidos podian contaminar coordenadas.
- Errores asincronos Firebase se ignoraban; ahora se muestran en sala y partida.
- Publicacion no inyectaba Firebase: configura GOOGLE_SERVICES_JSON y exige
  configuracion valida antes de publicar. Tests permiten una build solitaria.

## Validacion

`tools/check_shaders.py` compila los diez shaders, enlaza los cinco programas y
rechaza smoothstep numericos indefinidos. Tests JVM cubren geometria, volumen,
normales, indices, limites de triangulos y regresiones de red. CI compila las
variantes Android debug/release y exporta modelos y reportes.

Queda pendiente validar en GPU Android real: aspecto final, FPS/temperatura,
recuperacion del contexto y dos telefonos con Firebase configurado. Pasar CI
no sustituye estas pruebas. La validacion local de modelos usa el compilador
Kotlin 2.0.21 y las funciones de geometria sin inicializar OpenGL.

Referencias tecnicas: https://google.github.io/filament/main/filament.html y
https://firebase.google.com/docs/database/android/read-and-write (callbacks).
