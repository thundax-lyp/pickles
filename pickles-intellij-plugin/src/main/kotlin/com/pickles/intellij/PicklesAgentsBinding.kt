package com.pickles.intellij

object PicklesAgentsBinding {
    const val BEGIN_MARKER = "<!-- PICKLES:BEGIN -->"
    const val END_MARKER = "<!-- PICKLES:END -->"

    val managedBlock: String = """
        $BEGIN_MARKER
        ## Pickles Governance

        Before finalizing a task:

        1. Run or request the current Pickles feedback.
        2. Fix `ERROR` problems before reporting completion.
        3. Review `WARN` problems and either fix them or mention why they remain.
        4. Do not edit `.pickles/` runtime state files manually.
        5. Keep Pickles rules in `pickles.config.*` or explicitly imported `.pickles/rules/*` modules.
        $END_MARKER
    """.trimIndent()

    fun isBound(content: String?): Boolean = content?.let { locateManagedBlock(it) != null } ?: false

    fun bind(content: String?): String {
        val existing = content.orEmpty()
        val blockRange = locateManagedBlock(existing)
        if (blockRange != null) {
            return existing.replaceRange(blockRange, managedBlock).ensureTrailingNewline()
        }

        if (existing.isBlank()) {
            return "$managedBlock\n"
        }

        return existing.trimEnd() + "\n\n" + managedBlock + "\n"
    }

    fun unbind(content: String?): String? {
        val existing = content ?: return null
        val blockRange = locateManagedBlock(existing) ?: return existing
        return removeManagedBlock(existing, blockRange)
    }

    private fun locateManagedBlock(content: String): IntRange? {
        val beginIndexes = markerIndexes(content, BEGIN_MARKER)
        val endIndexes = markerIndexes(content, END_MARKER)

        if (beginIndexes.isEmpty() && endIndexes.isEmpty()) {
            return null
        }

        if (beginIndexes.size != 1 || endIndexes.size != 1) {
            throw PicklesAgentsBindingConflict("Pickles AGENTS.md block must contain exactly one BEGIN marker and one END marker.")
        }

        val begin = beginIndexes.single()
        val end = endIndexes.single()
        if (end < begin) {
            throw PicklesAgentsBindingConflict("Pickles AGENTS.md END marker appears before BEGIN marker.")
        }

        return begin until end + END_MARKER.length
    }

    private fun markerIndexes(content: String, marker: String): List<Int> {
        val indexes = mutableListOf<Int>()
        var startIndex = content.indexOf(marker)
        while (startIndex >= 0) {
            indexes.add(startIndex)
            startIndex = content.indexOf(marker, startIndex + marker.length)
        }
        return indexes
    }

    private fun removeManagedBlock(content: String, blockRange: IntRange): String {
        val before = content.substring(0, blockRange.first).trimEnd()
        val after = content.substring(blockRange.last + 1).trimStart().trimEnd()

        return when {
            before.isEmpty() && after.isEmpty() -> ""
            before.isEmpty() -> "$after\n"
            after.isEmpty() -> "$before\n"
            else -> "$before\n\n$after\n"
        }
    }

    private fun String.ensureTrailingNewline(): String = if (endsWith("\n")) this else "$this\n"
}

class PicklesAgentsBindingConflict(message: String) : IllegalStateException(message)
