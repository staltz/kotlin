/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.codegen.AsmUtil.writeKotlinSyntheticClassAnnotation
import org.jetbrains.kotlin.codegen.context.PropertyReferenceContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.KotlinSyntheticClass
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetCallableReferenceExpression
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.*
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.scopes.receivers.ScriptReceiver
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_FINAL
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_SUPER
import org.jetbrains.org.objectweb.asm.Opcodes.V1_6
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

public class PropertyReferenceCodegen(
        state: GenerationState,
        parentCodegen: MemberCodegen<*>,
        context: PropertyReferenceContext,
        expression: JetCallableReferenceExpression,
        classBuilder: ClassBuilder,
        private val classDescriptor: ClassDescriptor,
        private val resolvedCall: ResolvedCall<VariableDescriptor>
) : MemberCodegen<JetCallableReferenceExpression>(state, parentCodegen, context, expression, classBuilder) {
    private val target = resolvedCall.getResultingDescriptor()
    private val asmType = typeMapper.mapClass(classDescriptor)
    private val superAsmType: Type

    init {
        val superClass = classDescriptor.getSuperClassNotAny().sure { "No super class for $classDescriptor" }
        superAsmType = typeMapper.mapClass(superClass)
    }

    override fun generateDeclaration() {
        v.defineClass(
                element,
                V1_6,
                ACC_FINAL or ACC_SUPER or AsmUtil.getVisibilityAccessFlagForAnonymous(classDescriptor), // TODO: test inline
                asmType.getInternalName(),
                null,
                superAsmType.getInternalName(),
                emptyArray()
        )

        v.visitSource(element.getContainingFile().getName(), null)
    }

    // TODO: ImplementationBodyCodegen.markLineNumberForSyntheticFunction?
    override fun generateBody() {
        // TODO: instance should be already wrapped by Reflection
        generateConstInstance(asmType)

        generateMethod("property reference init", 0, "<init>", "()V") {
            load(0, OBJECT_TYPE)
            invokespecial(superAsmType.getInternalName(), "<init>", "()V", false)
            areturn(Type.VOID_TYPE)
        }

        generateMethod("property reference getOwner", ACC_PUBLIC, "getOwner", Type.getMethodDescriptor(K_DECLARATION_CONTAINER_TYPE)) {
            ClosureCodegen.generateCallableReferenceDeclarationContainer(this, target, typeMapper)
            areturn(K_DECLARATION_CONTAINER_TYPE)
        }

        generateMethod("property reference getName", ACC_PUBLIC, "getName", Type.getMethodDescriptor(JAVA_STRING_TYPE)) {
            aconst(target.getName().asString())
            areturn(JAVA_STRING_TYPE)
        }

        generateMethod("property reference getSignature", ACC_PUBLIC, "getSignature", Type.getMethodDescriptor(JAVA_STRING_TYPE)) {
            target as PropertyDescriptor

            // TODO: ensure getter/setter are always created by front-end? this is more convenient for back-end
            val getter = target.getGetter() ?: run {
                val defaultGetter = DescriptorFactory.createDefaultGetter(target)
                defaultGetter.initialize(target.getType())
                defaultGetter
            }

            val method =
                    typeMapper.mapSignature(getter.sure { "No getter: $target" }).getAsmMethod()
            aconst(method.getName() + method.getDescriptor())
            areturn(JAVA_STRING_TYPE)
        }

        generateAccessors()
    }

    private fun generateAccessors() {
        val dispatchReceiver = resolvedCall.getDispatchReceiver()
        val extensionReceiver = resolvedCall.getExtensionReceiver()
        val receiverType =
                when {
                    dispatchReceiver is ScriptReceiver -> {
                        // TODO: fix receiver for scripts, see ScriptReceiver#getType
                        dispatchReceiver.getDeclarationDescriptor().getClassDescriptor().getDefaultType()
                    }
                    dispatchReceiver.exists() -> dispatchReceiver.getType()
                    extensionReceiver.exists() -> extensionReceiver.getType()
                    else -> null
                }

        fun generateAccessor(name: String, signature: String, accessorBody: InstructionAdapter.(StackValue) -> Unit) {
            generateMethod("property reference $name", ACC_PUBLIC, name, signature) {
                // Note: this descriptor is an inaccurate representation of the get/set method. In particular, it has incorrect
                // return type and value parameter types. However, it's created only to be able to use
                // ExpressionCodegen#intermediateValueForProperty, which is poorly coupled with everything else.
                val fakeDescriptor = SimpleFunctionDescriptorImpl.create(
                        classDescriptor, Annotations.EMPTY, Name.identifier(name), CallableMemberDescriptor.Kind.DECLARATION,
                        SourceElement.NO_SOURCE
                )
                fakeDescriptor.initialize(null, classDescriptor.getThisAsReceiverParameter(), emptyList(), emptyList(),
                                          classDescriptor.builtIns.getAnyType(), Modality.OPEN, Visibilities.PUBLIC)

                val codegen = ExpressionCodegen(
                        this, FrameMap(), OBJECT_TYPE, context.intoFunction(fakeDescriptor), state, this@PropertyReferenceCodegen
                )

                val receiver =
                        if (receiverType != null) StackValue.coercion(StackValue.local(1, OBJECT_TYPE), typeMapper.mapType(receiverType))
                        else StackValue.none()
                val value = codegen.intermediateValueForProperty(target as PropertyDescriptor, false, null, receiver)
                accessorBody(value)
            }
        }

        val getterSignature =
                if (receiverType != null) Type.getMethodDescriptor(OBJECT_TYPE, OBJECT_TYPE) else Type.getMethodDescriptor(OBJECT_TYPE)

        generateAccessor("get", getterSignature) { value ->
            value.put(OBJECT_TYPE, this)
            areturn(OBJECT_TYPE)
        }

        if (!target.isVar()) return

        val setterSignature =
                if (receiverType != null) Type.getMethodDescriptor(Type.VOID_TYPE, OBJECT_TYPE, OBJECT_TYPE)
                else Type.getMethodDescriptor(Type.VOID_TYPE, OBJECT_TYPE)

        generateAccessor("set", setterSignature) { value ->
            // Hard-coded 1 or 2 is safe here because there's only java/lang/Object in the signature, no double/long parameters
            value.store(StackValue.local(if (receiverType != null) 2 else 1, OBJECT_TYPE), this)
            areturn(Type.VOID_TYPE)
        }
    }

    private fun generateMethod(debugString: String, access: Int, name: String, desc: String, generate: InstructionAdapter.() -> Unit) {
        val mv = v.newMethod(JvmDeclarationOrigin.NO_ORIGIN, access, name, desc, null, null)

        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            val iv = InstructionAdapter(mv)
            iv.visitCode()
            iv.generate()
            FunctionCodegen.endVisit(mv, debugString, element)
        }
    }

    override fun generateKotlinAnnotation() {
        writeKotlinSyntheticClassAnnotation(v, KotlinSyntheticClass.Kind.CALLABLE_REFERENCE_WRAPPER)
    }

    public fun putInstanceOnStack(): StackValue {
        val hasReceiver = target.getDispatchReceiverParameter() != null || target.getExtensionReceiverParameter() != null

        val (propertyType, internalReferenceType, methodName) =
                when {
                    hasReceiver -> when {
                        target.isVar() -> Triple(K_MUTABLE_PROPERTY1_TYPE, MUTABLE_PROPERTY_REFERENCE1, "mutableProperty1")
                        else -> Triple(K_PROPERTY1_TYPE, PROPERTY_REFERENCE1, "property1")
                    }
                    else -> when {
                        target.isVar() -> Triple(K_MUTABLE_PROPERTY0_TYPE, MUTABLE_PROPERTY_REFERENCE0, "mutableProperty0")
                        else -> Triple(K_PROPERTY0_TYPE, PROPERTY_REFERENCE0, "property0")
                    }
                }

        return StackValue.operation(propertyType) { iv ->
            iv.getstatic(asmType.getInternalName(), JvmAbi.INSTANCE_FIELD, asmType.getDescriptor())

            iv.invokestatic(REFLECTION, methodName, Type.getMethodDescriptor(propertyType, internalReferenceType), false)
        }
    }
}
