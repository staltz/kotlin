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

import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.callUtil.*
import org.jetbrains.kotlin.resolve.calls.context.*
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastUtils
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionTask
import org.jetbrains.kotlin.resolve.calls.tasks.*
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.types.expressions.JetTypeInfo

import javax.inject.Inject
import java.util.*

import org.jetbrains.kotlin.diagnostics.Errors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT
import org.jetbrains.kotlin.diagnostics.Errors.SUPER_CANT_BE_EXTENSION_RECEIVER
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver.getLastElementDeparenthesized
import org.jetbrains.kotlin.resolve.calls.CallResolverUtil.ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS
import org.jetbrains.kotlin.resolve.calls.CallResolverUtil.ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS
import org.jetbrains.kotlin.resolve.calls.CallTransformer.CallForImplicitInvoke
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.RECEIVER_POSITION
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.VALUE_PARAMETER_POSITION
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus.*
import org.jetbrains.kotlin.types.TypeUtils.*

public class CandidateResolver(val argumentTypeResolver: ArgumentTypeResolver) {

    public fun <D : CallableDescriptor, F : D> performResolutionForCandidateCall(context: CallCandidateResolutionContext<D>, task: ResolutionTask<D, F>) {

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val candidateCall = context.candidateCall
        val candidate = candidateCall.getCandidateDescriptor()

        candidateCall.addStatus(checkReceiverTypeError(context))

        if (ErrorUtils.isError(candidate)) {
            candidateCall.addStatus(SUCCESS)
            return
        }

        if (!checkOuterClassMemberIsAccessible(context)) {
            candidateCall.addStatus(OTHER_ERROR)
            return
        }


        val receiverValue = ExpressionTypingUtils.normalizeReceiverValueForVisibility(candidateCall.getDispatchReceiver(), context.trace.getBindingContext())
        val invisibleMember = Visibilities.findInvisibleMember(receiverValue, candidate, context.scope.getContainingDeclaration())
        if (invisibleMember != null) {
            candidateCall.addStatus(OTHER_ERROR)
            context.tracing.invisibleMember(context.trace, invisibleMember)
        }

        if (task.checkArguments === CheckValueArgumentsMode.ENABLED) {
            val argumentMappingStatus = ValueArgumentsToParametersMapper.mapValueArgumentsToParameters(context.call, context.tracing, candidateCall, Sets.newLinkedHashSet<ValueArgument>())
            if (!argumentMappingStatus.isSuccess()) {
                candidateCall.addStatus(OTHER_ERROR)
            }
        }

        checkExtensionReceiver(context)

        if (!checkDispatchReceiver(context)) {
            candidateCall.addStatus(OTHER_ERROR)
        }

        val jetTypeArguments = context.call.getTypeArguments()
        if (!jetTypeArguments.isEmpty()) {
            // Explicit type arguments passed

            val typeArguments = ArrayList<JetType>()
            for (projection in jetTypeArguments) {
                if (projection.getProjectionKind() !== JetProjectionKind.NONE) {
                    context.trace.report(PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(projection))
                    ModifiersChecker.checkIncompatibleVarianceModifiers(projection.getModifierList(), context.trace)
                }
                val type = argumentTypeResolver.resolveTypeRefWithDefault(projection.getTypeReference(), context.scope, context.trace, ErrorUtils.createErrorType("Star projection in a call"))
                ForceResolveUtil.forceResolveAllContents(type)
                typeArguments.add(type)
            }
            val expectedTypeArgumentCount = candidate.getTypeParameters().size()
            for (index in jetTypeArguments.size()..expectedTypeArgumentCount - 1) {
                typeArguments.add(ErrorUtils.createErrorType("Explicit type argument expected for " + candidate.getTypeParameters().get(index).getName()))
            }
            val substitutionContext = FunctionDescriptorUtil.createSubstitutionContext(candidate as FunctionDescriptor, typeArguments)
            val substitutor = TypeSubstitutor.create(substitutionContext)

            if (expectedTypeArgumentCount != jetTypeArguments.size()) {
                candidateCall.addStatus(OTHER_ERROR)
                context.tracing.wrongNumberOfTypeArguments(context.trace, expectedTypeArgumentCount)
            }
            else {
                checkGenericBoundsInAFunctionCall(jetTypeArguments, typeArguments, candidate, substitutor, context.trace)
            }

            candidateCall.setResultingSubstitutor(substitutor)
        }
        else if (candidateCall.getKnownTypeParametersSubstitutor() != null) {
            candidateCall.setResultingSubstitutor(candidateCall.getKnownTypeParametersSubstitutor())
        }

        if (jetTypeArguments.isEmpty() && !candidate.getTypeParameters().isEmpty() && candidateCall.getKnownTypeParametersSubstitutor() == null) {
            candidateCall.addStatus(inferTypeArguments(context))
        }
        else {
            candidateCall.addStatus(checkAllValueArguments(context, SHAPE_FUNCTION_ARGUMENTS).status)
        }

        checkAbstractAndSuper(context)

        checkNonExtensionCalledWithReceiver(context)
    }

