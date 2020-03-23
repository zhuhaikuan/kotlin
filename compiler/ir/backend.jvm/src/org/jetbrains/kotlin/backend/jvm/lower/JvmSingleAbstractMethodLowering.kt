/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.SingleAbstractMethodLowering
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.load.java.JavaVisibilities

internal val singleAbstractMethodPhase = makeIrFilePhase(
    ::JvmSingleAbstractMethodLowering,
    name = "SingleAbstractMethod",
    description = "Replace SAM conversions with instances of interface-implementing classes"
)

private class JvmSingleAbstractMethodLowering(context: JvmBackendContext) : SingleAbstractMethodLowering(context) {
    private val jvmContext: JvmBackendContext get() = context as JvmBackendContext
    private val builtIns: IrBuiltIns get() = context.irBuiltIns

    private val functionAdapterClass = jvmContext.ir.symbols.functionAdapter.owner

    override val privateGeneratedWrapperVisibility: Visibility
        get() = JavaVisibilities.PACKAGE_VISIBILITY

    override fun getSuperTypeForWrapper(typeOperand: IrType): IrType =
        typeOperand.erasedUpperBound.defaultType

    private val IrType.isKotlinFunInterface: Boolean
        get() = getClass()?.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB

    override fun getAdditionalSupertypes(supertype: IrType): List<IrType> =
        if (supertype.isKotlinFunInterface) listOf(functionAdapterClass.typeWith())
        else emptyList()

    override fun generateAdditionalMembers(klass: IrClass, functionDelegateField: IrField) {
        val supertype = klass.superTypes.first()
        if (!supertype.isKotlinFunInterface) return

        val getFunctionDelegate = functionAdapterClass.functions.single { it.name.asString() == "getFunctionDelegate" }
        generateGetFunctionDelegate(klass, getFunctionDelegate, functionDelegateField)
        generateEquals(klass, supertype, functionDelegateField, getFunctionDelegate)
        generateHashCode(klass, functionDelegateField)
    }

    private fun generateGetFunctionDelegate(klass: IrClass, getFunctionDelegate: IrSimpleFunction, functionDelegateField: IrField) {
        klass.addFunction(getFunctionDelegate.name.asString(), getFunctionDelegate.returnType).apply {
            overriddenSymbols = listOf(getFunctionDelegate.symbol)
            body = jvmContext.createIrBuilder(symbol).run {
                irExprBody(irGetField(irGet(dispatchReceiverParameter!!), functionDelegateField))
            }
        }
    }

    private fun generateEquals(klass: IrClass, supertype: IrType, functionDelegateField: IrField, getFunctionDelegate: IrSimpleFunction) {
        klass.addFunction("equals", builtIns.booleanType).apply {
            overriddenSymbols = listOf(supertype.getClass()!!.functions.single {
                it.name.asString() == "equals" &&
                        it.extensionReceiverParameter == null &&
                        it.valueParameters.singleOrNull()?.type == builtIns.anyNType
            }.symbol)

            val other = addValueParameter("other", builtIns.anyNType)
            body = jvmContext.createIrBuilder(symbol).run {
                irExprBody(
                    irIfThenElse(
                        builtIns.booleanType,
                        irIs(irGet(other), supertype),
                        irIfThenElse(
                            builtIns.booleanType,
                            irIs(irGet(other), functionAdapterClass.typeWith()),
                            irEquals(
                                irGetField(irGet(dispatchReceiverParameter!!), functionDelegateField),
                                irCall(getFunctionDelegate).also {
                                    it.dispatchReceiver = irImplicitCast(irGet(other), functionAdapterClass.typeWith())
                                }
                            ),
                            irFalse()
                        ),
                        irFalse()
                    )
                )
            }
        }
    }

    private fun generateHashCode(klass: IrClass, functionDelegateField: IrField) {
        klass.addFunction("hashCode", builtIns.intType).apply {
            val hashCode = klass.superTypes.first().getClass()!!.functions.single {
                it.name.asString() == "hashCode" && it.extensionReceiverParameter == null && it.valueParameters.isEmpty()
            }.symbol
            overriddenSymbols = listOf(hashCode)
            body = jvmContext.createIrBuilder(symbol).run {
                irExprBody(
                    irCall(hashCode).also {
                        it.dispatchReceiver = irGetField(irGet(dispatchReceiverParameter!!), functionDelegateField)
                    }
                )
            }
        }
    }
}
