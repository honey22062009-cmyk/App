package com.example

import com.example.ui.components.CompanionGeometry
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class BunnyGeometryTest {
    @Test fun bunnyBodyHasFiniteUnitNormalsAndClosedSeamFromTorsoThroughHead() {
        val rows = 64; val columns = 48
        val mesh = CompanionGeometry.bunnyBody(rows, columns)
        assertEquals(rows * columns * 6 * 6, mesh.size)
        for (i in mesh.indices step 6) {
            assertTrue((0..5).all { mesh[i + it].isFinite() })
            val length = sqrt(mesh[i+3]*mesh[i+3] + mesh[i+4]*mesh[i+4] + mesh[i+5]*mesh[i+5])
            assertEquals(1f, length, .001f)
        }
        for (row in 0 until rows) {
            val first = row * columns * 36
            val end = first + (columns - 1) * 36 + 12
            for (axis in 0..2) assertEquals("Body seam at row $row", mesh[first + axis], mesh[end + axis], .00001f)
        }
        val neck = (0 until mesh.size step 6).filter { mesh[it+1] in .1f.. .2f }
        assertTrue(neck.isNotEmpty())
        assertTrue("Neck should transition smoothly into the head", neck.any { kotlin.math.abs(mesh[it]) > .25f })
    }

    @Test fun bunnyEarMeshesAreFiniteAndInsetEarIsNarrower() {
        val outer = CompanionGeometry.bunnyEar()
        val inner = CompanionGeometry.bunnyEar(inner = true)
        assertEquals(36 * 24 * 36, outer.size)
        assertEquals(outer.size, inner.size)
        for (mesh in listOf(outer, inner)) {
            for (i in mesh.indices step 6) {
                assertTrue((0..5).all { mesh[i + it].isFinite() })
                val normalLength = sqrt(mesh[i+3]*mesh[i+3] + mesh[i+4]*mesh[i+4] + mesh[i+5]*mesh[i+5])
                assertEquals(1f, normalLength, .001f)
            }
        }
        val outerWidth = (0 until outer.size step 6).maxOf { kotlin.math.abs(outer[it]) }
        val innerWidth = (0 until inner.size step 6).maxOf { kotlin.math.abs(inner[it]) }
        assertTrue("Inner-ear surface should be smaller than the outer ear", innerWidth < outerWidth)
    }

    @Test fun softLimbMeshHasFiniteVerticesAndUnitNormals() {
        val mesh = CompanionGeometry.softLimb()
        assertEquals(32 * 24 * 36, mesh.size)
        for (i in mesh.indices step 6) {
            assertTrue((0..5).all { mesh[i + it].isFinite() })
            val normalLength = sqrt(mesh[i+3]*mesh[i+3] + mesh[i+4]*mesh[i+4] + mesh[i+5]*mesh[i+5])
            assertEquals(1f, normalLength, .001f)
        }
    }
}
