package com.pickles.intellij

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class PicklesToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = PicklesToolWindowPanel(project, project.picklesService())
        val content = ContentFactory.getInstance().createContent(panel, "Board", false)
        toolWindow.contentManager.addContent(content)
    }
}
