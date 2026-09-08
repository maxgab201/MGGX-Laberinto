# Cómo armar el multijugador de MGGX Laberinto

El modo de a varios **está terminado y probado**. Esta guía cuenta cómo
funciona, qué hace falta de tu lado para que ande (crear una cuenta de
Firebase, que son cinco minutos en la web) y qué limitaciones conocidas tiene.

---

## 1. La idea, en dos renglones

El laberinto **no viaja por internet**. Con el número de nivel y una semilla,
los dos teléfonos generan exactamente la misma cueva: los mismos pasillos, las
mismas monedas, las mismas trampas, el mismo relieve y los mismos bichos.

Por la red va solo lo poquito que cambia:

- **dónde está cada uno** (unas 10 veces por segundo), y
- **los hechos que tocan el mundo**: agarré esta moneda, rompí esta pared,
  pisé esta trampa, llegué a la salida, me caí.

Cada mensaje son menos de 100 caracteres. Eso significa que anda con señal fea
y que un servidor gratis te aguanta muchísimas partidas.

---

## 2. Lo que ya está en el repositorio

| Archivo | Qué es |
|---|---|
| `app/src/main/java/com/mggx/laberinto/net/NetProtocol.kt` | Los mensajes: qué tipos hay, cómo se escriben y cómo se leen. |
| `app/src/main/java/com/mggx/laberinto/net/MatchState.kt` | El estado de la sala: quién está, dónde está, qué se agarró. Y `TransporteLocal`, un transporte de mentira en memoria. |
| `app/src/main/java/com/mggx/laberinto/net/RelayFirebase.kt` | Las cuentas del relay que no dependen del SDK de Firebase (path de la sala, saneo del código, cada cuánto podar). Probado en JVM. |
| `app/src/main/java/com/mggx/laberinto/net/TransporteFirebase.kt` | **El relay de verdad**, sobre Firebase Realtime Database. Ya implementa `Transporte`: solo falta que crees tu proyecto de Firebase (paso 1 de abajo). |
| `app/src/test/java/com/mggx/laberinto/MultijugadorTest.kt` | Los tests. Ya arman una partida de dos jugadores completa, sin red. |
| `app/src/test/java/com/mggx/laberinto/RelayFirebaseTest.kt` | Tests del path de sala, el saneo de código y la política de poda. |
| `app/src/main/java/com/mggx/laberinto/net/MatchLink.kt` | El puente entre la partida y la red: manda tu posición, avisa los hechos del mundo y aplica lo que llega. |
| `app/src/main/java/com/mggx/laberinto/net/CodigoSala.kt` | El código de 4 letras para invitar, y el id de cada jugador. |
| `app/src/main/java/com/mggx/laberinto/gl/PlayerMeshes.kt` | El cuerpo y el casco con que se dibuja al compañero. |
| `app/src/test/java/com/mggx/laberinto/PartidaEnRedTest.kt` | Dos partidas de verdad conectadas entre sí, jugando. |
| `tools/probar_relay.py` | Prueba contra tu Firebase de verdad: simula dos jugadores y verifica que los mensajes viajen. Correlo con `python3 tools/probar_relay.py`. |
| `app/src/main/java/com/mggx/laberinto/ui/screens/MultiplayerScreen.kt` | La sala: crear una, entrar con un código, ver quién está y arrancar. |

La pieza clave es la interfaz `Transporte`, que tiene tres métodos: `enviar`,
`recibir` y `cerrar`. **Todo el resto del juego ya está escrito contra esa
interfaz.** `TransporteFirebase` ya la implementa: no hay que tocar nada más
del juego para que la conexión de verdad funcione.

---

## 3. Cómo está armado, paso por paso

### Paso 1 — El relay (la parte de red) — YA ESTÁ ESCRITO, falta tu cuenta

Un *relay* es un servidor tonto: recibe un mensaje de alguien de la sala y se
lo reenvía a los demás. No entiende de juego, no valida nada, solo repite.

