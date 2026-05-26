package com.pickles.intellij

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import java.nio.file.Path

const val PICKLES_HTTP_SCHEMA_VERSION = 1

data class HookEvent(
    val sessionId: String? = null,
    val turnId: String? = null,
    val hookEventName: String? = null,
    val workspace: String? = null,
    val idempotencyKey: String? = null,
)

data class ChangedFile(
    val fileName: String? = null,
    val before: String? = null,
    val after: String? = null,
)

data class NotifyRequest(
    val schemaVersion: Int? = null,
    val requestId: String? = null,
    val event: HookEvent? = null,
    val files: List<ChangedFile>? = null,
)

data class NotifyResponse(
    val schemaVersion: Int = PICKLES_HTTP_SCHEMA_VERSION,
    val requestId: String,
    val accepted: Boolean = true,
    val processed: Boolean = false,
)

data class FeedbackRequest(
    val schemaVersion: Int? = null,
    val requestId: String? = null,
    val sessionId: String? = null,
    val turnId: String? = null,
    val workspace: String? = null,
)

data class FeedbackSummary(
    val errorCount: Int,
    val warnCount: Int,
    val text: String,
)

data class FeedbackResponse(
    val schemaVersion: Int = PICKLES_HTTP_SCHEMA_VERSION,
    val requestId: String,
    val status: String,
    val hasBlockingProblems: Boolean,
    val summary: FeedbackSummary,
    val problems: List<PicklesProblem>,
)

data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, Any> = emptyMap(),
)

data class ApiErrorResponse(
    val schemaVersion: Int = PICKLES_HTTP_SCHEMA_VERSION,
    val requestId: String?,
    val error: ApiError,
)

data class HealthResponse(
    val schemaVersion: Int = PICKLES_HTTP_SCHEMA_VERSION,
    val requestId: String? = null,
    val status: String = "ok",
)

data class PicklesHttpResult(
    val status: Int,
    val body: Any,
)

