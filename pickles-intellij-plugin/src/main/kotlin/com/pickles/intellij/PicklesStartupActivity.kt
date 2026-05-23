package com.pickles.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindowManager

class PicklesStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.picklesService().startHttpServer()
        ApplicationManager.getApplication().invokeLater {
            ToolWindowManager.getInstance(project).getToolWindow("Pickles")?.show()
        }
    }
}
