package com.pickles.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.wm.ToolWindowManager

class PicklesStartupActivity : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        project.picklesService().startHttpServer()
        ApplicationManager.getApplication().invokeLater {
            ToolWindowManager.getInstance(project).getToolWindow("Pickles")?.show()
        }
    }
}
