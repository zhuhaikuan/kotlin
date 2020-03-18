/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import org.jetbrains.plugins.gradle.model.ClassSetImportModelProvider
import org.jetbrains.plugins.gradle.model.ProjectImportModelProvider

class KotlinDslScriptModelResolver : KotlinDslScriptModelResolverCommon() {
    override fun requiresTaskRunning() = true

    override fun getModelProvider() = KotlinDslScriptModelProvider()

    override fun getProjectsLoadedModelProvider(): ProjectImportModelProvider? {
        return ClassSetImportModelProvider(
            emptySet(),
            setOf(KotlinDslScriptAdditionalTask::class.java)
        )
    }
}