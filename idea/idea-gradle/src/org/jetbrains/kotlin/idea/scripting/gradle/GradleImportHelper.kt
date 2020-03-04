/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.gradle.util.GradleConstants

// for new idea run partial import
fun runGradleImport(project: Project) {
    val projectPath = project.basePath ?: return
    val projectSettings = ExternalSystemApiUtil
        .getSettings(project, GradleConstants.SYSTEM_ID)
        .getLinkedProjectSettings(projectPath) ?: return

    ExternalSystemUtil.refreshProject(
        projectSettings.externalProjectPath,
        ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
            .build()
    )
}

fun showNotificationToRunProjectImport(project: Project, callback: () -> Unit) {
    val notification = getNotificationGroup().createNotification(
        "Script configuration should be updated",
        NotificationType.INFORMATION
    )
    notification.addAction(NotificationAction.createSimple("Load script configurations...") {
        callback()
        notification.expire()
    })
    notification.notify(project)
}

private fun getNotificationGroup(): NotificationGroup {
    return NotificationGroup.findRegisteredGroup("Kotlin script configurations")
        ?: NotificationGroup("Kotlin script configurations", NotificationDisplayType.STICKY_BALLOON, true)
}