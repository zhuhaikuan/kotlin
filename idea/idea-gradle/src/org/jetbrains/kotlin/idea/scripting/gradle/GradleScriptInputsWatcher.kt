/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.components.*
import com.intellij.openapi.externalSystem.service.project.autoimport.AsyncFileChangeListenerBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import org.jetbrains.annotations.TestOnly

@State(
    name = "KotlinBuildScriptsModificationInfo",
    storages = [Storage(StoragePathMacros.CACHE_FILE)]
)
class GradleScriptInputsWatcher(val project: Project) : PersistentStateComponent<GradleScriptInputsWatcher.Storage> {
    private var storage = Storage()

    fun startWatching() {
        VirtualFileManager.getInstance().addAsyncFileListener(
            object : AsyncFileChangeListenerBase() {
                override fun isRelevant(path: String): Boolean {
                    val files = getAffectedGradleProjectFiles(project)
                    return isInAffectedGradleProjectFiles(files, path)
                }

                override fun updateFile(file: VirtualFile, event: VFileEvent) {
                    storage.fileChanged(event.path, file.timeStamp)
                }

                // do nothing
                override fun prepareFileDeletion(file: VirtualFile) {}
                override fun apply() {}
                override fun reset() {}

            },
            project,
        )
    }

    fun areRelatedFilesUpToDate(file: VirtualFile, timeStamp: Long): Boolean {
        return storage.lastModifiedTimeStampExcept(file.path) < timeStamp
    }

    class Storage {
        private val lastModifiedFiles = LastModifiedFiles()

        fun lastModifiedTimeStampExcept(filePath: String): Long {
            return lastModifiedFiles.lastModifiedTimeStampExcept(filePath)
        }

        fun fileChanged(filePath: String, ts: Long) {
            lastModifiedFiles.fileChanged(ts, filePath)
        }
    }

    override fun getState(): Storage {
        return storage
    }

    override fun loadState(state: Storage) {
        this.storage = state
    }

    @TestOnly
    fun clearAndRefillState() {
        loadState(project.service<GradleScriptInputsWatcher>().state)
    }

    @TestOnly
    fun fileChanged(file: VirtualFile, ts: Long) {
        storage.fileChanged(file.path, ts)
    }
}