/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptsModel
import org.jetbrains.kotlin.idea.scripting.gradle.kotlinDslScriptsModelImportSupported
import org.jetbrains.plugins.gradle.service.project.ProjectModelBuilder
import org.jetbrains.plugins.gradle.service.project.ProjectModelContributor
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext
import org.jetbrains.plugins.gradle.service.project.ToolingModelsProvider

class KotlinDslScriptModelContributor : ProjectModelContributor {
    override fun accept(
        projectModelBuilder: ProjectModelBuilder,
        toolingModelsProvider: ToolingModelsProvider,
        resolverContext: ProjectResolverContext
    ) {
        if (!kotlinDslScriptsModelImportSupported(resolverContext.projectGradleVersion)) return

        toolingModelsProvider.projects().forEach {
            val projectIdentifier = it.projectIdentifier.projectPath
            if (projectIdentifier == ":") {
                val model = toolingModelsProvider.getProjectModel(it, KotlinDslScriptsModel::class.java)
                if (model != null) {
                    processScriptModel(model, projectIdentifier)
                }
            }
        }
    }
}