class PicklesHttpContractHandler(
    private val gson: Gson,
    private val projectRoot: Path,
    private val notifyQueue: ((List<RuntimeChangedFile>) -> Unit)? = null,
    private val problemBoard: PicklesProblemBoardState? = null,
) {
    fun health(): PicklesHttpResult = PicklesHttpResult(200, HealthResponse())

    fun notify(body: String): PicklesHttpResult {
        val requestId = extractRequestId(body)
        val request = parseJson(body, NotifyRequest::class.java)
            ?: return error(400, requestId, "INVALID_REQUEST", "Request body must be valid JSON.")

        val commonError = validateCommon(request.schemaVersion, request.requestId)
        if (commonError != null) return commonError

        val event = request.event
            ?: return error(400, request.requestId, "INVALID_REQUEST", "Missing event.")
        val sessionId = event.sessionId
        if (sessionId.isNullOrBlank()) {
            return error(400, request.requestId, "INVALID_REQUEST", "Missing event.sessionId.")
        }
        val hookEventName = event.hookEventName
        if (hookEventName !in HOOK_EVENT_NAMES) {
            return error(400, request.requestId, "INVALID_REQUEST", "Invalid event.hookEventName.")
        }
        if (event.idempotencyKey.isNullOrBlank()) {
            return error(400, request.requestId, "INVALID_REQUEST", "Missing event.idempotencyKey.")
        }
        val workspaceError = validateWorkspace(event.workspace, request.requestId)
        if (workspaceError != null) return workspaceError

        val files = request.files
            ?: return error(400, request.requestId, "INVALID_REQUEST", "Missing files.")
        files.forEachIndexed { index, file ->
            val fileName = file.fileName
            if (fileName.isNullOrBlank()) {
                return error(400, request.requestId, "INVALID_REQUEST", "Missing files[$index].fileName.")
            }
            if (Path.of(fileName).isAbsolute()) {
                return error(400, request.requestId, "INVALID_REQUEST", "ChangedFile.fileName must be repo-relative.")
            }
            if (file.before == null && file.after == null) {
                return error(400, request.requestId, "INVALID_REQUEST", "ChangedFile.before and after cannot both be null.")
            }
        }

        notifyQueue?.invoke(
            files.map { file ->
                RuntimeChangedFile(
                    fileName = file.fileName!!,
                    before = file.before,
                    after = file.after,
                )
            },
        )

        return PicklesHttpResult(
            202,
            NotifyResponse(
                requestId = request.requestId!!,
                processed = notifyQueue != null,
            ),
        )
    }

    fun feedback(body: String): PicklesHttpResult {
        val requestId = extractRequestId(body)
        val request = parseJson(body, FeedbackRequest::class.java)
            ?: return error(400, requestId, "INVALID_REQUEST", "Request body must be valid JSON.")

        val commonError = validateCommon(request.schemaVersion, request.requestId)
        if (commonError != null) return commonError

        if (request.sessionId.isNullOrBlank()) {
            return error(400, request.requestId, "INVALID_REQUEST", "Missing sessionId.")
        }
        val workspaceError = validateWorkspace(request.workspace, request.requestId)
        if (workspaceError != null) return workspaceError

        val problems = problemBoard?.problems()
        if (problems == null) {
            return PicklesHttpResult(
                200,
                FeedbackResponse(
                    requestId = request.requestId!!,
                    status = "unimplemented",
                    hasBlockingProblems = false,
                    summary = FeedbackSummary(
                        errorCount = 0,
                        warnCount = 0,
                        text = "Governance feedback is not implemented yet.",
                    ),
                    problems = emptyList(),
                ),
            )
        }

        return PicklesHttpResult(
            200,
            FeedbackResponse(
                requestId = request.requestId!!,
                status = "ok",
                hasBlockingProblems = problems.any { it.severity == "ERROR" },
                summary = feedbackSummary(problems),
                problems = problems,
            ),
        )
    }

    fun methodNotAllowed(requestId: String? = null): PicklesHttpResult = error(405, requestId, "INVALID_REQUEST", "Method not allowed.")

    fun internalError(message: String): PicklesHttpResult = error(500, null, "INTERNAL_ERROR", message)

    private fun validateCommon(schemaVersion: Int?, requestId: String?): PicklesHttpResult? {
        if (schemaVersion != PICKLES_HTTP_SCHEMA_VERSION) {
            return error(400, requestId, "INVALID_REQUEST", "schemaVersion must be 1.")
        }
        if (requestId.isNullOrBlank()) {
            return error(400, null, "INVALID_REQUEST", "Missing requestId.")
        }
        return null
    }

    private fun validateWorkspace(workspace: String?, requestId: String?): PicklesHttpResult? {
        if (workspace.isNullOrBlank()) {
            return error(400, requestId, "INVALID_REQUEST", "Missing workspace.")
        }
        val workspacePath = runCatching { Path.of(workspace).toRealPath() }.getOrNull()
            ?: return error(404, requestId, "WORKSPACE_MISMATCH", "Workspace does not match this project.")
        val rootPath = runCatching { projectRoot.toRealPath() }.getOrDefault(projectRoot.toAbsolutePath().normalize())
        if (workspacePath != rootPath) {
            return error(404, requestId, "WORKSPACE_MISMATCH", "Workspace does not match this project.")
        }
        return null
    }

    private fun error(status: Int, requestId: String?, code: String, message: String): PicklesHttpResult = PicklesHttpResult(
        status,
        ApiErrorResponse(
            requestId = requestId,
            error = ApiError(
                code = code,
                message = message,
                details = emptyMap(),
            ),
        ),
    )

    private fun extractRequestId(body: String): String? = runCatching {
        JsonParser.parseString(body).asJsonObject.get("requestId")?.takeIf { !it.isJsonNull }?.asString
    }.getOrNull()

    private fun <T> parseJson(body: String, type: Class<T>): T? = try {
        gson.fromJson(body, type)
    } catch (_: JsonParseException) {
        null
    } catch (_: IllegalStateException) {
        null
    }

    private companion object {
        val HOOK_EVENT_NAMES = setOf("SessionStart", "PreToolUse", "PostToolUse", "Stop")
    }
}

private fun feedbackSummary(problems: List<PicklesProblem>): FeedbackSummary {
    val errorCount = problems.count { it.severity == "ERROR" }
    val warnCount = problems.count { it.severity == "WARN" }
    val text = when {
        problems.isEmpty() -> "No Pickles governance problems."
        errorCount > 0 -> "Pickles found $errorCount blocking problem(s) and $warnCount warning(s)."
        else -> "Pickles found $warnCount warning(s)."
    }

    return FeedbackSummary(
        errorCount = errorCount,
        warnCount = warnCount,
        text = text,
    )
}
