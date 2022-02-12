package com.github.f2janyway.plugintest.services

import com.intellij.openapi.project.Project
import com.github.f2janyway.plugintest.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
