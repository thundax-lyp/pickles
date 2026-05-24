package com.pickles.intellij

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PicklesModelsTest {
    @Test
    fun defaultConfigMatchesMvpContract() {
        val config = PicklesConfig()

        assertEquals(1, config.version)
        assertEquals("codex", config.agent)
        assertEquals("AGENTS.md", config.bind.agentsFile)
        assertFalse(config.bind.enabled)
        assertEquals("http", config.hook.protocol)
        assertTrue(config.rules.archunit.enabled)
        assertEquals("", config.rules.archunit.command)
        assertTrue(config.rules.eslint.enabled)
        assertEquals("", config.rules.eslint.command)
        assertEquals(emptyList<String>(), config.rules.scripts)
        assertEquals("workspace", config.problemBoard.aggregation)
    }

    @Test
    fun defaultProblemPositionUsesOneBasedLocation() {
        val problem = PicklesProblem(
            title = "Title",
            type = "rule",
            message = "Message",
        )

        assertEquals("WARN", problem.severity)
        assertEquals("pickles", problem.source.tool)
        assertEquals(null, problem.source.rule)
        assertEquals(1, problem.position.line)
        assertEquals(1, problem.position.column)
    }
}
