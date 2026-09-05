# Cómo armar el multijugador de MGGX Laberinto

Esta es la guía para cuando quieras terminar el modo de a varios. Está escrita
para que la puedas seguir vos, sin dar por sentado que sabés de redes. Lo que
ya está hecho está hecho y probado; lo que falta está en el orden en que
conviene hacerlo.

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
| `app/src/main/java/com/mggx/laberinto/ui/screens/MultiplayerScreen.kt` | La pantalla de "próximamente" que hoy ve el jugador. |

La pieza clave es la interfaz `Transporte`, que tiene tres métodos: `enviar`,
`recibir` y `cerrar`. **Todo el resto del juego ya está escrito contra esa
interfaz.** `TransporteFirebase` ya la implementa: no hay que tocar nada más
del juego para que la conexión de verdad funcione.

---

## 3. Qué falta, en orden

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

**Lo único que falta hacer vos, a mano, es crear tu proyecto de Firebase:**

1. Andá a [console.firebase.google.com](https://console.firebase.google.com)
   con tu cuenta de Google y hacé clic en **Crear un proyecto**. Ponele
   cualquier nombre (por ejemplo "mggx-laberinto"). No hace falta activar
   Google Analytics, podés dejarlo destildado.
2. Adentro del proyecto, en el menú de la izquierda, andá a **Compilación →
   Realtime Database** y hacé clic en **Crear base de datos**. Elegí la
   ubicación que te quede más cerca y arrancá en **modo de prueba** (test
   mode): eso te da reglas de lectura/escritura abiertas por 30 días, que
   después vas a reemplazar por las de abajo.
3. Andá a la pestaña **Reglas** de la Realtime Database y pegá esto
   (reemplazá lo que haya):
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
   mandar mensajes de juego, nada de datos sensibles). Guardá con
   **Publicar**.
4. Ahora conectá tu app: en la página principal del proyecto (el ícono de
   engranaje → **Configuración del proyecto**), en la sección "Tus apps",
   hacé clic en el ícono de Android para agregar una app nueva:
   - **Nombre del paquete de Android**: `com.mggx.laberinto` (tiene que ser
     EXACTO, es el `applicationId` de `app/build.gradle.kts`).
   - Los demás campos (apodo, SHA-1) son opcionales, los podés dejar vacíos.
5. Firebase te va a ofrecer descargar un archivo llamado
   **`google-services.json`**. Descargalo y ponelo en la carpeta `app/` del
   repositorio (al lado de `build.gradle.kts`, **no** adentro de `src/`).
6. Compilá de nuevo (`./gradlew assembleDebug` o desde Android Studio). El
   `build.gradle.kts` ya está preparado para detectar el archivo solo: en
   cuanto `app/google-services.json` existe, el plugin de Google se activa
   solo y la app queda conectada a tu proyecto.

**Un detalle de seguridad:** `google-services.json` no lleva ninguna clave
secreta (es información pública que igual viaja adentro de cualquier APK), así
que no pasa nada grave si lo subís al repositorio. Si preferís no subirlo de
todos modos, agregalo a `.gitignore`: el build sigue andando igual para
cualquiera que clone el repo, nomás que sin multijugador hasta que ponga el
suyo.

**Cómo saber que anda:** dos teléfonos en la misma sala (mismo código), creá
`TransporteFirebase(codigo)` en los dos, uno manda un `PING` con
`enviar(NetProtocol.ping(miId).codificar())` y el otro lo tiene que ver
aparecer en `recibir()`. Con eso ya está resuelta la parte difícil.

### Paso 2 — La pantalla de sala

Reemplazá el contenido de `MultiplayerScreen.kt` por:

- un campo para el **nombre**;
- un botón **Crear sala**, que inventa un código de 4 letras y lo muestra
  grande para dictárselo a un amigo;
- un campo y un botón **Entrar a una sala** con el código;
- la lista de quién está adentro (sale de `MatchState.todos()`);
- el selector de **modo** (Carrera o Cooperativo), que solo toca el anfitrión;
- un botón **Empezar**, también solo del anfitrión, que manda
  `NetProtocol.arranque(...)` con el modo, el nivel y una semilla al azar.

El código de sala no hace falta guardarlo en ningún lado: es el nombre del
canal de Firebase (`salas/CODIGO/msgs`), nada más.

### Paso 3 — Conectar la partida

En `MggxApp.kt`, cuando llegue el mensaje `ARRANQUE`:

```kotlin
val s = GameSession(save, match.nivel, match.semilla)
```

Y nada más. El mismo nivel y la misma semilla en los dos teléfonos dan la misma
cueva: eso ya está probado en los tests.

Después, mientras se juega:

- **cada 100 ms**, mandá `NetProtocol.pose(...)` con tu posición, tu ángulo y tu
  postura;
- cada vez que agarres algo, mandá `tomar(indice)`; cuando rompas una pared,
  `romper(indice)`; cuando pises una trampa, `trampa(indice)`;
- cuando llegues a la salida, `llegada(tiempo)`.

Y al revés: aplicá todo lo que llegue con `match.aplicar(texto)`, y antes de
dibujar un objeto fijate si su casilla está en `match.objetosTomados`.

### Paso 4 — Dibujar al otro

En `CaveRenderer.drawProps` hay un bloque que dibuja los bichos armándolos con
cuerpos, cabezas y ojos. Copiá esa idea para los compañeros: cuerpo, casco y
una lucecita de antorcha, con los colores de su skin (`JugadorRemoto.skin`).

Para que no se vea a los saltos, no lo pongas en la posición que llegó: movelo
hacia ella suavizando, algo así como

```kotlin
p.x += (objetivoX - p.x) * (12f * dt).coerceAtMost(1f)
```

Eso solo ya alcanza para que se vea natural aunque lleguen 10 mensajes por
segundo.

### Paso 5 — Las reglas de cada modo

- **Carrera**: cuando alguien manda `LLEGADA`, la partida termina para todos y
  gana el del menor tiempo (`match.ganador()`). Los ecos se los lleva cada uno.
- **Cooperativo**: cuando alguien queda en `CAIDO`, sigue viendo pero no se
  mueve; otro que se le acerque a menos de 1,5 m manda `revivir(suId)` y lo
  levanta. Si `match.equipoCaido()` da verdadero, perdieron todos.

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

Si tenés una tarde: **hacé el Paso 1 y nada más** (que hoy es solo crear la
cuenta de Firebase y bajar el `google-services.json`, el código ya está
escrito). Cuando dos teléfonos se manden un `PING` y se vean, el resto es
juego, no es red, y es la parte divertida.
