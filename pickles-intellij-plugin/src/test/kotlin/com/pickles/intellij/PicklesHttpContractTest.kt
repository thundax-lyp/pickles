package com.pickles.intellij

import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PicklesHttpContractTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val gson = GsonBuilder().serializeNulls().create()

    @Test
    fun healthReturnsContractEnvelope() {
        val result = handler().health()
        val body = result.body as HealthResponse
        val json = gson.toJson(body)

        assertEquals(200, result.status)
        assertEquals(1, body.schemaVersion)
        assertEquals(null, body.requestId)
        assertEquals("ok", body.status)
        assertTrue(json.contains("\"requestId\":null"))
    }

    @Test
    fun notifyReturnsAcceptedStubForValidRequest() {
        val root = temporaryFolder.newFolder("workspace").toPath()
        val result = handler(root).notify(
            """
            {
              "schemaVersion": 1,
              "requestId": "req-1",
              "event": {
                "sessionId": "session-1",
                "turnId": "turn-1",
                "hookEventName": "PostToolUse",
                "workspace": "${root.toAbsolutePath()}",
                "idempotencyKey": "session-1:turn-1:PostToolUse:src/File.kt"
              },
              "files": [
                {
                  "fileName": "src/File.kt",
                  "before": "old",
                  "after": "new"
                }
              ]
            }
            """.trimIndent(),
        )
        val body = result.body as NotifyResponse

        assertEquals(202, result.status)
        assertEquals("req-1", body.requestId)
        assertTrue(body.accepted)
        assertFalse(body.processed)
    }

    @Test
    fun notifyReturnsProcessedWhenRequestIsQueued() {
        val root = temporaryFolder.newFolder("workspace").toPath()
        var queuedFiles: List<RuntimeChangedFile> = emptyList()
        val result = PicklesHttpContractHandler(
            gson = gson,
            projectRoot = root,
            notifyQueue = { files -> queuedFiles = files },
        ).notify(
            """
            {
              "schemaVersion": 1,
              "requestId": "req-queued",
              "event": {
                "sessionId": "session-1",
                "turnId": "turn-1",
                "hookEventName": "PostToolUse",
                "workspace": "${root.toAbsolutePath()}",
                "idempotencyKey": "session-1:turn-1:PostToolUse:src/File.kt"
              },
              "files": [
                {
                  "fileName": "src/File.kt",
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
        assertEquals(listOf(RuntimeChangedFile("src/File.kt", "old", "new")), queuedFiles)
    }

    @Test
    fun notifyRejectsMissingSessionId() {
        val root = temporaryFolder.newFolder("workspace").toPath()
        val result = handler(root).notify(
            """
            {
              "schemaVersion": 1,
              "requestId": "req-2",
              "event": {
                "hookEventName": "PostToolUse",
                "workspace": "${root.toAbsolutePath()}",
                "idempotencyKey": "session-1:turn-1:PostToolUse:src/File.kt"
              },
              "files": []
            }
            """.trimIndent(),
        )
        val body = result.body as ApiErrorResponse

        assertEquals(400, result.status)
        assertEquals("req-2", body.requestId)
        assertEquals("INVALID_REQUEST", body.error.code)
        assertEquals("Missing event.sessionId.", body.error.message)
    }

    @Test
    fun notifyRejectsWorkspaceMismatch() {
        val root = temporaryFolder.newFolder("workspace").toPath()
        val otherRoot = temporaryFolder.newFolder("other-workspace").toPath()
        val result = handler(root).notify(
            """
            {
              "schemaVersion": 1,
              "requestId": "req-3",
              "event": {
                "sessionId": "session-1",
                "turnId": "turn-1",
                "hookEventName": "PostToolUse",
                "workspace": "${otherRoot.toAbsolutePath()}",
                "idempotencyKey": "session-1:turn-1:PostToolUse:src/File.kt"
              },
              "files": []
            }
            """.trimIndent(),
        )
        val body = result.body as ApiErrorResponse

        assertEquals(404, result.status)
        assertEquals("req-3", body.requestId)
        assertEquals("WORKSPACE_MISMATCH", body.error.code)
    }

    @Test
    fun feedbackReturnsUnimplementedStub() {
        val root = temporaryFolder.newFolder("workspace").toPath()
        val result = handler(root).feedback(
            """
            {
              "schemaVersion": 1,
              "requestId": "req-4",
              "sessionId": "session-1",
              "turnId": "turn-1",
              "workspace": "${root.toAbsolutePath()}"
            }
            """.trimIndent(),
        )
        val body = result.body as FeedbackResponse

        assertEquals(200, result.status)
        assertEquals("req-4", body.requestId)
        assertEquals("unimplemented", body.status)
        assertFalse(body.hasBlockingProblems)
        assertEquals(0, body.summary.errorCount)
        assertEquals(0, body.summary.warnCount)
        assertEquals(emptyList<PicklesProblem>(), body.problems)
    }

    @Test
    fun feedbackReturnsOkWhenProblemBoardIsAvailable() {
        val root = temporaryFolder.newFolder("workspace").toPath()
        val problem = PicklesProblem(
            title = "Problem",
            type = "architecture",
            message = "Message",
            severity = "ERROR",
        )
        val problemBoard = PicklesProblemBoardState()
        problemBoard.replaceProblems(listOf(problem))
        val result = PicklesHttpContractHandler(
            gson = gson,
            projectRoot = root,
            problemBoard = problemBoard,
        ).feedback(
            """
            {
              "schemaVersion": 1,
              "requestId": "req-feedback-ok",
              "sessionId": "session-1",
              "turnId": "turn-1",
              "workspace": "${root.toAbsolutePath()}"
            }
            """.trimIndent(),
        )
        val body = result.body as FeedbackResponse

        assertEquals(200, result.status)
        assertEquals("ok", body.status)
        assertTrue(body.hasBlockingProblems)
        assertEquals(1, body.summary.errorCount)
        assertEquals(0, body.summary.warnCount)
        assertEquals(listOf(problem), body.problems)
    }

    @Test
    fun methodNotAllowedUsesErrorEnvelope() {
        val result = handler().methodNotAllowed()
        val body = result.body as ApiErrorResponse

        assertEquals(405, result.status)
        assertEquals(null, body.requestId)
        assertEquals(1, body.schemaVersion)
        assertEquals("INVALID_REQUEST", body.error.code)
    }

    @Test
    fun responseObjectsSerializeWithoutGlobalCodeMessageDataWrapper() {
        val body = gson.toJson(NotifyResponse(requestId = "req-5"))

        assertFalse(body.contains("\"code\""))
        assertFalse(body.contains("\"message\""))
        assertFalse(body.contains("\"data\""))
        assertTrue(body.contains("\"accepted\""))
        assertTrue(body.contains("\"processed\""))
    }

    private fun handler() = handler(temporaryFolder.newFolder("workspace").toPath())

    private fun handler(root: java.nio.file.Path) = PicklesHttpContractHandler(
        gson = gson,
        projectRoot = root,
    )
}