**Se usa Firebase Realtime Database**, no Supabase. El motivo del cambio: el
plan gratis de Supabase permite solo 2 proyectos por organización, y es común
quedarse sin cupo si ya usás Supabase para otra cosa. Firebase (que es de
Google) no tiene ese límite de cantidad de proyectos: podés crear todos los
que necesites en tu cuenta, gratis, sin tarjeta.

Comparación con lo que ofrecía Supabase, para que quede claro que no se pierde
nada al cambiar:

| | Firebase Realtime DB (Spark, gratis) | Supabase Realtime (gratis) |
|---|---|---|
| Conexiones simultáneas | 100 | 200 |
| Tráfico | 10 GB/mes de descarga | 2 millones de mensajes/mes |
| Límite de proyectos por cuenta | Sin límite práctico | 2 por organización |
| Tarjeta de crédito | No hace falta | No hace falta |
| SDK oficial de Kotlin/Android | Sí | Sí |

Con 100 conexiones simultáneas y jugando en salas de 2 a 4 amigos, alcanza y
sobra. Si algún día se queda corto, la alternativa de siempre sigue siendo un
servidor propio de 40 líneas en Node con `ws`, en cualquier VPS barato: cambiar
de uno al otro es reescribir una sola clase, porque todo el juego habla contra
la interfaz `Transporte`.

**El código ya está escrito** (`RelayFirebase.kt` + `TransporteFirebase.kt`):
publica cada mensaje con `push()` en `salas/CODIGO/msgs` y escucha
`onChildAdded` para recibir los de los demás. También se poda solo: cada 40
mensajes enviados borra lo que tenga más de 30 segundos, así la sala no crece
para siempre y no hace falta una Cloud Function (esas piden tarjeta, aunque
después no se cobre nada).

La división del trabajo es simple: todo lo que es clickear en la web de
Firebase lo hacés vos (no toca ningún archivo del repositorio); todo lo que es
tocar código o el repo lo hace quien te está ayudando con el proyecto (Claude
Code), una vez que le pases lo que Firebase te dio.

#### Lo que hacés vos, en la web (unos 5 minutos, cero archivos)

