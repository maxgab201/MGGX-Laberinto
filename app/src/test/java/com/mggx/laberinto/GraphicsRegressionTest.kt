package com.mggx.laberinto

import com.mggx.laberinto.gl.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class GraphicsRegressionTest {
    @Test fun exportInspectableModelsWhenRequested() {
        val destination = System.getenv("MGGX_EXPORT_MODELS") ?: return
        ModelExport.export(java.io.File(destination))
    }

    private fun validate(g: PropMeshes.Geometry) {
        val v = g.vertices
        assertEquals(0, v.size % 6)
        assertEquals(0, g.indices.size % 3)
        assertTrue(v.all { it.isFinite() })
        assertTrue(g.indices.all { it >= 0 && it < v.size / 6 })
        for (i in v.indices step 6) {
            val length = sqrt(v[i+3]*v[i+3]+v[i+4]*v[i+4]+v[i+5]*v[i+5])
            assertEquals("normal no unitaria en $i", 1f, length, 0.002f)
        }
        for (i in g.indices.indices step 3) {
            val a=g.indices[i]*6; val b=g.indices[i+1]*6; val c=g.indices[i+2]*6
            val ux=v[b]-v[a]; val uy=v[b+1]-v[a+1]; val uz=v[b+2]-v[a+2]
            val vx=v[c]-v[a]; val vy=v[c+1]-v[a+1]; val vz=v[c+2]-v[a+2]
            val nx=uy*vz-uz*vy; val ny=uz*vx-ux*vz; val nz=ux*vy-uy*vx
            if (nx*nx+ny*ny+nz*nz < 1e-16f) continue
            val dot=nx*(v[a+3]+v[b+3]+v[c+3])+ny*(v[a+4]+v[b+4]+v[c+4])+nz*(v[a+5]+v[b+5]+v[c+5])
            assertTrue("cara invertida en triangulo ${i/3}", dot > 0f)
        }
    }

    @Test fun newMeshesHaveFiniteUnitNormalsAndOutwardWinding() {
        val models = listOf(DetailMeshes.roundedBox(), DetailMeshes.boulder(), DetailMeshes.gem(),
            DetailMeshes.stalagmite(), DetailMeshes.stalagmite(true), DetailMeshes.torus(0.135f,0.018f),
            PlayerMeshes.mineroCuerpo(), PlayerMeshes.mineroCasco(), EnemyMeshes.murcielagoAla(),
            StructureMeshes.estacionCarburo())
        for (g in models) validate(g)
    }

    @Test fun gemsEnclosePositiveVolumeInsteadOfFacingInwards() {
        // Comparing a derived normal with itself cannot detect an inside-out mesh.
        for (g in listOf(PropMeshes.octahedron(), DetailMeshes.gem())) {
            val v=g.vertices
            var volume = 0.0
            for (i in g.indices.indices step 3) {
                val a=g.indices[i]*6; val b=g.indices[i+1]*6; val c=g.indices[i+2]*6
                volume += v[a]*(v[b+1]*v[c+2]-v[b+2]*v[c+1]) +
                    v[a+1]*(v[b+2]*v[c]-v[b]*v[c+2]) + v[a+2]*(v[b]*v[c+1]-v[b+1]*v[c])
            }
            assertTrue("gema orientada hacia adentro",volume > 0.0)
        }
    }

    @Test fun invertedConeNormalsPointTowardItsTip() {
        val g=PropMeshes.cone(12,2f,0.13f,true)
        assertTrue(g.vertices[4] < 0f)
        validate(g)
    }

    @Test fun roundedHandsStayWithinMobileGeometryBudget() {
        val g=ArmsMesh.build()
        assertTrue(g.indices.size / 3 < 12000)
        assertTrue(g.vertices.all { it.isFinite() })
    }
}
