package com.mggx.laberinto.net

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * El relay de verdad, sobre Firebase Realtime Database.
 *
 * Es la pieza que faltaba en `docs/MULTIJUGADOR.md`: un servidor tonto que
 * recibe un mensaje de alguien de la sala y se lo reenvia a los demas, sin
 * entender nada de juego. Ver ese documento para el paso a paso de como
 * crear el proyecto de Firebase (es gratis y no hace falta programar ni
 * desplegar nada del lado del servidor).
 *
 * Como funciona: cada sala es un nodo de la base
 * (`salas/CODIGO/msgs`, ver [RelayFirebase.pathDeSala]) donde todos publican
 * con `push()` (que le pone un id que ordena por tiempo solo) y todos
 * escuchan `onChildAdded`. No hace falta autenticacion para jugar con
 * amigos: alcanza con reglas de Realtime Database que dejen leer y escribir
 * ese nodo (ver el JSON de reglas en el documento).
 *
 * Los mensajes no se guardan para siempre: el propio cliente los poda cada
 * tantos envios (ver [RelayFirebase.tocaPodar]), asi la sala no crece sin
 * limite y no hace falta una Cloud Function (esas piden tarjeta, aunque
 * despues no se cobre nada).
 */
class TransporteFirebase(codigoSala: String) : Transporte {

    private val raiz = FirebaseDatabase.getInstance()
        .getReference(RelayFirebase.pathDeSala(codigoSala))

    @Volatile override var errorActual: String? = null
        private set

    private fun fallo(mensaje: String) {
        if (abierto) errorActual = mensaje
    }

    private val cola = ConcurrentLinkedQueue<String>()
    private val enviosHechos = AtomicLong(0)
    @Volatile private var abierto = true

    private val listener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            if (!abierto) return
            val texto = snapshot.child("m").getValue(String::class.java) ?: return
            cola.add(texto)
        }
        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) = Unit
        override fun onChildRemoved(snapshot: DataSnapshot) = Unit
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit
        override fun onCancelled(error: DatabaseError) { fallo("Firebase: ${error.message}") }
    }

    /**
     * La consulta filtrada, si hizo falta una: hay que soltarla al cerrar.
     *
     * Va declarada ANTES del init a proposito: en Kotlin las propiedades se
     * inicializan en el orden en que estan escritas, asi que si estuviera
     * abajo su "= null" correria DESPUES del init y borraria la consulta que
     * el init acaba de guardar, dejando el listener colgado para siempre.
     */
    @Volatile private var consulta: com.google.firebase.database.Query? = null

    init {
        // Al suscribirse, Firebase entrega PRIMERO todo lo que ya estaba en el
        // nodo, y recien despues lo que va llegando. Eso es justo lo que no se
        // quiere: si alguien uso esta misma sala hace un rato, entrarian los
        // mensajes de esa partida vieja, y un "arranque" viejo te tira solo a
        // una cueva que ya termino.
        //
        // La solucion no usa relojes (el del telefono y el del servidor nunca
        // coinciden del todo): los ids que genera push() se ordenan solos por
        // tiempo, asi que alcanza con mirar cual es el ultimo que ya estaba y
        // pedir de ahi en adelante.
        raiz.orderByKey().limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!abierto) return
                    val ultimoViejo = snapshot.children.lastOrNull()?.key
                    if (ultimoViejo == null) {
                        // Sala limpia: no hay nada viejo de lo que cuidarse.
                        raiz.addChildEventListener(listener)
                    } else {
                        consulta = raiz.orderByKey().startAfter(ultimoViejo)
                        consulta?.addChildEventListener(listener)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    fallo("No se pudo leer la sala: ${error.message}")
                }
            })
    }

    override fun enviar(texto: String) {
        if (!abierto) return
        raiz.push().setValue(mapOf("m" to texto, "t" to ServerValue.TIMESTAMP))
            .addOnFailureListener { fallo("No se pudo enviar: ${it.message}") }
        if (RelayFirebase.tocaPodar(enviosHechos.incrementAndGet())) podarViejos()
    }

    /**
     * Barre los mensajes de mas de [RelayFirebase.VIDA_MENSAJE_MS]. Usa el
     * reloj del telefono, no el del servidor: para un margen de 30 segundos
     * el desfasaje de un reloj de celular normal no importa, y de ultima
     * podar un mensaje un toque tarde no rompe nada (el protocolo ya esta
     * pensado para bancarse mensajes de mas, ver seccion 4 del documento).
     */
    private fun podarViejos() {
        val limite = System.currentTimeMillis() - RelayFirebase.VIDA_MENSAJE_MS
        raiz.orderByChild("t").endAt(limite.toDouble()).limitToFirst(100)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (hijo in snapshot.children) hijo.ref.removeValue()
                }
                override fun onCancelled(error: DatabaseError) { fallo("Firebase: ${error.message}") }
            })
    }

    override fun recibir(): List<String> {
        if (cola.isEmpty()) return emptyList()
        val out = ArrayList<String>(cola.size)
        while (true) out.add(cola.poll() ?: break)
        return out
    }

    override fun cerrar() {
        abierto = false
        // Se saca de las dos: segun como haya arrancado (sala limpia o con
        // mensajes viejos), el listener quedo colgado del nodo o de la
        // consulta filtrada.
        raiz.removeEventListener(listener)
        consulta?.removeEventListener(listener)
        consulta = null
        cola.clear()
    }
}

