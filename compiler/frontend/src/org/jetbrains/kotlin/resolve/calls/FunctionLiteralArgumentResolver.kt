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

package org.jetbrains.kotlin.resolve.calls

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.calls.CallResolverUtil.ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS
import org.jetbrains.kotlin.resolve.calls.CallResolverUtil.getEffectiveExpectedType
import org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency.DEPENDENT
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.VALUE_PARAMETER_POSITION
import org.jetbrains.kotlin.resolve.calls.inference.createCorrespondingFunctionTypeForFunctionPlaceholder
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.TypeUtils.DONT_CARE
import org.jetbrains.kotlin.types.Variance

class FunctionLiteralArgumentResolver(
        val argumentTypeResolver: ArgumentTypeResolver
) {

    public fun <D : CallableDescriptor> completeTypeInferenceDependentOnFunctionLiteralsForCall(
            context: CallCandidateResolutionContext<D>
    ) {
        val resolvedCall = context.candidateCall
        val constraintSystem = resolvedCall.getConstraintSystem() ?: return

        // constraints for function literals
        for (entry in resolvedCall.getValueArguments().entrySet()) {
            val resolvedValueArgument = entry.getValue()
            val valueParameterDescriptor = entry.getKey()

            for (valueArgument in resolvedValueArgument.getArguments()) {
                addConstraintForFunctionLiteral(valueArgument, valueParameterDescriptor, constraintSystem, context)
            }
        }
        resolvedCall.setResultingSubstitutor(constraintSystem.getResultingSubstitutor())
    }

    private fun <D : CallableDescriptor> addConstraintForFunctionLiteral(
            valueArgument: ValueArgument,
            valueParameterDescriptor: ValueParameterDescriptor,
            constraintSystem: ConstraintSystem,
            context: CallCandidateResolutionContext<D>
    ) {
        val argumentExpression = valueArgument.getArgumentExpression() ?: return
        if (!ArgumentTypeResolver.isFunctionLiteralArgument(argumentExpression, context)) return

        val functionLiteral = ArgumentTypeResolver.getFunctionLiteralArgument(argumentExpression, context)

        val effectiveExpectedType = getEffectiveExpectedType(valueParameterDescriptor, valueArgument)
        var expectedType = constraintSystem.getCurrentSubstitutor().substitute(effectiveExpectedType, Variance.INVARIANT)
        if (expectedType == null || TypeUtils.isDontCarePlaceholder(expectedType)
                || !KotlinBuiltIns.isFunctionOrExtensionFunctionType(expectedType)) {
            val shape = argumentTypeResolver.getShapeTypeOfFunctionLiteral(functionLiteral, context.scope, context.trace)
            //todo it won't work for extension function literals!
            expectedType = createCorrespondingFunctionTypeForFunctionPlaceholder(shape, KotlinBuiltIns.getInstance().getAnyType())
        }
        if (CallResolverUtil.hasUnknownFunctionParameter(expectedType)) return

        val dataFlowInfoForArguments = context.candidateCall.getDataFlowInfoForArguments()
        val dataFlowInfoForArgument = dataFlowInfoForArguments.getInfo(valueArgument)

        val expectedReturnType = KotlinBuiltIns.getReturnTypeFromFunctionType(expectedType)
        val hasUnitReturnType = KotlinBuiltIns.isUnit(expectedReturnType)
        // Unit is not replaced by DONT_CARE to be able to do COERCION_TO_UNIT
        val expectedTypeWithNoOrUnitReturnType = if (hasUnitReturnType) expectedType else CallResolverUtil.replaceReturnTypeBy(expectedType, DONT_CARE)
        val newContext = context.replaceExpectedType(expectedTypeWithNoOrUnitReturnType)
                .replaceDataFlowInfo(dataFlowInfoForArgument).replaceContextDependency(DEPENDENT)
        val type = argumentTypeResolver.getFunctionLiteralTypeInfo(argumentExpression, functionLiteral, newContext, RESOLVE_FUNCTION_ARGUMENTS).type
        constraintSystem.addSubtypeConstraint(type, effectiveExpectedType, VALUE_PARAMETER_POSITION.position(valueParameterDescriptor.getIndex()))
    }
}