package com.mggx.laberinto

import com.mggx.laberinto.gl.*
import java.io.File

/** Exporta las MISMAS mallas del renderer; los OBJ sirven para inspeccion/Blender. */
object ModelExport {
    fun models(): Map<String, PropMeshes.Geometry> = linkedMapOf(
        "gema" to DetailMeshes.gem(), "roca" to DetailMeshes.boulder(),
        "estalagmita" to DetailMeshes.stalagmite(), "estalactita" to DetailMeshes.stalagmite(true),
        "caja" to DetailMeshes.roundedBox(), "tablón" to DetailMeshes.roundedBox(1f,0.13f,0.13f,0.018f),
        "antorcha" to StructureMeshes.antorcha(), "llama" to StructureMeshes.llama(),
        "cristal" to StructureMeshes.cristal(), "cofre" to StructureMeshes.cofre(),
        "hongo" to StructureMeshes.hongo(), "pincho" to StructureMeshes.pincho(),
        "estacion-carburo" to StructureMeshes.estacionCarburo(), "salida" to StructureMeshes.obeliscoSalida(),
        "murcielago-cuerpo" to EnemyMeshes.murcielagoCuerpo(), "murcielago-ala" to EnemyMeshes.murcielagoAla(),
        "rastrero-cuerpo" to EnemyMeshes.rastreroSegmento(), "rastrero-pata" to EnemyMeshes.rastreroPata(),
        "guardian-torso" to EnemyMeshes.guardianTorso(), "guardian-cabeza" to EnemyMeshes.guardianCabeza(),
        "guardian-brazo" to EnemyMeshes.guardianBrazo(), "minero-cuerpo" to PlayerMeshes.mineroCuerpo(),
        "minero-casco" to PlayerMeshes.mineroCasco()
    )

    @JvmStatic fun main(args: Array<String>) { export(File(args.firstOrNull() ?: "build/models")) }

    fun export(directory: File) {
        check(directory.isDirectory || directory.mkdirs())
        val meshes=models().toMutableMap()
        val arms=ArmsMesh.build()
        val v=FloatArray(arms.vertices.size/ArmsMesh.STRIDE_FLOATS*6)
        for (i in 0 until arms.vertices.size/ArmsMesh.STRIDE_FLOATS)
            arms.vertices.copyInto(v,i*6,i*ArmsMesh.STRIDE_FLOATS,i*ArmsMesh.STRIDE_FLOATS+6)
        meshes["brazos"]=PropMeshes.Geometry(v,arms.indices)
        for ((name,g) in meshes) {
            File(directory,"$name.obj").bufferedWriter().use { out ->
                out.appendLine("# MGGX: geometry generated from Kotlin, Y up; no baked materials.")
                out.appendLine("o $name")
                for (i in g.vertices.indices step 6)
                    out.appendLine("v ${g.vertices[i]} ${g.vertices[i+1]} ${g.vertices[i+2]}")
                for (i in g.vertices.indices step 6)
                    out.appendLine("vn ${g.vertices[i+3]} ${g.vertices[i+4]} ${g.vertices[i+5]}")
                for (i in g.indices.indices step 3) {
                    val a=g.indices[i]+1;val b=g.indices[i+1]+1;val c=g.indices[i+2]+1
                    out.appendLine("f $a//$a $b//$b $c//$c")
                }
            }
        }
        File(directory,"README.txt").writeText("${meshes.size} modelos originales exportados del motor.\n" +
            "Eje Y arriba. Escala local por pieza; el renderer aplica la escala final.\n" +
            "Los materiales procedurales viven en Shaders.kt y no estan horneados en los OBJ.\n" +
            "ModelExport.kt permite regenerarlos. No son escaneos ni modelos fotogrametricos.\n")
    }
}