    private fun <D : CallableDescriptor> checkExtensionReceiver(context: CallCandidateResolutionContext<D>) {
        val candidateCall = context.candidateCall
        val receiverParameter = candidateCall.getCandidateDescriptor().getExtensionReceiverParameter()
        val receiverArgument = candidateCall.getExtensionReceiver()
        if (receiverParameter != null && !receiverArgument.exists()) {
            context.tracing.missingReceiver(candidateCall.getTrace(), receiverParameter)
            candidateCall.addStatus(OTHER_ERROR)
        }
        if (receiverParameter == null && receiverArgument.exists()) {
            context.tracing.noReceiverAllowed(candidateCall.getTrace())
            if (context.call.getCalleeExpression() is JetSimpleNameExpression) {
                candidateCall.addStatus(RECEIVER_PRESENCE_ERROR)
            }
            else {
                candidateCall.addStatus(OTHER_ERROR)
            }
        }
    }

    private fun checkDispatchReceiver(context: CallCandidateResolutionContext<*>): Boolean {
        val candidateCall = context.candidateCall
        val candidateDescriptor = candidateCall.getCandidateDescriptor()
        val dispatchReceiver = candidateCall.getDispatchReceiver()
        if (dispatchReceiver.exists()) {
            var nestedClass: ClassDescriptor? = null
            if (candidateDescriptor is ConstructorDescriptor && DescriptorUtils.isStaticNestedClass(candidateDescriptor.getContainingDeclaration())) {
                nestedClass = candidateDescriptor.getContainingDeclaration()
            }
            else if (candidateDescriptor is FakeCallableDescriptorForObject) {
                nestedClass = candidateDescriptor.getReferencedDescriptor()
            }
            if (nestedClass != null) {
                context.tracing.nestedClassAccessViaInstanceReference(context.trace, nestedClass, candidateCall.getExplicitReceiverKind())
                return false
            }
        }

        assert((dispatchReceiver.exists() == (candidateCall.getResultingDescriptor().getDispatchReceiverParameter() != null))) { "Shouldn't happen because of TaskPrioritizer: $candidateDescriptor" }

        return true
    }

    private fun checkOuterClassMemberIsAccessible(context: CallCandidateResolutionContext<*>): Boolean {
        // In "this@Outer.foo()" the error will be reported on "this@Outer" instead
        if (context.call.getExplicitReceiver().exists() || context.call.getDispatchReceiver().exists()) return true

        val candidateThis = getDeclaringClass(context.candidateCall.getCandidateDescriptor())
        if (candidateThis == null || candidateThis.getKind().isSingleton()) return true

        return DescriptorResolver.checkHasOuterClassInstance(context.scope, context.trace, context.call.getCallElement(), candidateThis)
    }

    private fun <D : CallableDescriptor> checkAbstractAndSuper(context: CallCandidateResolutionContext<D>) {
        val candidateCall = context.candidateCall
        val descriptor = candidateCall.getCandidateDescriptor()
        val expression = context.candidateCall.getCall().getCalleeExpression()

        if (expression is JetSimpleNameExpression) {
            // 'B' in 'class A: B()' is JetConstructorCalleeExpression
            if (descriptor is ConstructorDescriptor) {
                val modality = descriptor.getContainingDeclaration().getModality()
                if (modality === Modality.ABSTRACT) {
                    context.tracing.instantiationOfAbstractClass(context.trace)
                }
            }
        }

        val superDispatchReceiver = getReceiverSuper(candidateCall.getDispatchReceiver())
        if (superDispatchReceiver != null) {
            if (descriptor is MemberDescriptor && descriptor.getModality() === Modality.ABSTRACT) {
                context.tracing.abstractSuperCall(context.trace)
                candidateCall.addStatus(OTHER_ERROR)
            }
        }

        // 'super' cannot be passed as an argument, for receiver arguments expression typer does not track this
        // See TaskPrioritizer for more
        val superExtensionReceiver = getReceiverSuper(candidateCall.getExtensionReceiver())
        if (superExtensionReceiver != null) {
            context.trace.report(SUPER_CANT_BE_EXTENSION_RECEIVER.on(superExtensionReceiver, superExtensionReceiver.getText()))
            candidateCall.addStatus(OTHER_ERROR)
        }
    }

