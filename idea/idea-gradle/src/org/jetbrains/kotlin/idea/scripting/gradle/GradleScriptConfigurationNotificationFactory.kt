/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.notification.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationNotificationFactory
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.psi.UserDataProperty

class GradleScriptConfigurationNotificationFactory : ScriptConfigurationNotificationFactory {
    override fun showNotification(file: VirtualFile, project: Project, onClick: () -> Unit): Boolean {
        if (isGradleKotlinScript(file) && isGradleImportCanBeUsed(project)) {
            showNotificationForProjectImport(project, onClick)
            return true
        }
        return false
    }

    override fun hideNotification(file: VirtualFile, project: Project): Boolean {
        return hideNotificationForProjectImport(project)
    }

    private fun isGradleImportCanBeUsed(project: Project): Boolean {
        val gradleVersion = getGradleVersion(project)
        if (gradleVersion != null && kotlinDslScriptsModelImportSupported(gradleVersion)) {
            return true
        }
        return false
    }

    private var Project.notificationPanel: Notification?
            by UserDataProperty<Project, Notification>(Key.create("load.script.configuration.panel"))


    fun showNotificationForProjectImport(project: Project, callback: () -> Unit) {
        if (project.notificationPanel != null) return

        val notification = getNotificationGroup().createNotification(
            "Script configurations may be changed. Gradle Project Import needs to be run to load changes",
            NotificationType.INFORMATION
        )
        notification.addAction(NotificationAction.createSimple("Import changes") {
            callback()
        })
        notification.addAction(NotificationAction.createSimple("Enable auto-reload") {
            callback()
            KotlinScriptingSettings.getInstance(project).isAutoReloadEnabled = true
        })
        project.notificationPanel = notification
        notification.notify(project)
    }

    fun hideNotificationForProjectImport(project: Project): Boolean {
        if (project.notificationPanel == null) return false
        project.notificationPanel?.expire()
        return true
    }

    private fun getNotificationGroup(): NotificationGroup {
        return NotificationGroup.findRegisteredGroup("Kotlin DSL script configurations")
            ?: NotificationGroup("Kotlin DSL script configurations", NotificationDisplayType.STICKY_BALLOON, true)
    }
}