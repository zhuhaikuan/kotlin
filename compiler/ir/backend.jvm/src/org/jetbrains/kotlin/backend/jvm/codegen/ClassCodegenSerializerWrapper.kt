/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.createFreeFakeLambdaDescriptor
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.codegen.serialization.JvmSerializerExtension
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapperBase
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.serialization.FirElementSerializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.Type

sealed class ClassCodegenSerializerWrapper {

    abstract val parentWrapper: ClassCodegenSerializerWrapper?

    abstract fun classProto(metadata: MetadataSource.Class): ProtoBuf.Class.Builder

    abstract fun functionProto(metadata: MetadataSource.Function): ProtoBuf.Function.Builder?

    abstract fun packagePartProto(packageFqName: FqName, metadata: MetadataSource.File): ProtoBuf.Package.Builder

    abstract fun writeAnnotationData(visitor: AnnotationVisitor, messageLite: MessageLite)

    abstract fun serializeJvmPackage(proto: ProtoBuf.Package.Builder, partAsmType: Type)

    class Classic(
        val serializer: DescriptorSerializer,
        override val parentWrapper: Classic?
    ) : ClassCodegenSerializerWrapper() {
        override fun classProto(metadata: MetadataSource.Class): ProtoBuf.Class.Builder =
            serializer.classProto(metadata.descriptor)

        override fun functionProto(metadata: MetadataSource.Function): ProtoBuf.Function.Builder? =
            serializer.functionProto(createFreeFakeLambdaDescriptor(metadata.descriptor))

        override fun packagePartProto(packageFqName: FqName, metadata: MetadataSource.File): ProtoBuf.Package.Builder =
            serializer.packagePartProto(packageFqName, metadata.descriptors)

        override fun writeAnnotationData(visitor: AnnotationVisitor, messageLite: MessageLite) {
            AsmUtil.writeAnnotationData(visitor, serializer, messageLite)
        }

        override fun serializeJvmPackage(proto: ProtoBuf.Package.Builder, partAsmType: Type) {
            (serializer.extension as JvmSerializerExtension).serializeJvmPackage(proto, partAsmType)
        }
    }

    class FrontendIR(
        val serializer: FirElementSerializer,
        override val parentWrapper: FrontendIR?
    ) : ClassCodegenSerializerWrapper() {
        override fun classProto(metadata: MetadataSource.Class): ProtoBuf.Class.Builder =
            serializer.classProto((metadata as FirMetadataSource.Class).klass)

        override fun functionProto(metadata: MetadataSource.Function): ProtoBuf.Function.Builder? =
            serializer.functionProto((metadata as FirMetadataSource.Function).function)

        override fun packagePartProto(packageFqName: FqName, metadata: MetadataSource.File): ProtoBuf.Package.Builder {
            TODO("Not yet implemented")
        }

        override fun writeAnnotationData(visitor: AnnotationVisitor, messageLite: MessageLite) {

        }

        override fun serializeJvmPackage(proto: ProtoBuf.Package.Builder, partAsmType: Type) {
            TODO("Not yet implemented")
        }
    }

    companion object {
        fun create(
            bindings: JvmSerializationBindings,
            state: GenerationState,
            typeMapper: KotlinTypeMapperBase,
            irClass: IrClass,
            parentClassCodegen: ClassCodegen?,
        ): ClassCodegenSerializerWrapper? {
            val extension = JvmSerializerExtension(bindings, state, typeMapper)
            val classicSerializerWrapper = parentClassCodegen?.serializerWrapper as? Classic
            return when (val metadata = irClass.metadata) {
                is MetadataSource.Class -> Classic(
                    DescriptorSerializer.create(metadata.descriptor, extension, classicSerializerWrapper?.serializer),
                    classicSerializerWrapper
                )
                is MetadataSource.File -> Classic(
                    DescriptorSerializer.createTopLevel(extension),
                    classicSerializerWrapper
                )
                is MetadataSource.Function -> Classic(
                    DescriptorSerializer.createForLambda(extension),
                    classicSerializerWrapper
                )
                else -> null
            }
        }
    }
}