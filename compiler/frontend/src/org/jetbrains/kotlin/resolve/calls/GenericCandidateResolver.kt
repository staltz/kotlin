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

import com.google.common.collect.Maps
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.FunctionDescriptorUtil
import org.jetbrains.kotlin.resolve.calls.CallResolverUtil.getEffectiveExpectedType
import org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency.PARTLY_DEPENDENT
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.RECEIVER_POSITION
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.VALUE_PARAMETER_POSITION
import org.jetbrains.kotlin.resolve.calls.inference.createTypeForFunctionPlaceholder
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus.INCOMPLETE_TYPE_INFERENCE
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus.OTHER_ERROR
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.ErrorUtils.isFunctionPlaceholder
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.TypeUtils.DONT_CARE
import org.jetbrains.kotlin.types.TypeUtils.makeConstantSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.checker.JetTypeChecker

class GenericCandidateResolver(
        val argumentTypeResolver: ArgumentTypeResolver
) {
    fun <D : CallableDescriptor> inferTypeArguments(context: CallCandidateResolutionContext<D>): ResolutionStatus {
        val candidateCall = context.candidateCall
        val candidate = candidateCall.getCandidateDescriptor()

        val constraintSystem = ConstraintSystemImpl()

        // If the call is recursive, e.g.
        //   fun foo<T>(t : T) : T = foo(t)
        // we can't use same descriptor objects for T's as actual type values and same T's as unknowns,
        // because constraints become trivial (T :< T), and inference fails
        //
        // Thus, we replace the parameters of our descriptor with fresh objects (perform alpha-conversion)
        val candidateWithFreshVariables = FunctionDescriptorUtil.alphaConvertTypeParameters(candidate)

        val typeVariables = Maps.newLinkedHashMap<TypeParameterDescriptor, Variance>()
        val backConversion = Maps.newHashMap<TypeParameterDescriptor, TypeParameterDescriptor>()
        for (typeParameterDescriptor in candidateWithFreshVariables.getTypeParameters()) {
            typeVariables.put(typeParameterDescriptor, Variance.INVARIANT) // TODO: variance of the occurrences
            backConversion.put(typeParameterDescriptor, candidate.getTypeParameters().get(typeParameterDescriptor.getIndex()))
        }

        constraintSystem.registerTypeVariables(typeVariables)

        val substituteDontCare = makeConstantSubstitutor(candidateWithFreshVariables.getTypeParameters(), DONT_CARE)

        // Value parameters
        for (entry in candidateCall.getValueArguments().entrySet()) {
            val resolvedValueArgument = entry.getValue()
            val valueParameterDescriptor = candidateWithFreshVariables.getValueParameters().get(entry.getKey().getIndex())


            for (valueArgument in resolvedValueArgument.getArguments()) {
                // TODO : more attempts, with different expected types

                // Here we type check expecting an error type (DONT_CARE, substitution with substituteDontCare)
                // and throw the results away
                // We'll type check the arguments later, with the inferred types expected
                addConstraintForValueArgument(valueArgument, valueParameterDescriptor, substituteDontCare, constraintSystem,
                                              context.replaceResolveArgumentsMode(SHAPE_FUNCTION_ARGUMENTS))
            }
        }

        // Receiver
        // Error is already reported if something is missing
        val receiverArgument = candidateCall.getExtensionReceiver()
        val receiverParameter = candidateWithFreshVariables.getExtensionReceiverParameter()
        if (receiverArgument.exists() && receiverParameter != null) {
            var receiverType: JetType? = if (context.candidateCall.isSafeCall())
                TypeUtils.makeNotNullable(receiverArgument.getType())
            else
                receiverArgument.getType()
            if (receiverArgument is ExpressionReceiver) {
                receiverType = updateResultTypeForSmartCasts(receiverType, receiverArgument.getExpression(), context)
            }
            constraintSystem.addSubtypeConstraint(receiverType, receiverParameter.getType(), RECEIVER_POSITION.position())
        }

        // Restore type variables before alpha-conversion
        val constraintSystemWithRightTypeParameters = constraintSystem.substituteTypeVariables { backConversion.get(it) }
        candidateCall.setConstraintSystem(constraintSystemWithRightTypeParameters)

        // Solution
        val hasContradiction = constraintSystem.getStatus().hasContradiction()
        if (!hasContradiction) {
            return INCOMPLETE_TYPE_INFERENCE
        }
        return OTHER_ERROR
    }

    private fun addConstraintForValueArgument(
            valueArgument: ValueArgument,
            valueParameterDescriptor: ValueParameterDescriptor,
            substitutor: TypeSubstitutor,
            constraintSystem: ConstraintSystem,
            context: CallCandidateResolutionContext<*>
    ) {
        val effectiveExpectedType = getEffectiveExpectedType(valueParameterDescriptor, valueArgument)
        val argumentExpression = valueArgument.getArgumentExpression()

        val expectedType = substitutor.substitute(effectiveExpectedType, Variance.INVARIANT)
        val dataFlowInfoForArgument = context.candidateCall.getDataFlowInfoForArguments().getInfo(valueArgument)
        val newContext = context.replaceExpectedType(expectedType).replaceDataFlowInfo(dataFlowInfoForArgument)

        val typeInfoForCall = argumentTypeResolver.getArgumentTypeInfo(argumentExpression, newContext)
        context.candidateCall.getDataFlowInfoForArguments().updateInfo(valueArgument, typeInfoForCall.dataFlowInfo)

        val type = updateResultTypeForSmartCasts(typeInfoForCall.type, argumentExpression, context.replaceDataFlowInfo(dataFlowInfoForArgument))
        constraintSystem.addSubtypeConstraint(type, effectiveExpectedType, VALUE_PARAMETER_POSITION.position(valueParameterDescriptor.getIndex()))
    }

    private fun updateResultTypeForSmartCasts(
            type: JetType?,
            argumentExpression: JetExpression?,
            context: ResolutionContext<*>
    ): JetType? {
        val deparenthesizedArgument = ArgumentTypeResolver.getLastElementDeparenthesized(argumentExpression, context)
        if (deparenthesizedArgument == null || type == null) return type

        val dataFlowValue = DataFlowValueFactory.createDataFlowValue(deparenthesizedArgument, type, context)
        if (!dataFlowValue.isPredictable()) return type

        val possibleTypes = context.dataFlowInfo.getPossibleTypes(dataFlowValue)
        if (possibleTypes.isEmpty()) return type

        return TypeUtils.intersect(JetTypeChecker.DEFAULT, possibleTypes)
    }

    public fun <D : CallableDescriptor> completeTypeInferenceDependentOnFunctionLiteralsForCall(
            context: CallCandidateResolutionContext<D>,
            inCompleter: Boolean
    ) {
        val resolvedCall = context.candidateCall
        val constraintSystem = resolvedCall.getConstraintSystem() ?: return

        // constraints for function literals
        for ((valueParameterDescriptor, resolvedValueArgument) in resolvedCall.getValueArguments()) {

            for (valueArgument in resolvedValueArgument.getArguments()) {
                val argumentExpression = valueArgument.getArgumentExpression()
                if (argumentExpression != null && ArgumentTypeResolver.isFunctionLiteralArgument(argumentExpression, context)) {
                    addConstraintForFunctionLiteral(valueArgument, valueParameterDescriptor, context, inCompleter)
                }
            }
        }
        resolvedCall.setResultingSubstitutor(constraintSystem.getResultingSubstitutor())
    }

    private fun <D : CallableDescriptor> addConstraintForFunctionLiteral(
            valueArgument: ValueArgument,
            valueParameterDescriptor: ValueParameterDescriptor,
            context: CallCandidateResolutionContext<D>,
            inCompleter: Boolean
    ) {
        val argumentExpression = valueArgument.getArgumentExpression() ?: return
        val functionLiteral = ArgumentTypeResolver.getFunctionLiteralArgument(argumentExpression, context)

        val constraintSystem = context.candidateCall.getConstraintSystem()!!

        val effectiveExpectedType = getEffectiveExpectedType(valueParameterDescriptor.getOriginal(), valueArgument)
        var expectedType = constraintSystem.getCurrentSubstitutor().substitute(effectiveExpectedType, Variance.INVARIANT)
        if (expectedType == null || TypeUtils.isDontCarePlaceholder(expectedType)
            || !KotlinBuiltIns.isFunctionOrExtensionFunctionType(expectedType)) {
            val shape = argumentTypeResolver.getShapeTypeOfFunctionLiteral(functionLiteral, context.scope, context.trace)
            if (!inCompleter && isFunctionPlaceholder(shape)) return

            expectedType = createTypeForFunctionPlaceholder(shape, KotlinBuiltIns.getInstance().getAnyType())
        }
        if (!inCompleter && CallResolverUtil.hasUnknownFunctionParameter(expectedType)) return

        if (context.candidateCall.isProcessed(valueArgument)) return

        val dataFlowInfoForArguments = context.candidateCall.getDataFlowInfoForArguments()
        val dataFlowInfoForArgument = dataFlowInfoForArguments.getInfo(valueArgument)

        val expectedReturnType = KotlinBuiltIns.getReturnTypeFromFunctionType(expectedType)
        val hasUnitReturnType = KotlinBuiltIns.isUnit(expectedReturnType)
        // Unit is not replaced by DONT_CARE to be able to do COERCION_TO_UNIT
        val expectedTypeWithNoOrUnitReturnType = if (hasUnitReturnType) expectedType else CallResolverUtil.replaceReturnTypeBy(expectedType, DONT_CARE)
        val newContext = context.replaceExpectedType(expectedTypeWithNoOrUnitReturnType)
                .replaceDataFlowInfo(dataFlowInfoForArgument).replaceContextDependency(if (inCompleter) INDEPENDENT else PARTLY_DEPENDENT)
        val type = argumentTypeResolver.getFunctionLiteralTypeInfo(argumentExpression, functionLiteral, newContext).type
        constraintSystem.addSubtypeConstraint(type, effectiveExpectedType, VALUE_PARAMETER_POSITION.position(valueParameterDescriptor.getIndex()))
        context.candidateCall.markAsProcessed(valueArgument);
    }
}