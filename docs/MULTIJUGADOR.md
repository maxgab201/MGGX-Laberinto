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
| `app/src/test/java/com/mggx/laberinto/MultijugadorTest.kt` | Los tests. Ya arman una partida de dos jugadores completa, sin red. |
| `app/src/main/java/com/mggx/laberinto/ui/screens/MultiplayerScreen.kt` | La pantalla de "próximamente" que hoy ve el jugador. |

La pieza clave es la interfaz `Transporte`, que tiene tres métodos: `enviar`,
`recibir` y `cerrar`. **Todo el resto del juego ya está escrito contra esa
interfaz.** Cuando enchufes una conexión de verdad, no hay que tocar nada más.

---

## 3. Qué falta, en orden

### Paso 1 — El relay (la parte de red)

Un *relay* es un servidor tonto: recibe un mensaje de alguien de la sala y se
lo reenvía a los demás. No entiende de juego, no valida nada, solo repite.

**Mi recomendación: Supabase Realtime.** Motivos concretos:

- El plan gratis alcanza y sobra para probar con amigos.
- Es WebSocket, que es exactamente lo que necesitás.
- No hay que programar ni desplegar un servidor: creás un proyecto y listo.
- Tiene librería de Kotlin oficial.

La alternativa, si algún día querés algo propio, es un servidor de 40 líneas en
Node con la librería `ws`, en cualquier VPS barato. Con lo que hay hecho, cambiar
de uno al otro es reescribir una sola clase.

**Qué hacer:**

1. Creá una cuenta en supabase.com y un proyecto nuevo (gratis).
2. Anotá la **URL del proyecto** y la **clave pública** (`anon key`). La clave
   pública puede ir en la app; la clave `service_role` **nunca**.
3. En `app/build.gradle.kts`, agregá la dependencia del cliente de Kotlin de
   Supabase (`supabase-kt`, módulo `realtime`).
4. Creá `app/src/main/java/com/mggx/laberinto/net/TransporteSupabase.kt` con una
   clase que implemente `Transporte`:
   - en el constructor se suscribe a un canal que se llame como el código de la
     sala (por ejemplo `sala-A7K2`);
   - `enviar(texto)` publica un evento `msg` en ese canal con el texto;
   - cada evento que llega se guarda en una cola;
   - `recibir()` vacía la cola y la devuelve;
   - `cerrar()` se desuscribe.

Ese es todo el trabajo de red. Es una clase de unas 60 líneas.

**Cómo saber que anda:** dos teléfonos en la misma sala, uno manda un `PING` y
el otro lo recibe. Con eso ya está resuelta la parte difícil.

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
canal, nada más.

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

---

## 5. Cuánto sale

Nada para empezar. El plan gratis de Supabase da 200 conexiones simultáneas y
2 millones de mensajes por mes. A 10 mensajes por segundo por jugador, eso es
más o menos 55 horas de partidas de dos. Si algún día se queda corto, ahí sí un
VPS de 5 dólares aguanta muchísimo más.

---

## 6. Por dónde arrancar mañana

Si tenés una tarde: **hacé el Paso 1 y nada más**. Cuando dos teléfonos se
manden un `PING` y se vean, el resto es juego, no es red, y es la parte
divertida.
