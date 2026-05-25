package com.pickles.intellij

import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PicklesRuntimeFlowTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val gson = GsonBuilder().serializeNulls().create()

    @Test
    fun notifyCallsRuntimeAndStoresProblemBoardData() {
        val root = temporaryFolder.newFolder("workspace").toPath()
        val runtime = RecordingRuntimeClient(
            listOf(
                PicklesProblem(
                    title = "Controller must not import repository directly",
                    type = "architecture",
                    message = "Controller imports repository directly.",
                    severity = "ERROR",
                    source = ProblemSource(tool = "pickles-native", rule = "sample-rule"),
                    file = "src/main/java/com/example/web/OrderController.java",
                    position = ProblemPosition(line = 3, column = 1),
                ),
            ),
        )
        val problemBoard = PicklesProblemBoardState()
        val handler = PicklesHttpContractHandler(
            gson = gson,
            projectRoot = root,
            runtimeClient = runtime,
            problemBoard = problemBoard,
        )

        val result = handler.notify(
            """
            {
              "schemaVersion": 1,
              "requestId": "req-runtime-1",
              "event": {
                "sessionId": "session-1",
                "turnId": "turn-1",
                "hookEventName": "PostToolUse",
                "workspace": "${root.toAbsolutePath()}",
                "idempotencyKey": "session-1:turn-1:PostToolUse:src/main/java/com/example/web/OrderController.java"
              },
              "files": [
                {
                  "fileName": "src/main/java/com/example/web/OrderController.java",
                  "before": "old",
                  "after": "new"
                }
              ]
            }
            """.trimIndent(),
        )
        val body = result.body as NotifyResponse

        assertEquals(202, result.status)
        assertTrue(body.processed)
        assertEquals(
            listOf(RuntimeChangedFile("src/main/java/com/example/web/OrderController.java", "old", "new")),
            runtime.receivedFiles,
        )
        assertEquals(runtime.problemsToReturn, problemBoard.problems())
    }

    @Test
    fun deletingProblemOnlyChangesCurrentBoardState() {
        val problem = PicklesProblem(
            title = "Problem",
            type = "architecture",
            message = "Message",
            severity = "WARN",
        )
        val problemBoard = PicklesProblemBoardState()

        problemBoard.replaceProblems(listOf(problem))
        problemBoard.deleteProblem(problem)

        assertEquals(emptyList<PicklesProblem>(), problemBoard.problems())
    }

    @Test
    fun runtimeChangedFileDerivesRuntimeChangeType() {
        assertEquals("added", RuntimeChangedFile("src/New.java", null, "new").changeType)
        assertEquals("deleted", RuntimeChangedFile("src/Old.java", "old", null).changeType)
        assertEquals("modified", RuntimeChangedFile("src/File.java", "old", "new").changeType)
    }

    @Test
    fun runtimeLocatorFindsConfiguredRuntimeRoot() {
        val projectRoot = temporaryFolder.newFolder("workspace").toPath()
        val runtimeRoot = temporaryFolder.newFolder("runtime").toPath()
        runtimeRoot.resolve("src").toFile().mkdirs()
        runtimeRoot.resolve("package.json").toFile().writeText("{}")
        runtimeRoot.resolve("src").resolve("stdio.ts").toFile().writeText("")

        withSystemProperty("pickles.runtime.dir", runtimeRoot.toString()) {
            assertEquals(runtimeRoot.toAbsolutePath().normalize(), PicklesRuntimeLocator.find(projectRoot))
        }
    }

    @Test
    fun runtimeLocatorReturnsNullWhenRuntimeRootIsUnavailable() {
        val projectRoot = temporaryFolder.newFolder("workspace").toPath()

        withSystemProperty("pickles.runtime.dir", "") {
            assertEquals(null, PicklesRuntimeLocator.find(projectRoot))
        }
    }

    private class RecordingRuntimeClient(
        val problemsToReturn: List<PicklesProblem>,
    ) : PicklesRuntimeClient {
        var receivedFiles: List<RuntimeChangedFile> = emptyList()
            private set

        override fun inspect(files: List<RuntimeChangedFile>): List<PicklesProblem> {
            receivedFiles = files
            return problemsToReturn
        }
    }

    private fun withSystemProperty(name: String, value: String, action: () -> Unit) {
        val previous = System.getProperty(name)
        try {
            System.setProperty(name, value)
            action()
        } finally {
            if (previous == null) {
                System.clearProperty(name)
            } else {
                System.setProperty(name, previous)
            }
        }
    }
}
