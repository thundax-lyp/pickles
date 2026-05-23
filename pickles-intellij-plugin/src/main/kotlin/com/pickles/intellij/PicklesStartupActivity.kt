package com.pickles.intellij

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class PicklesStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.picklesService().startHttpServer()
    }
}
