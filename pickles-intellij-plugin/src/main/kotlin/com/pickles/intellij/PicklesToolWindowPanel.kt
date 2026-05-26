package com.pickles.intellij

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTabbedPane

class PicklesToolWindowPanel(
    private val project: com.intellij.openapi.project.Project,
    private val service: PicklesProjectService,
) : JPanel(BorderLayout()),
    Disposable {
    private val statusLabel = JBLabel()
    private val bindButton = JButton("Bind")
    private val unbindButton = JButton("Unbind")
    private val refreshButton = JButton("Refresh")
    private val reindexButton = JButton("Reindex")
    private val saveButton = JButton("Save Config")
    private val configText = JBTextArea(10, 40)
    private val problemsPanel = JPanel()
    private var listenerDisposable: Disposable? = null

    init {
        border = JBUI.Borders.empty(8)
        add(buildHeader(), BorderLayout.NORTH)
        add(buildBody(), BorderLayout.CENTER)

        bindButton.addActionListener { runAndRefresh("Bind failed") { service.bind() } }
        unbindButton.addActionListener { runAndRefresh("Unbind failed") { service.unbind() } }
        refreshButton.addActionListener { refresh() }
        reindexButton.addActionListener {
            service.reindexWorkspace()
            refresh()
        }
        saveButton.addActionListener { saveConfig() }

        listenerDisposable = service.addListener { refresh() }
        refresh()
    }

    override fun dispose() {
        listenerDisposable?.dispose()
    }

    private fun buildHeader(): JComponent {
        val panel = JPanel(BorderLayout())
        val actions = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0))
        actions.add(bindButton)
        actions.add(unbindButton)
        actions.add(refreshButton)
        actions.add(reindexButton)
        panel.add(actions, BorderLayout.WEST)
        panel.add(statusLabel, BorderLayout.CENTER)
        return panel
    }

    private fun buildBody(): JComponent {
        problemsPanel.layout = GridBagLayout()
        return JTabbedPane().apply {
            addTab("Problems", JBScrollPane(problemsPanel))
            addTab("Config", buildConfigPanel())
        }
    }

    private fun buildConfigPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val c = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            insets = JBUI.insets(4)
        }

        c.gridx = 0
        c.gridy = 0
        c.fill = GridBagConstraints.BOTH
        c.weightx = 1.0
        c.weighty = 1.0
        panel.add(JBScrollPane(configText), c)

        c.gridy = 1
        c.fill = GridBagConstraints.HORIZONTAL
        c.weighty = 0.0
        panel.add(saveButton, c)
        return panel
    }

    private fun refresh() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runCatching {
                val config = service.loadConfigText().getOrThrow()
                config to service.bindStatus()
            }

            ApplicationManager.getApplication().invokeLater {
                result.onFailure {
                    statusLabel.text = "Config read failed: ${it.message}"
                    return@invokeLater
                }

                val (configTextValue, bindStatus) = result.getOrThrow()
                bindButton.isEnabled = !bindStatus.bound
                unbindButton.isEnabled = bindStatus.bound
                val statusSnapshot = service.statusSnapshot()
                reindexButton.isEnabled = statusSnapshot.indexStatus != PicklesIndexStatus.RUNNING
                statusLabel.text = PicklesStatusText.format(statusSnapshot)

                configText.text = configTextValue

                refreshProblems()
            }
        }
    }

    private fun refreshProblems() {
        problemsPanel.removeAll()
        val problems = service.problems()
        val c = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            gridx = 0
            insets = JBUI.insets(4)
        }

        if (problems.isEmpty()) {
            c.gridy = 0
            problemsPanel.add(JBLabel("No current problems."), c)
        } else {
            PicklesProblemOrdering.sorted(problems).forEachIndexed { index, problem ->
                c.gridy = index
                problemsPanel.add(problemRow(problem), c)
            }
        }

        problemsPanel.revalidate()
        problemsPanel.repaint()
    }

    private fun problemRow(problem: PicklesProblem): JComponent {
        val row = JPanel(BorderLayout(8, 0)).apply {
            border = JBUI.Borders.empty(6)
        }
        val title = StringUtil.escapeXmlEntities(problem.title)
        val type = StringUtil.escapeXmlEntities(problem.type)
        val message = StringUtil.escapeXmlEntities(problem.message)
        val severity = StringUtil.escapeXmlEntities(problem.severity)
        val location = StringUtil.escapeXmlEntities(formatLocation(problem))
        val rule = StringUtil.escapeXmlEntities(problem.source.rule ?: "-")
        val text = JButton("<html><b>$severity</b> $title [$type]<br>$message<br>$location - rule: $rule</html>")
        text.horizontalAlignment = JButton.LEFT
        text.addActionListener { service.openProblem(problem) }
        val delete = JButton("Delete")
        delete.addActionListener { service.deleteProblem(problem) }
        row.add(text, BorderLayout.CENTER)
        row.add(delete, BorderLayout.EAST)
        return row
    }

    private fun saveConfig() {
        runAndRefresh("Save failed") { service.saveConfigText(configText.text) }
    }

    private fun formatLocation(problem: PicklesProblem): String {
        val file = problem.file ?: "workspace"
        val position = problem.position ?: return file
        return "$file:${position.line}:${position.column}"
    }

    private fun runAndRefresh(title: String, action: () -> Result<Unit>) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val result = action()
            ApplicationManager.getApplication().invokeLater {
                result.onFailure {
                    Messages.showErrorDialog(project, it.message ?: title, "Pickles")
                }
                refresh()
            }
        }
    }
}