1. Andá a [console.firebase.google.com](https://console.firebase.google.com)
   con tu cuenta de Google y hacé clic en **Crear un proyecto**. Ponele
   cualquier nombre (por ejemplo "mggx-laberinto"). No hace falta activar
   Google Analytics, podés dejarlo destildado.
2. Adentro del proyecto, en el menú de la izquierda, andá a **Compilación →
   Realtime Database** y hacé clic en **Crear base de datos**. Elegí la
   ubicación que te quede más cerca y arrancá en **modo de prueba** (test
   mode): eso te da reglas de lectura/escritura abiertas por 30 días, que
   después se reemplazan por las de abajo.
3. Andá a la pestaña **Reglas** de la Realtime Database y pegá esto
   (reemplazá lo que haya), después guardá con **Publicar**:
   ```json
   {
     "rules": {
       "salas": {
         "$sala": {
           "msgs": {
             ".read": true,
             ".write": true,
             ".indexOn": ["t"]
           }
         }
       }
     }
   }
   ```
   Esto deja que cualquiera lea y escriba mensajes de cualquier sala, sin
   pedir login. Para jugar con amigos está bien (es la misma idea que la
   `anon key` pública de Supabase: no es secreta, solo abre la puerta a
   mandar mensajes de juego, nada de datos sensibles).
4. En la página principal del proyecto, el ícono de engranaje →
   **Configuración del proyecto**, sección "Tus apps": hacé clic en el ícono
   de Android para agregar una app nueva.
   - **Nombre del paquete de Android**: `com.mggx.laberinto` (tiene que ser
     EXACTO, letra por letra).
   - Los demás campos (apodo, SHA-1) son opcionales, dejalos vacíos.
5. Firebase te va a ofrecer descargar un archivo llamado
   **`google-services.json`**. Descargalo. **Ese es el único archivo que hace
   falta pasar** — pegá su contenido en el chat, o subilo, y de ahí en más lo
   conecta y lo prueba quien te está ayudando con el código.

**Un detalle de seguridad, para que sepas qué estás compartiendo:**
`google-services.json` no lleva ninguna clave secreta (es información pública
que igual viaja adentro de cualquier APK), así que no hay drama en pasarlo así
nomás.

#### Lo que se hace del lado del código, con lo que pasaste

1. El archivo se guarda en `app/google-services.json` (ya está en
   `.gitignore`: no se sube al repositorio, queda solo en este entorno).
2. Se compila (`./gradlew assembleDebug`): el `build.gradle.kts` ya detecta
   el archivo solo y activa el plugin de Google.
3. Se prueba que ande de verdad con `python3 tools/probar_relay.py`, que
   simula dos jugadores en la misma sala hablando el protocolo real: uno
   manda, el otro tiene que recibir. Si algo falla (reglas mal pegadas, falta
   el índice, paquete mal escrito), el script dice exactamente qué corregir
   en la consola de Firebase. Con eso ya está resuelta la parte difícil.

> **Si reusaste un proyecto de Firebase que ya tenías para otra cosa**, ojo con
> un detalle: las reglas del punto 3 reemplazan a las que hubiera antes en la
> Realtime Database, y solo dejan pasar `salas/*/msgs`. Si la otra app usaba
> Realtime Database, se quedó sin permisos. (Si usaba Firestore o Storage no
> pasa nada: son bases distintas, con reglas propias.) Para que convivan, hay
> que agregar a las reglas el bloque de la otra app junto al de `salas`.

### Paso 2 — La pantalla de sala — HECHO

`MultiplayerScreen.kt` ya es la sala de verdad. Tiene:

- un campo para el **nombre** (se guarda en el perfil);
- un botón **Crear sala**, que inventa un código de 4 letras y lo muestra
  grande para dictárselo a un amigo;
- un campo y un botón **Entrar a una sala** con el código;
- la lista de quién está adentro, que se actualiza sola;
- el selector de **modo** (Carrera o Cooperativo), que solo toca el anfitrión;
- un botón **Empezar**, también solo del anfitrión, que manda
  `NetProtocol.arranque(...)` con el modo, el nivel y una semilla al azar.

El código de 4 letras no usa I, O, 0 ni 1: son las que se confunden al
dictarlas en voz alta, que es exactamente como se pasa un código de sala.

El código de sala no hace falta guardarlo en ningún lado: es el nombre del
canal de Firebase (`salas/CODIGO/msgs`), nada más.

### Paso 3 — Conectar la partida — HECHO

Lo hace `MatchLink`, que es el puente entre la partida y la red.
`GameSession` tiene un campo `red` que en una partida solitaria queda en null
(y entonces nada de esto corre).

Mientras se juega, solo:

- **cada 100 ms** manda tu posición, tu ángulo y tu postura, y solo si algo
  cambió: quieto mirando el mapa no gasta mensajes;
- avisa cada hecho del mundo en el momento en que pasa (agarré esta moneda,
  rompí esta pared, pisé esta trampa, llegué, me caí);
- **cada 3 segundos** manda un latido, que además repite quién sos, para el
  que entró después y no vio tu presentación.

Al revés, todo lo que llega se aplica solo: la moneda que levantó el otro
desaparece de tu cueva, la pared que rompió se te abre, y en cooperativo la
plata que junta uno la cobran los dos.

### Paso 4 — Dibujar al otro — HECHO

El compañero se dibuja con cuerpo, casco y la lucecita del casco (que además
dice para dónde está mirando), con el color del traje de su skin. Si está
agachado se lo ve más bajo, y si está caído queda tirado y apagado.

Las poses llegan 10 veces por segundo y la pantalla dibuja 60, así que no se
lo pone en la posición que llegó: la posición dibujada va corriendo atrás de
la real, y el ojo lee eso como caminar.

### Paso 5 — Las reglas de cada modo — HECHO

- **Carrera**: el primero que sale gana y a los demás se les termina la
  partida ahí mismo. Los ecos se los lleva cada uno.
- **Cooperativo**: el que se queda sin vida no pierde: queda tirado, sigue
  viendo y puede seguir mirando alrededor para guiar al otro. Un compañero que
  se le acerque a menos de 1,5 m lo levanta con media barra de vida, pero
  recién después de que pase unos segundos en el piso (si no, dos que van
  pegados no perderían nunca). Al caído no se le puede pegar más ni se cura
  solo. Si caen todos los que están en la cueva, ahí sí perdieron todos.

---

## 4. Cosas que te van a morder

- **Los mensajes se pierden.** Por eso los hechos del mundo son "conjuntos":
  agarrar dos veces la misma moneda no rompe nada, porque es un `HashSet`. Nunca
  mandes cosas del tipo "sumale 3 ecos"; mandá "la moneda 412 ya no está".
- **La gente cierra la app de golpe.** Para eso está `MatchState.envejecer(dt)`:
  llamalo una vez por frame y el que dejó de dar señales de vida a los 12
  segundos se va solo de la lista.
- **Los nombres los escribe cualquiera.** Ya pasan por `NetProtocol.limpiar()`,
  que les saca las barras verticales y los recorta. No lo saques.
- **No confíes en el otro teléfono.** Con amigos no pasa nada, pero si algún día
  querés partidas públicas, el que manda "llegué a la salida en 3 segundos"
  puede estar mintiendo. La solución es que el anfitrión valide, y eso es
  bastante más laburo. Para jugar con conocidos, así está bien.
- **Los bichos los mueve el anfitrión.** El que armó la sala corre la cabeza de
  los bichos y reparte dónde está cada uno unas siete veces por segundo; los
  demás los copian. Van sólo los que están cerca de alguien (26 m): los del
  otro extremo de la cueva no se ven, y mandarlos sería pagar mensajes por
  cuerpos que nadie mira. Los que quedan afuera se siguen moviendo por su
  cuenta en cada teléfono y se acomodan solos cuando alguien se les acerca.
- **La vida de los bichos no viaja.** El daño lo resuelve cada teléfono contra
  su propio jugador, así que pegar y recibir no dependen de que llegue un
  mensaje. La contra: si vos volteás un bicho, el anfitrión lo sigue teniendo
  en pie hasta que se entera por su lado; al revés se ve al toque, porque el
  que copia le cree.
- **Si el anfitrión se va, los bichos se quedan sin quien los mueva.** Todavía
  no hay un reemplazo automático: los que quedan los ven quietos hasta que
  vuelvan a la sala. Es la limitación más grande que queda del modo de a
  varios.
- **Las reglas de Firebase quedan abiertas.** Como no hay login, cualquiera que
  se entere del código de sala puede mandar mensajes ahí. Para jugar con
  amigos no es problema; si algún día abrís partidas públicas, ahí sí conviene
  sumar autenticación anónima de Firebase y validar del lado del anfitrión.

---

## 5. Cuánto sale

Nada para empezar. El plan gratis de Firebase (Spark) da 100 conexiones
simultáneas y 10 GB de descarga por mes en Realtime Database. A 10 mensajes
por segundo por jugador, con mensajes de menos de 100 caracteres, eso da para
muchísimas horas de partidas de a varios sin gastar un peso. Si algún día se
queda corto, ahí sí un VPS de 5 dólares aguanta muchísimo más.

---

## 6. Por dónde arrancar mañana

Si tenés una tarde: **hacé el Paso 1 y nada más**. De tu lado son 5 minutos en
la web de Firebase (crear el proyecto, activar Realtime Database, pegar las
reglas, bajar el `google-services.json`); el resto — conectarlo al código y
probar que ande — no hace falta que lo toques vos. Cuando el `PING` de una
instancia aparezca en la otra, el resto es juego, no es red, y es la parte
divertida.

