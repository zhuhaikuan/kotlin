/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.messageCollectorLogger
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.lower.moveBodilessDeclarationsToSeparatePlace
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.utils.JsMainFunctionDetector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.generateTypicalIrProviderList
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.library.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File

abstract class AbstractJsIrWithKlibTest : AbstractIrJsTextTestCase() {
    // TODO reuse
    private val fullRuntimeKlib = "libraries/stdlib/js-ir/build/fullRuntime/klib"
    private val kotlinTestKLib = "libraries/stdlib/js-ir/build/kotlin.test/klib"

    override fun doTest(wholeFile: File, testFiles: List<TestFile>) {
        setupEnvironment(testFiles)
        loadMultiFiles(testFiles)
        val irModule = generateIrModule()

        val testFilesNames = testFiles.map { "/${it.name}" }
        val irFiles = irModule.files.filter { testFilesNames.contains(it.fileEntry.name) }

        val expectedPath = wholeFile.absolutePath.replace(".kt", ".txt")
        KotlinTestUtils.assertEqualsToFile(File(expectedPath), irFiles.joinToString(separator = "\n") { it.dump() })
    }

    private fun setupEnvironment(testFiles: List<TestFile>) {
        val configuration = createConfiguration(
            ConfigurationKind.ALL, TestJdkKind.FULL_JDK, listOf(), listOfNotNull(writeJavaFiles(testFiles)), testFiles
        )
        //needs this to be able to compile code
//        configuration.put(CommonConfigurationKeys.MODULE_NAME, "<test-module>")
//        configuration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)

        myEnvironment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JS_CONFIG_FILES)
    }

    private fun generateIrModule(): IrModuleFragment {
        val runtimeKlibs = listOf(fullRuntimeKlib, kotlinTestKLib)
        val allKlibPaths = runtimeKlibs.map { File(it).absolutePath }
        val resolvedLibraries = jsResolveLibraries(allKlibPaths, messageCollectorLogger(MessageCollector.NONE))

        return createIr(myEnvironment.project, myFiles.psiFiles, myEnvironment.configuration, resolvedLibraries)
    }

    private fun createIr(
        project: Project, files: List<KtFile>, config: CompilerConfiguration, allDependencies: KotlinLibraryResolveResult
    ): IrModuleFragment {
        val mainModule = MainModule.SourceFiles(files)
        val (moduleFragment, dependencyModules, irBuiltIns, symbolTable, deserializer) =
            loadIr(project, mainModule, AnalyzerWithCompilerReport(config), config, allDependencies, emptyList())

        val moduleDescriptor = moduleFragment.descriptor
        val context = JsIrBackendContext(moduleDescriptor, irBuiltIns, symbolTable, moduleFragment, emptySet(), config)

        // Load declarations referenced during `context` initialization
        dependencyModules.forEach {
            val irProviders = generateTypicalIrProviderList(it.descriptor, irBuiltIns, symbolTable, deserializer)
            ExternalDependenciesGenerator(symbolTable, irProviders).generateUnboundSymbolsAsDependencies()
        }

        val irFiles = (dependencyModules + listOf(moduleFragment)).flatMap { it.files }

        moduleFragment.files.clear()
        moduleFragment.files += irFiles

        // Create stubs
        val irProvidersWithoutDeserializer = generateTypicalIrProviderList(moduleDescriptor, irBuiltIns, symbolTable)
        ExternalDependenciesGenerator(symbolTable, irProvidersWithoutDeserializer).generateUnboundSymbolsAsDependencies()
        moduleFragment.patchDeclarationParents()
        deserializer.finalizeExpectActualLinker()

        moveBodilessDeclarationsToSeparatePlace(context, moduleFragment)

        jsPhases.invokeToplevel(PhaseConfig(jsPhases), context, moduleFragment)

        /*val mainFunction = JsMainFunctionDetector.getMainFunctionOrNull(moduleFragment)
        val transformer = IrModuleToJsTransformer(context, mainFunction, null)
        transformer.generateModule(moduleFragment, true, false)*/

        return moduleFragment
    }
}