    private fun checkNonExtensionCalledWithReceiver(context: CallCandidateResolutionContext<*>) {
        val candidateCall = context.candidateCall

        if (isSynthesizedInvoke(candidateCall.getCandidateDescriptor()) && !KotlinBuiltIns.isExtensionFunctionType(candidateCall.getDispatchReceiver().getType())) {
            context.tracing.freeFunctionCalledAsExtension(context.trace)
            candidateCall.addStatus(OTHER_ERROR)
        }
    }

    private fun getReceiverSuper(receiver: ReceiverValue): JetSuperExpression? {
        if (receiver is ExpressionReceiver) {
            val expression = receiver.getExpression()
            if (expression is JetSuperExpression) {
                return expression
            }
        }
        return null
    }

    private fun getDeclaringClass(candidate: CallableDescriptor): ClassDescriptor? {
        val expectedThis = candidate.getDispatchReceiverParameter() ?: return null
        val descriptor = expectedThis.getContainingDeclaration()
        return if (descriptor is ClassDescriptor) descriptor else null
    }

    public fun <D : CallableDescriptor> completeTypeInferenceDependentOnFunctionLiteralsForCall(context: CallCandidateResolutionContext<D>) {
        val resolvedCall = context.candidateCall
        val constraintSystem = resolvedCall.getConstraintSystem() ?: return

        // constraints for function literals
        // Value parameters
        for (entry in resolvedCall.getValueArguments().entrySet()) {
            val resolvedValueArgument = entry.getValue()
            val valueParameterDescriptor = entry.getKey()

            for (valueArgument in resolvedValueArgument.getArguments()) {
                addConstraintForFunctionLiteral<D>(valueArgument, valueParameterDescriptor, constraintSystem, context)
            }
        }
        resolvedCall.setResultingSubstitutor(constraintSystem.getResultingSubstitutor())
    }

