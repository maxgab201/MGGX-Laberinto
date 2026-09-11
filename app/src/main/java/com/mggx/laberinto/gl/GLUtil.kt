package com.mggx.laberinto.gl

import android.opengl.GLES30
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

object GLUtil {
    const val TAG = "MggxGL"

    fun compile(type: Int, src: String): Int {
        val id = GLES30.glCreateShader(type)
        GLES30.glShaderSource(id, src)
        GLES30.glCompileShader(id)
        val ok = IntArray(1)
        GLES30.glGetShaderiv(id, GLES30.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(id)
            GLES30.glDeleteShader(id)
            throw RuntimeException("Error compilando shader: $log")
        }
        return id
    }

    fun program(vs: String, fs: String): Int {
        val v = compile(GLES30.GL_VERTEX_SHADER, vs)
        val f = compile(GLES30.GL_FRAGMENT_SHADER, fs)
        val p = GLES30.glCreateProgram()
        GLES30.glAttachShader(p, v)
        GLES30.glAttachShader(p, f)
        GLES30.glLinkProgram(p)
        val ok = IntArray(1)
        GLES30.glGetProgramiv(p, GLES30.GL_LINK_STATUS, ok, 0)
        GLES30.glDeleteShader(v)
        GLES30.glDeleteShader(f)
        if (ok[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(p)
            GLES30.glDeleteProgram(p)
            throw RuntimeException("Error enlazando programa: $log")
        }
        return p
    }

    fun floatBuffer(data: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply { put(data); position(0) }

    fun intBuffer(data: IntArray): IntBuffer =
        ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder())
            .asIntBuffer().apply { put(data); position(0) }

    fun checkError(where: String) {
        val e = GLES30.glGetError()
        if (e != GLES30.GL_NO_ERROR) Log.w(TAG, "Error GL 0x${Integer.toHexString(e)} en $where")
    }
}

/** Buffer de vertices que crece solo, para armar geometria sin conocer el total. */
class FloatList(initial: Int = 4096) {
    var data = FloatArray(initial)
        private set
    var size = 0
        private set

    fun add(v: Float) {
        if (size == data.size) data = data.copyOf(data.size * 2)
        data[size++] = v
    }

    fun add(vararg vs: Float) { for (v in vs) add(v) }

    fun clear() { size = 0 }

    fun toArray(): FloatArray = data.copyOf(size)
}

class IntList(initial: Int = 4096) {
    var data = IntArray(initial)
        private set
    var size = 0
        private set

    fun add(v: Int) {
        if (size == data.size) data = data.copyOf(data.size * 2)
        data[size++] = v
    }

    fun clear() { size = 0 }
    fun toArray(): IntArray = data.copyOf(size)
}

/**
 * Cache de posiciones de uniforms.
 *
 * `glGetUniformLocation` no es gratis: cada llamada cruza a JNI y le hace al
 * driver una busqueda por NOMBRE (string) dentro de la tabla del programa. El
 * renderer hacia ~62 de esas por cuadro, o sea unas 3.700 por segundo a 60
 * fps, para preguntar 62 veces lo mismo: la posicion de un uniform no cambia
 * nunca mientras el programa siga enlazado.
 *
 * Aca se pregunta una sola vez por nombre y despues sale de un HashMap. En un
 * telefono eso no se nota tanto en cuadros por segundo como en BATERIA: es
 * trabajo de CPU que se hace en el hilo de GL, cuadro tras cuadro, sin que
 * cambie ningun resultado.
 *
 * La busqueda entra por parametro ([buscar]) en vez de llamar a GLES30 aca
 * adentro: asi esta clase es aritmetica pura y se puede verificar en un test
 * de JVM, donde las llamadas a GLES30 no hacen nada de verdad.
 *
 * IMPORTANTE: cuando se pierde el contexto de GL los programas se vuelven a
 * crear con ids nuevos, y una posicion vieja apuntaria a cualquier lado. Por
 * eso [limpiar] se llama en `onSurfaceCreated`.
 */
class UniformCache {

    private val porPrograma = HashMap<Int, HashMap<String, Int>>()

    /** Cuantas veces se fue de verdad a preguntarle al driver. Lo mira el test. */
    var consultasReales = 0
        private set

    fun loc(programa: Int, nombre: String, buscar: (Int, String) -> Int): Int {
        val mapa = porPrograma.getOrPut(programa) { HashMap() }
        val cacheado = mapa[nombre]
        if (cacheado != null) return cacheado
        consultasReales++
        val loc = buscar(programa, nombre)
        mapa[nombre] = loc
        return loc
    }

    /** Se llama al recrear el contexto de GL: los ids de programa cambiaron. */
    fun limpiar() {
        porPrograma.clear()
        consultasReales = 0
    }
}
