package com.example

import com.example.ui.components.CompanionGeometry
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
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
        assertTrue("Neck never collapses into disconnected parts", neck.any { abs(mesh[it]) > .25f })
    }
}
