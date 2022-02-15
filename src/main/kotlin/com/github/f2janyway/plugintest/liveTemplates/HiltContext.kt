package com.github.f2janyway.plugintest.liveTemplates

import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType

class HiltContext : TemplateContextType("PluginTest", "F2j plugins") {
    override fun isInContext(templateActionContext: TemplateActionContext): Boolean {
        return templateActionContext.file.name.endsWith(".kt")
    }
}