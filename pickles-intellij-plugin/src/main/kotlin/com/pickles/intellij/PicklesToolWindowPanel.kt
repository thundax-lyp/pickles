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
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

class PicklesToolWindowPanel(
    private val project: com.intellij.openapi.project.Project,
    private val service: PicklesProjectService,
) : JPanel(BorderLayout()), Disposable {
    private val statusLabel = JBLabel()
    private val bindButton = JButton("Bind")
    private val unbindButton = JButton("Unbind")
    private val refreshButton = JButton("Refresh")
    private val saveButton = JButton("Save Config")
    private val archunitEnabled = JCheckBox("ArchUnit")
    private val eslintEnabled = JCheckBox("ESLint")
    private val archunitCommand = JTextField()
    private val eslintCommand = JTextField()
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
        panel.add(actions, BorderLayout.WEST)
        panel.add(statusLabel, BorderLayout.CENTER)
        return panel
    }

    private fun buildBody(): JComponent {
        val root = JPanel(GridBagLayout())
        val constraints = GridBagConstraints().apply {
            fill = GridBagConstraints.BOTH
            weightx = 1.0
            insets = JBUI.insets(6)
            gridx = 0
        }

        constraints.gridy = 0
        constraints.weighty = 0.0
        root.add(buildConfigPanel(), constraints)

        constraints.gridy = 1
        constraints.weighty = 1.0
        problemsPanel.layout = GridBagLayout()
        root.add(JBScrollPane(problemsPanel), constraints)

        return root
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
        c.weightx = 0.0
        panel.add(archunitEnabled, c)
        c.gridx = 1
        c.weightx = 1.0
        panel.add(archunitCommand, c)

        c.gridx = 0
        c.gridy = 1
        c.weightx = 0.0
        panel.add(eslintEnabled, c)
        c.gridx = 1
        c.weightx = 1.0
        panel.add(eslintCommand, c)

        c.gridx = 0
        c.gridy = 2
        c.gridwidth = 2
        c.fill = GridBagConstraints.BOTH
        c.weighty = 1.0
        panel.add(JBScrollPane(configText), c)

        c.gridy = 3
        c.fill = GridBagConstraints.HORIZONTAL
        c.weighty = 0.0
        panel.add(saveButton, c)
        return panel
    }

    private fun refresh() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runCatching {
                val config = service.loadConfig().getOrThrow()
                config to service.bindStatus(config)
            }

            ApplicationManager.getApplication().invokeLater {
                result.onFailure {
                    statusLabel.text = "Config read failed: ${it.message}"
                    return@invokeLater
                }

                val (config, bindStatus) = result.getOrThrow()
                bindButton.isEnabled = !bindStatus.bound
                unbindButton.isEnabled = bindStatus.bound
                statusLabel.text = service.lastStatus

                archunitEnabled.isSelected = config.rules.archunit.enabled
                eslintEnabled.isSelected = config.rules.eslint.enabled
                archunitCommand.text = config.rules.archunit.command
                eslintCommand.text = config.rules.eslint.command
                configText.text = service.formatConfig(config)

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
            problems.forEachIndexed { index, problem ->
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
        val text = JButton("<html><b>$title</b> [$type]<br>$message</html>")
        text.horizontalAlignment = JButton.LEFT
        text.addActionListener { service.openProblem(problem) }
        val delete = JButton("Delete")
        delete.addActionListener { service.deleteProblem(problem) }
        row.add(text, BorderLayout.CENTER)
        row.add(delete, BorderLayout.EAST)
        return row
    }

    private fun saveConfig() {
        val parsed = service.parseConfig(configText.text).getOrElse {
            Messages.showErrorDialog(project, "Config JSON is invalid: ${it.message}", "Pickles")
            return
        }
        val updated = parsed.copy(
            rules = parsed.rules.copy(
                archunit = parsed.rules.archunit.copy(
                    enabled = archunitEnabled.isSelected,
                    command = archunitCommand.text,
                ),
                eslint = parsed.rules.eslint.copy(
                    enabled = eslintEnabled.isSelected,
                    command = eslintCommand.text,
                ),
            ),
        )
        runAndRefresh("Save failed") { service.saveConfig(updated) }
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