    private fun <D : CallableDescriptor> addConstraintForFunctionLiteral(valueArgument: ValueArgument, valueParameterDescriptor: ValueParameterDescriptor, constraintSystem: ConstraintSystem, context: CallCandidateResolutionContext<D>) {
        val argumentExpression = valueArgument.getArgumentExpression() ?: return
        if (!ArgumentTypeResolver.isFunctionLiteralArgument(argumentExpression, context)) return

        val functionLiteral = ArgumentTypeResolver.getFunctionLiteralArgument(argumentExpression, context)

        val effectiveExpectedType = getEffectiveExpectedType(valueParameterDescriptor, valueArgument)
        var expectedType = constraintSystem.getCurrentSubstitutor().substitute(effectiveExpectedType, Variance.INVARIANT)
        if (expectedType == null || TypeUtils.isDontCarePlaceholder(expectedType)) {
            expectedType = argumentTypeResolver.getShapeTypeOfFunctionLiteral(functionLiteral, context.scope, context.trace, false)
        }
        if (expectedType == null || !KotlinBuiltIns.isFunctionOrExtensionFunctionType(expectedType) || CallResolverUtil.hasUnknownFunctionParameter(expectedType)) {
            return
        }
        val dataFlowInfoForArguments = context.candidateCall.getDataFlowInfoForArguments()
        val dataFlowInfoForArgument = dataFlowInfoForArguments.getInfo(valueArgument)

        //todo analyze function literal body once in 'dependent' mode, then complete it with respect to expected type
        val hasExpectedReturnType = !CallResolverUtil.hasUnknownReturnType(expectedType)
        if (hasExpectedReturnType) {
            val temporaryToResolveFunctionLiteral = TemporaryTraceAndCache.create(context, "trace to resolve function literal with expected return type", argumentExpression)

            val statementExpression = JetPsiUtil.getExpressionOrLastStatementInBlock(functionLiteral.getBodyExpression())
            if (statementExpression == null) return
            val mismatch = BooleanArray(1)
            val errorInterceptingTrace = ExpressionTypingUtils.makeTraceInterceptingTypeMismatch(temporaryToResolveFunctionLiteral.trace, statementExpression, mismatch)
            val newContext = context.replaceBindingTrace(errorInterceptingTrace).replaceExpectedType(expectedType).replaceDataFlowInfo(dataFlowInfoForArgument).replaceResolutionResultsCache(temporaryToResolveFunctionLiteral.cache).replaceContextDependency(INDEPENDENT)
            val type = argumentTypeResolver.getFunctionLiteralTypeInfo(argumentExpression, functionLiteral, newContext, RESOLVE_FUNCTION_ARGUMENTS).type
            if (!mismatch[0]) {
                constraintSystem.addSubtypeConstraint(type, effectiveExpectedType, VALUE_PARAMETER_POSITION.position(valueParameterDescriptor.getIndex()))
                temporaryToResolveFunctionLiteral.commit()
                return
            }
        }
        val expectedTypeWithoutReturnType = if (hasExpectedReturnType) CallResolverUtil.replaceReturnTypeByUnknown(expectedType) else expectedType
        val newContext = context.replaceExpectedType(expectedTypeWithoutReturnType).replaceDataFlowInfo(dataFlowInfoForArgument).replaceContextDependency(INDEPENDENT)
        val type = argumentTypeResolver.getFunctionLiteralTypeInfo(argumentExpression, functionLiteral, newContext, RESOLVE_FUNCTION_ARGUMENTS).type
        constraintSystem.addSubtypeConstraint(type, effectiveExpectedType, VALUE_PARAMETER_POSITION.position(valueParameterDescriptor.getIndex()))
    }

    private fun <D : CallableDescriptor> inferTypeArguments(context: CallCandidateResolutionContext<D>): ResolutionStatus {
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
        for (typeParameterDescriptor in candidateWithFreshVariables.getTypeParameters()) {
            typeVariables.put(typeParameterDescriptor, Variance.INVARIANT) // TODO: variance of the occurrences
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
                addConstraintForValueArgument(valueArgument, valueParameterDescriptor, substituteDontCare, constraintSystem, context, SHAPE_FUNCTION_ARGUMENTS)
            }
        }

        // Receiver
        // Error is already reported if something is missing
        val receiverArgument = candidateCall.getExtensionReceiver()
        val receiverParameter = candidateWithFreshVariables.getExtensionReceiverParameter()
        if (receiverArgument.exists() && receiverParameter != null) {
            var receiverType = if (context.candidateCall.isSafeCall())
                TypeUtils.makeNotNullable(receiverArgument.getType())
            else
                receiverArgument.getType()
            if (receiverArgument is ExpressionReceiver) {
                receiverType = updateResultTypeForSmartCasts(receiverType, receiverArgument.getExpression(), context)
            }
            constraintSystem.addSubtypeConstraint(receiverType, receiverParameter.getType(), RECEIVER_POSITION.position())
        }

        // Restore type variables before alpha-conversion
        val constraintSystemWithRightTypeParameters = constraintSystem.substituteTypeVariables(object : Function1<TypeParameterDescriptor, TypeParameterDescriptor> {
            override fun invoke(typeParameterDescriptor: TypeParameterDescriptor): TypeParameterDescriptor {
                return candidate.getTypeParameters().get(typeParameterDescriptor.getIndex())
            }
        })
        candidateCall.setConstraintSystem(constraintSystemWithRightTypeParameters)


