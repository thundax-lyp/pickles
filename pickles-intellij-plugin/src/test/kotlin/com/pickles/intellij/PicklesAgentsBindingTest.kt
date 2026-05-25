package com.pickles.intellij

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PicklesAgentsBindingTest {
    @Test
    fun bindCreatesManagedBlockWhenFileDoesNotExist() {
        val updated = PicklesAgentsBinding.bind(null)

        assertEquals(PicklesAgentsBinding.managedBlock + "\n", updated)
        assertTrue(PicklesAgentsBinding.isBound(updated))
    }

    @Test
    fun bindAppendsManagedBlockWhenMarkersAreMissing() {
        val updated = PicklesAgentsBinding.bind("# Project Agents\n")

        assertEquals("# Project Agents\n\n" + PicklesAgentsBinding.managedBlock + "\n", updated)
    }

    @Test
    fun bindReplacesExistingManagedBlock() {
        val updated = PicklesAgentsBinding.bind(
            """
            # Project Agents

            ${PicklesAgentsBinding.BEGIN_MARKER}
            old content
            ${PicklesAgentsBinding.END_MARKER}
            """.trimIndent() + "\n",
        )

        assertEquals("# Project Agents\n\n" + PicklesAgentsBinding.managedBlock + "\n", updated)
    }

    @Test
    fun unbindRemovesOnlyManagedBlock() {
        val updated = PicklesAgentsBinding.unbind(
            "# Project Agents\n\n" +
                PicklesAgentsBinding.managedBlock +
                "\n\nHuman notes.\n",
        )

        assertEquals("# Project Agents\n\nHuman notes.\n", updated)
    }

    @Test
    fun unbindWithoutMarkersKeepsContentUnchanged() {
        val content = "# Project Agents\n"

        assertEquals(content, PicklesAgentsBinding.unbind(content))
    }

    @Test
    fun missingEndMarkerIsConflict() {
        val error = assertThrows(PicklesAgentsBindingConflict::class.java) {
            PicklesAgentsBinding.bind("${PicklesAgentsBinding.BEGIN_MARKER}\n")
        }

        assertTrue(error.message!!.contains("exactly one BEGIN marker and one END marker"))
    }

    @Test
    fun multipleBlocksAreConflict() {
        val error = assertThrows(PicklesAgentsBindingConflict::class.java) {
            PicklesAgentsBinding.bind(
                """
                ${PicklesAgentsBinding.managedBlock}

                ${PicklesAgentsBinding.managedBlock}
                """.trimIndent(),
            )
        }

        assertTrue(error.message!!.contains("exactly one BEGIN marker and one END marker"))
    }

    @Test
    fun endBeforeBeginIsConflict() {
        val error = assertThrows(PicklesAgentsBindingConflict::class.java) {
            PicklesAgentsBinding.bind(
                """
                ${PicklesAgentsBinding.END_MARKER}
                ${PicklesAgentsBinding.BEGIN_MARKER}
                """.trimIndent(),
            )
        }

        assertTrue(error.message!!.contains("END marker appears before BEGIN marker"))
    }

    @Test
    fun unbindMissingFileIsAlreadyUnbound() {
        assertEquals(null, PicklesAgentsBinding.unbind(null))
        assertFalse(PicklesAgentsBinding.isBound(null))
    }
}
