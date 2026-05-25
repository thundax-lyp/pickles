package com.pickles.intellij

import org.junit.Assert.assertEquals
import org.junit.Test

class PicklesModelsTest {
    @Test
    fun bindStatusUsesProjectFiles() {
        val status = BindStatus(
            agentsFileExists = true,
            hooksFileExists = true,
        )

        assertEquals(true, status.bound)
    }

    @Test
    fun defaultProblemHasWorkspaceLevelLocation() {
        val problem = PicklesProblem(
            title = "Title",
            type = "rule",
            message = "Message",
        )

        assertEquals("WARN", problem.severity)
        assertEquals("pickles", problem.source.tool)
        assertEquals(null, problem.source.rule)
        assertEquals(null, problem.file)
        assertEquals(null, problem.position)
    }
}