        // Solution
        val hasContradiction = constraintSystem.getStatus().hasContradiction()
        if (!hasContradiction) {
            return INCOMPLETE_TYPE_INFERENCE
        }
        return OTHER_ERROR
    }

    private fun addConstraintForValueArgument(valueArgument: ValueArgument, valueParameterDescriptor: ValueParameterDescriptor, substitutor: TypeSubstitutor, constraintSystem: ConstraintSystem, context: CallCandidateResolutionContext<*>, resolveFunctionArgumentBodies: CallResolverUtil.ResolveArgumentsMode) {

        val effectiveExpectedType = getEffectiveExpectedType(valueParameterDescriptor, valueArgument)
        val argumentExpression = valueArgument.getArgumentExpression()

        val expectedType = substitutor.substitute(effectiveExpectedType, Variance.INVARIANT)
        val dataFlowInfoForArgument = context.candidateCall.getDataFlowInfoForArguments().getInfo(valueArgument)
        val newContext = context.replaceExpectedType(expectedType).replaceDataFlowInfo(dataFlowInfoForArgument)

        val typeInfoForCall = argumentTypeResolver.getArgumentTypeInfo(argumentExpression, newContext, resolveFunctionArgumentBodies)
        context.candidateCall.getDataFlowInfoForArguments().updateInfo(valueArgument, typeInfoForCall.dataFlowInfo)

        val type = updateResultTypeForSmartCasts(typeInfoForCall.type, argumentExpression, context.replaceDataFlowInfo(dataFlowInfoForArgument))
        constraintSystem.addSubtypeConstraint(type, effectiveExpectedType, VALUE_PARAMETER_POSITION.position(valueParameterDescriptor.getIndex()))
    }

    private fun updateResultTypeForSmartCasts(type: JetType?, argumentExpression: JetExpression?, context: ResolutionContext<*>): JetType? {
        val deparenthesizedArgument = getLastElementDeparenthesized(argumentExpression, context)
        if (deparenthesizedArgument == null || type == null) return type

        val dataFlowValue = DataFlowValueFactory.createDataFlowValue(deparenthesizedArgument, type, context)
        if (!dataFlowValue.isPredictable()) return type

        val possibleTypes = context.dataFlowInfo.getPossibleTypes(dataFlowValue)
        if (possibleTypes.isEmpty()) return type

        return TypeUtils.intersect(JetTypeChecker.DEFAULT, possibleTypes)
    }

    private fun <D : CallableDescriptor> checkAllValueArguments(context: CallCandidateResolutionContext<D>, resolveFunctionArgumentBodies: CallResolverUtil.ResolveArgumentsMode): ValueArgumentsCheckingResult {
        return checkAllValueArguments(context, context.candidateCall.getTrace(), resolveFunctionArgumentBodies)
    }

    public fun <D : CallableDescriptor> checkAllValueArguments(context: CallCandidateResolutionContext<D>, trace: BindingTrace, resolveFunctionArgumentBodies: CallResolverUtil.ResolveArgumentsMode): ValueArgumentsCheckingResult {
        val checkingResult = checkValueArgumentTypes(context, context.candidateCall, trace, resolveFunctionArgumentBodies)
        var resultStatus = checkingResult.status
        resultStatus = resultStatus.combine(checkReceivers(context, trace))

        return ValueArgumentsCheckingResult(resultStatus, checkingResult.argumentTypes)
    }

    private fun <D : CallableDescriptor> checkReceivers(context: CallCandidateResolutionContext<D>, trace: BindingTrace): ResolutionStatus {
        var resultStatus = SUCCESS
        val candidateCall = context.candidateCall

        resultStatus = resultStatus.combine(checkReceiverTypeError(context))

        // Comment about a very special case.
        // Call 'b.foo(1)' where class 'Foo' has an extension member 'fun B.invoke(Int)' should be checked two times for safe call (in 'checkReceiver'), because
        // both 'b' (receiver) and 'foo' (this object) might be nullable. In the first case we mark dot, in the second 'foo'.
        // Class 'CallForImplicitInvoke' helps up to recognise this case, and parameter 'implicitInvokeCheck' helps us to distinguish whether we check receiver or this object.

        resultStatus = resultStatus.combine(checkReceiver(context, candidateCall, trace, candidateCall.getResultingDescriptor().getExtensionReceiverParameter(), candidateCall.getExtensionReceiver(), candidateCall.getExplicitReceiverKind().isExtensionReceiver(), false))

        resultStatus = resultStatus.combine(checkReceiver(context, candidateCall, trace, candidateCall.getResultingDescriptor().getDispatchReceiverParameter(), candidateCall.getDispatchReceiver(), candidateCall.getExplicitReceiverKind().isDispatchReceiver(), // for the invocation 'foo(1)' where foo is a variable of function type we should mark 'foo' if there is unsafe call error
                                                          context.call is CallForImplicitInvoke))
        return resultStatus
    }

    private fun <D : CallableDescriptor, C : CallResolutionContext<C>> checkValueArgumentTypes(context: CallResolutionContext<C>, candidateCall: MutableResolvedCall<D>, trace: BindingTrace, resolveFunctionArgumentBodies: CallResolverUtil.ResolveArgumentsMode): ValueArgumentsCheckingResult {
        var resultStatus = SUCCESS
        val argumentTypes = Lists.newArrayList<JetType>()
        val infoForArguments = candidateCall.getDataFlowInfoForArguments()
        for (entry in candidateCall.getValueArguments().entrySet()) {
            val parameterDescriptor = entry.getKey()
            val resolvedArgument = entry.getValue()


            for (argument in resolvedArgument.getArguments()) {
                val expression = argument.getArgumentExpression() ?: continue

                val expectedType = getEffectiveExpectedType(parameterDescriptor, argument)

                val newContext = context.replaceDataFlowInfo(infoForArguments.getInfo(argument)).replaceBindingTrace(trace).replaceExpectedType(expectedType)
                val typeInfoForCall = argumentTypeResolver.getArgumentTypeInfo(expression, newContext, resolveFunctionArgumentBodies)
                val type = typeInfoForCall.type
                infoForArguments.updateInfo(argument, typeInfoForCall.dataFlowInfo)

                var matchStatus = ArgumentMatchStatus.SUCCESS
                var resultingType: JetType? = type
                if (type == null || (type.isError() && !ErrorUtils.isFunctionPlaceholder(type))) {
                    matchStatus = ArgumentMatchStatus.ARGUMENT_HAS_NO_TYPE
                }
                else if (!noExpectedType(expectedType)) {
                    if (!ArgumentTypeResolver.isSubtypeOfForArgumentType(type, expectedType)) {
                        val smartCast = smartCastValueArgumentTypeIfPossible(expression, newContext.expectedType, type, newContext)
                        if (smartCast == null) {
                            resultStatus = OTHER_ERROR
                            matchStatus = ArgumentMatchStatus.TYPE_MISMATCH
                        }
                        else {
                            resultingType = smartCast
                        }
                    }
                    else if (ErrorUtils.containsUninferredParameter(expectedType)) {
                        matchStatus = ArgumentMatchStatus.MATCH_MODULO_UNINFERRED_TYPES
                    }
                }
                argumentTypes.add(resultingType)
                candidateCall.recordArgumentMatchStatus(argument, matchStatus)
            }
        }
        return ValueArgumentsCheckingResult(resultStatus, argumentTypes)
    }

    private fun smartCastValueArgumentTypeIfPossible(expression: JetExpression, expectedType: JetType, actualType: JetType, context: ResolutionContext<*>): JetType? {
        val receiverToCast = ExpressionReceiver(JetPsiUtil.safeDeparenthesize(expression, false), actualType)
        val variants = SmartCastUtils.getSmartCastVariantsExcludingReceiver(context, receiverToCast)
        for (possibleType in variants) {
            if (JetTypeChecker.DEFAULT.isSubtypeOf(possibleType, expectedType)) {
                return possibleType
            }
        }
        return null
    }

    private fun <D : CallableDescriptor> checkReceiverTypeError(context: CallCandidateResolutionContext<D>): ResolutionStatus {
        val candidateCall = context.candidateCall
        val candidateDescriptor = candidateCall.getCandidateDescriptor()

        val extensionReceiver = candidateDescriptor.getExtensionReceiverParameter()
        val dispatchReceiver = candidateDescriptor.getDispatchReceiverParameter()
        var status = SUCCESS
        // For the expressions like '42.(f)()' where f: String.() -> Unit we'd like to generate a type mismatch error on '1',
        // not to throw away the candidate, so the following check is skipped.
        if (!CallResolverUtil.isInvokeCallOnExpressionWithBothReceivers(context.call)) {
            status = status.combine(checkReceiverTypeError(context, extensionReceiver, candidateCall.getExtensionReceiver()))
        }
        status = status.combine(checkReceiverTypeError(context, dispatchReceiver, candidateCall.getDispatchReceiver()))
        return status
    }

    private fun <D : CallableDescriptor> checkReceiverTypeError(context: CallCandidateResolutionContext<D>, receiverParameterDescriptor: ReceiverParameterDescriptor?, receiverArgument: ReceiverValue): ResolutionStatus {
        if (receiverParameterDescriptor == null || !receiverArgument.exists()) return SUCCESS

        val candidateDescriptor = context.candidateCall.getCandidateDescriptor()

        val erasedReceiverType = CallResolverUtil.getErasedReceiverType(receiverParameterDescriptor, candidateDescriptor)

        val isSubtypeBySmartCast = SmartCastUtils.isSubTypeBySmartCastIgnoringNullability(receiverArgument, erasedReceiverType, context)
        if (!isSubtypeBySmartCast) {
            return RECEIVER_TYPE_ERROR
        }

        return SUCCESS
    }

    private fun <D : CallableDescriptor> checkReceiver(context: CallCandidateResolutionContext<D>, candidateCall: ResolvedCall<D>, trace: BindingTrace, receiverParameter: ReceiverParameterDescriptor?, receiverArgument: ReceiverValue, isExplicitReceiver: Boolean, implicitInvokeCheck: Boolean): ResolutionStatus {
        if (receiverParameter == null || !receiverArgument.exists()) return SUCCESS
        val candidateDescriptor = candidateCall.getCandidateDescriptor()
        if (TypeUtils.dependsOnTypeParameters(receiverParameter.getType(), candidateDescriptor.getTypeParameters())) return SUCCESS

        val safeAccess = isExplicitReceiver && !implicitInvokeCheck && candidateCall.getCall().isExplicitSafeCall()
        val isSubtypeBySmartCast = SmartCastUtils.isSubTypeBySmartCastIgnoringNullability(receiverArgument, receiverParameter.getType(), context)
        if (!isSubtypeBySmartCast) {
            context.tracing.wrongReceiverType(trace, receiverParameter, receiverArgument)
            return OTHER_ERROR
        }
        if (!SmartCastUtils.recordSmartCastIfNecessary(receiverArgument, receiverParameter.getType(), context, safeAccess)) {
            return OTHER_ERROR
        }

        val receiverArgumentType = receiverArgument.getType()

        val bindingContext = trace.getBindingContext()
        if (!safeAccess && !receiverParameter.getType().isMarkedNullable() && receiverArgumentType.isMarkedNullable()) {
            if (!SmartCastUtils.canBeSmartCast(receiverParameter, receiverArgument, context)) {
                context.tracing.unsafeCall(trace, receiverArgumentType, implicitInvokeCheck)
                return UNSAFE_CALL_ERROR
            }
        }
        val receiverValue = DataFlowValueFactory.createDataFlowValue(receiverArgument, bindingContext, context.scope.getContainingDeclaration())
        if (safeAccess && !context.dataFlowInfo.getNullability(receiverValue).canBeNull()) {
            context.tracing.unnecessarySafeCall(trace, receiverArgumentType)
        }

        context.additionalTypeChecker.checkReceiver(receiverParameter, receiverArgument, safeAccess, context)

        return SUCCESS
    }

    public inner class ValueArgumentsCheckingResult(public val status: ResolutionStatus, public val argumentTypes: List<JetType>)

    public fun getEffectiveExpectedType(parameterDescriptor: ValueParameterDescriptor, argument: ValueArgument): JetType {
        if (argument.getSpreadElement() != null) {
            if (parameterDescriptor.getVarargElementType() == null) {
                // Spread argument passed to a non-vararg parameter, an error is already reported by ValueArgumentsToParametersMapper
                return DONT_CARE
            }
            else {
                return parameterDescriptor.getType()
            }
        }
        else {
            val varargElementType = parameterDescriptor.getVarargElementType()
            if (varargElementType != null) {
                return varargElementType
            }

            return parameterDescriptor.getType()
        }
    }

    private fun checkGenericBoundsInAFunctionCall(jetTypeArguments: List<JetTypeProjection>, typeArguments: List<JetType>, functionDescriptor: CallableDescriptor, substitutor: TypeSubstitutor, trace: BindingTrace) {
        val typeParameters = functionDescriptor.getTypeParameters()
        for (i in 0..Math.min(typeParameters.size(), jetTypeArguments.size()) - 1) {
            val typeParameterDescriptor = typeParameters.get(i)
            val typeArgument = typeArguments.get(i)
            val typeReference = jetTypeArguments.get(i).getTypeReference()
            if (typeReference != null) {
                DescriptorResolver.checkBounds(typeReference, typeArgument, typeParameterDescriptor, substitutor, trace)
            }
        }
    }
}
