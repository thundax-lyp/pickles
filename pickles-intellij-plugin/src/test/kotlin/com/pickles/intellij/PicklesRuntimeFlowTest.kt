package com.pickles.intellij

import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

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
    fun problemBoardSummaryCountsEmptyErrorAndWarnProblems() {
        val problemBoard = PicklesProblemBoardState()

        assertEquals(
            PicklesProblemSummary(
                totalCount = 0,
                errorCount = 0,
                warnCount = 0,
                text = "No Pickles governance problems.",
            ),
            problemBoard.summary(),
        )

        problemBoard.replaceProblems(
            listOf(
                PicklesProblem(
                    title = "Blocking",
                    type = "architecture",
                    message = "Blocking problem.",
                    severity = "ERROR",
                ),
                PicklesProblem(
                    title = "Warning",
                    type = "maintainability",
                    message = "Warning problem.",
                    severity = "WARN",
                ),
            ),
        )

        assertEquals(
            PicklesProblemSummary(
                totalCount = 2,
                errorCount = 1,
                warnCount = 1,
                text = "Pickles found 1 blocking problem(s) and 1 warning(s).",
            ),
            problemBoard.summary(),
        )
    }

    @Test
    fun runtimeChangedFileDerivesRuntimeChangeType() {
        assertEquals("added", RuntimeChangedFile("src/New.java", null, "new").changeType)
        assertEquals("deleted", RuntimeChangedFile("src/Old.java", "old", null).changeType)
        assertEquals("modified", RuntimeChangedFile("src/File.java", "old", "new").changeType)
    }

    @Test
    fun workspaceIndexGateRejectsConcurrentRunsUntilFinished() {
        val gate = PicklesWorkspaceIndexGate()

        assertTrue(gate.tryStart())
        assertTrue(gate.isRunning())
        assertEquals(false, gate.tryStart())

        gate.finish()

        assertEquals(false, gate.isRunning())
        assertTrue(gate.tryStart())
    }

    @Test
    fun runtimeQueueStartsFirstRequestImmediately() {
        val queue = PicklesRuntimeQueue()

        val run = queue.enqueue(queueRequest(RuntimeQueueSource.REINDEX, runtimeFile("src/App.java", "reindex")))

        assertEquals(1L, run?.version)
        assertEquals(RuntimeQueueSource.REINDEX, run?.request?.source)
        assertEquals(
            RuntimeQueueSnapshot(
                running = true,
                pending = false,
                currentInvalidated = false,
            ),
            queue.snapshot(),
        )
    }

    @Test
    fun runtimeQueueQueuesNonOverlappingRequestAfterCurrentRun() {
        val queue = PicklesRuntimeQueue()
        val first = queue.enqueue(queueRequest(RuntimeQueueSource.REINDEX, runtimeFile("src/App.java", "reindex")))

        val second = queue.enqueue(queueRequest(RuntimeQueueSource.NOTIFY, runtimeFile("src/Other.java", "notify")))
        val completion = queue.complete(first!!.version)

        assertEquals(null, second)
        assertTrue(completion.shouldApplyResult)
        assertEquals(2L, completion.nextRun?.version)
        assertEquals(listOf("src/Other.java"), completion.nextRun?.request?.files?.map { it.fileName })
    }

    @Test
    fun runtimeQueueInvalidatesCurrentRunWhenOverlappingRequestArrives() {
        val queue = PicklesRuntimeQueue()
        val first = queue.enqueue(queueRequest(RuntimeQueueSource.REINDEX, runtimeFile("src/App.java", "reindex")))

        queue.enqueue(queueRequest(RuntimeQueueSource.NOTIFY, runtimeFile("src/App.java", "notify")))
        assertEquals(
            RuntimeQueueSnapshot(
                running = true,
                pending = true,
                currentInvalidated = true,
            ),
            queue.snapshot(),
        )
        val completion = queue.complete(first!!.version)

        assertEquals(false, completion.shouldApplyResult)
        assertEquals(2L, completion.nextRun?.version)
        assertEquals("notify", completion.nextRun?.request?.files?.single()?.after)
    }

    @Test
    fun runtimeQueueKeepsLatestPendingContentForSamePath() {
        val queue = PicklesRuntimeQueue()
        queue.enqueue(queueRequest(RuntimeQueueSource.REINDEX, runtimeFile("src/App.java", "reindex")))

        queue.enqueue(queueRequest(RuntimeQueueSource.NOTIFY, runtimeFile("src/Other.java", "old")))
        queue.enqueue(queueRequest(RuntimeQueueSource.NOTIFY, runtimeFile("src/Other.java", "new")))
        val completion = queue.complete(1L)

        assertEquals(true, completion.shouldApplyResult)
        assertEquals(RuntimeQueueSource.NOTIFY, completion.nextRun?.request?.source)
        assertEquals(listOf("new"), completion.nextRun?.request?.files?.map { it.after })
    }

    @Test
    fun problemOrderingSortsBySeverityLocationAndRuntimeOrder() {
        val warnWithLocation = PicklesProblem(
            title = "Warn with location",
            type = "maintainability",
            message = "Warn.",
            severity = "WARN",
            file = "src/Warn.java",
            position = ProblemPosition(line = 1, column = 1),
        )
        val errorWithoutLocation = PicklesProblem(
            title = "Error without location",
            type = "architecture",
            message = "Error.",
            severity = "ERROR",
        )
        val errorWithLocation = PicklesProblem(
            title = "Error with location",
            type = "architecture",
            message = "Error.",
            severity = "ERROR",
            file = "src/Error.java",
            position = ProblemPosition(line = 1, column = 1),
        )
        val warnWithoutLocation = PicklesProblem(
            title = "Warn without location",
            type = "maintainability",
            message = "Warn.",
            severity = "WARN",
        )

        assertEquals(
            listOf(errorWithLocation, errorWithoutLocation, warnWithLocation, warnWithoutLocation),
            PicklesProblemOrdering.sorted(
                listOf(warnWithoutLocation, warnWithLocation, errorWithoutLocation, errorWithLocation),
            ),
        )
    }

    @Test
    fun workspaceInspectionCollectsRepoRelativeJavaFilesAsModifiedInputs() {
        val root = temporaryFolder.newFolder("workspace").toPath()
        val javaFile = root.resolve("src/main/java/com/example/App.java")
        val ignoredFile = root.resolve("src/main/resources/app.txt")
        javaFile.parent.toFile().mkdirs()
        ignoredFile.parent.toFile().mkdirs()
        javaFile.toFile().writeText("class App {}\n")
        ignoredFile.toFile().writeText("ignored")

        val files = PicklesWorkspaceInspection.collectJavaFiles(root)

        assertEquals(1, files.size)
        assertEquals("src/main/java/com/example/App.java", files.single().fileName)
        assertEquals(null, files.single().before)
        assertEquals("class App {}\n", files.single().after)
        assertEquals("modified", files.single().changeType)
    }

    @Test
    fun workspaceInspectionUsesBuiltInAndGitignoreFilters() {
        val root = temporaryFolder.newFolder("workspace").toPath()
        root.resolve(".gitignore").toFile().writeText(
            """
            generated/
            *.generated.java
            """.trimIndent(),
        )
        val included = root.resolve("src/main/java/com/example/App.java")
        val buildOutput = root.resolve("build/generated/BuildOutput.java")
        val generatedDirectory = root.resolve("generated/Ignored.java")
        val generatedFile = root.resolve("src/main/java/com/example/App.generated.java")
        listOf(included, buildOutput, generatedDirectory, generatedFile).forEach { file ->
            file.parent.toFile().mkdirs()
            file.toFile().writeText("class Ignored {}\n")
        }

        val files = PicklesWorkspaceInspection.collectJavaFiles(root)

        assertEquals(listOf("src/main/java/com/example/App.java"), files.map { it.fileName })
    }

    @Test
    fun workspaceInspectionDoesNotParsePicklesConfigIgnore() {
        val root = temporaryFolder.newFolder("workspace").toPath()
        root.resolve("pickles.config.ts").toFile().writeText(
            """
            export default {
                workspace: {
                    ignore: ["generated/"],
                },
            };
            """.trimIndent(),
        )
        val generatedFile = root.resolve("generated/Ignored.java")
        generatedFile.parent.toFile().mkdirs()
        generatedFile.toFile().writeText("class Ignored {}\n")

        val files = PicklesWorkspaceInspection.collectJavaFiles(root)

        assertEquals(listOf("generated/Ignored.java"), files.map { it.fileName })
    }

    @Test
    fun workspaceInspectionCallsRuntimeAndStoresProblemBoardData() {
        val root = temporaryFolder.newFolder("workspace").toPath()
        val javaFile = root.resolve("src/main/java/com/example/App.java")
        javaFile.parent.toFile().mkdirs()
        javaFile.toFile().writeText("class App {}\n")
        val problem = PicklesProblem(
            title = "Problem",
            type = "architecture",
            message = "Message.",
            severity = "WARN",
            file = "src/main/java/com/example/App.java",
        )
        val runtime = RecordingRuntimeClient(listOf(problem))
        val problemBoard = PicklesProblemBoardState()

        val problems = PicklesWorkspaceInspection.inspect(root, runtime, problemBoard)

        assertEquals(listOf(problem), problems)
        assertEquals(listOf(problem), problemBoard.problems())
        assertEquals(1, runtime.receivedFiles.size)
        assertEquals("src/main/java/com/example/App.java", runtime.receivedFiles.single().fileName)
    }

    @Test
    fun workspaceInspectionFailureKeepsExistingProblemBoardData() {
        val root = temporaryFolder.newFolder("workspace").toPath()
        val javaFile = root.resolve("src/main/java/com/example/App.java")
        javaFile.parent.toFile().mkdirs()
        javaFile.toFile().writeText("class App {}\n")
        val existingProblem = PicklesProblem(
            title = "Existing",
            type = "architecture",
            message = "Existing problem.",
            severity = "ERROR",
        )
        val problemBoard = PicklesProblemBoardState()
        problemBoard.replaceProblems(listOf(existingProblem))

        assertThrows(IOException::class.java) {
            PicklesWorkspaceInspection.inspect(root, FailingRuntimeClient(), problemBoard)
        }

        assertEquals(listOf(existingProblem), problemBoard.problems())
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

    @Test
    fun runtimeResponseParserParsesSuccessProblems() {
        val problems = NodePicklesRuntimeResponseParser.parse(
            stdout = """
            {
              "problems": [
                {
                  "title": "Problem",
                  "type": "architecture",
                  "message": "Message",
                  "severity": "ERROR",
                  "source": {
                    "tool": "pickles-native",
                    "rule": "sample-rule"
                  },
                  "file": "src/File.java",
                  "position": {
                    "line": 1,
                    "column": 2
                  },
                  "fixHint": "Fix it."
                }
              ]
            }
            """.trimIndent(),
            stderr = "",
            exitCode = 0,
            gson = gson,
        )

        assertEquals(1, problems.size)
        assertEquals("sample-rule", problems.single().source.rule)
        assertEquals("Fix it.", problems.single().fixHint)
    }

    @Test
    fun runtimeResponseParserFailsOnRuntimeError() {
        val error = assertThrows(IOException::class.java) {
            NodePicklesRuntimeResponseParser.parse(
                stdout = """{"error":{"message":"Config failed."}}""",
                stderr = "",
                exitCode = 1,
                gson = gson,
            )
        }

        assertEquals("Config failed.", error.message)
    }

    @Test
    fun runtimeResponseParserFailsOnInvalidJson() {
        val error = assertThrows(IOException::class.java) {
            NodePicklesRuntimeResponseParser.parse(
                stdout = "not-json",
                stderr = "",
                exitCode = 0,
                gson = gson,
            )
        }

        assertEquals("Pickles Runtime returned invalid JSON.", error.message)
    }

    @Test
    fun runtimeResponseParserFailsOnEmptyStdout() {
        val error = assertThrows(IOException::class.java) {
            NodePicklesRuntimeResponseParser.parse(
                stdout = "",
                stderr = "",
                exitCode = 0,
                gson = gson,
            )
        }

        assertEquals("Pickles Runtime returned an empty response.", error.message)
    }

    @Test
    fun notifyReturnsInternalErrorWhenRuntimeFailsWithoutClearingProblemBoard() {
        val root = temporaryFolder.newFolder("workspace").toPath()
        val existingProblem = PicklesProblem(
            title = "Existing",
            type = "architecture",
            message = "Existing problem.",
            severity = "ERROR",
        )
        val problemBoard = PicklesProblemBoardState()
        problemBoard.replaceProblems(listOf(existingProblem))
        val handler = PicklesHttpContractHandler(
            gson = gson,
            projectRoot = root,
            runtimeClient = FailingRuntimeClient(),
            problemBoard = problemBoard,
        )

        val result = handler.notify(
            """
            {
              "schemaVersion": 1,
              "requestId": "req-runtime-error",
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
        val body = result.body as ApiErrorResponse

        assertEquals(500, result.status)
        assertEquals("req-runtime-error", body.requestId)
        assertEquals("INTERNAL_ERROR", body.error.code)
        assertEquals("Runtime unavailable.", body.error.message)
        assertEquals(listOf(existingProblem), problemBoard.problems())
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

    private class FailingRuntimeClient : PicklesRuntimeClient {
        override fun inspect(files: List<RuntimeChangedFile>): List<PicklesProblem> = throw IOException("Runtime unavailable.")
    }

    private fun queueRequest(
        source: RuntimeQueueSource,
        vararg files: RuntimeChangedFile,
    ): RuntimeQueueRequest = RuntimeQueueRequest(
        source = source,
        files = files.toList(),
    )

    private fun runtimeFile(path: String, after: String): RuntimeChangedFile = RuntimeChangedFile(
        fileName = path,
        before = null,
        after = after,
    )

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
