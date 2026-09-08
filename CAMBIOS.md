# MGGX Laberinto 1.4.0

**Fecha:** 2026-09-05
**Version anterior:** 1.3.2

## Que se hizo

La cueva deja de ser una fila de cajas: el tunel pasa a tener una seccion
redonda e irregular de verdad, y ademas cada bicho y cada estructura del
mundo pasa a tener un modelo 3D propio en vez de apilar las mismas
primitivas genericas (octaedros, cilindros, cajas) que usaba todo lo demas.

## Cambios esperados

Que la cueva se vea como una cueva real (con paredes curvas y sin aristas
duras cada 3 metros) y que los bichos y las estructuras se distingan entre
si por su forma, no solo por su color.

## Cambios realizados

**Cueva redonda e irregular**:
- el desplazamiento de la roca pasa a calcularse con un campo de distancia
  del mundo (cuanto falta hasta el borde mas cercano donde la superficie
  deja de continuar), en vez de apagarse en los cuatro bordes de cada cara
  por separado — asi dos casillas vecinas calculan el mismo valor en el
  borde que comparten y la roca corre larga sin grietas;
- un perfil fijo redondea la seccion del tunel: las paredes se abren hacia
  afuera a media altura y el techo sube en el medio, pasando de rectangulo a
  ovalo;
- la subdivision de cada cara ahora sube con la calidad grafica elegida en
  Ajustes, en vez de ser fija.

**Un modelo propio para cada bicho** (`EnemyMeshes`): murcielago con hocico,
orejas y alas de membrana con dedos festoneados; rastrero con placas de
caparazon achatadas, cresta y patas quebradas en dos tramos; guardian con
torso tallado, lajas en la espalda y brazos de roca. Se agregan
herramientas de modelado nuevas a `PropMeshes` (cuerpos de revolucion,
extrusion de contornos con espesor) para poder armarlos por piezas.

**Un modelo propio para cada estructura** (`StructureMeshes`): antorcha con
escarpia, mastil y cuenco (ahora gira hacia la pared que la sostiene, con el
brazo llegando hasta la roca en vez de quedar flotando); cofre con tapa
curva y flejes; cristal como prisma inclinado; hongo de una pieza; pincho
con rebaba; estacion de carburo con bidon y volante; la salida como
obelisco facetado.

## Bugs reparados

- La antorcha quedaba flotando en el aire en vez de apoyarse contra la
  pared.
- La llama (antes un octaedro) se hundia parcialmente en el poste; la nueva
  llama en forma de gota tiene la base en cero y queda apoyada en el cuenco.
- Se sacan `shapeSpike` y `shapeConeDown`, dos buffers de GPU reservados que
  habian quedado sin ningun uso (una fuga de memoria de video potencial si
  se hubiera seguido acumulando codigo muerto de este tipo).

## Tests

Tests nuevos de continuidad de la cueva (simetria de los predicados que
garantizan que no puede abrirse una grieta entre casillas vecinas, y que el
piso no da ningun salto al cruzar de casilla) y de modelado (orientacion de
caras de cada figura y de cada herramienta nueva, cobertura exacta del
triangulador, y rechazo de los factores de escala que darian vuelta una
figura). El aspecto final de los modelos quedo pendiente de confirmar en un
dispositivo real.
