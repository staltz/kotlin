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

package org.jetbrains.kotlin.resolve.calls.inference.constraintUtil

import com.google.common.collect.Lists
import com.google.common.collect.Maps
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemStatus
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Collections
import java.util.Comparator

public fun getFirstConflictingParameter(constraintSystem: ConstraintSystem): TypeParameterDescriptor? {
    for (typeParameter in constraintSystem.getTypeVariables()) {
        val constraints = constraintSystem.getTypeBounds(typeParameter)
        if (constraints.values.size() > 1) {
            return typeParameter
        }
    }
    return null
}

public fun getSubstitutorsForConflictingParameters(constraintSystem: ConstraintSystem): Collection<TypeSubstitutor> {
    val firstConflictingParameter = getFirstConflictingParameter(constraintSystem) ?: return emptyList<TypeSubstitutor>()

    val conflictingTypes = constraintSystem.getTypeBounds(firstConflictingParameter).values

    val substitutionContexts = Lists.newArrayList<MutableMap<TypeConstructor, TypeProjection>>()
    for (type in conflictingTypes) {
        val context = Maps.newLinkedHashMap<TypeConstructor, TypeProjection>()
        context.put(firstConflictingParameter.getTypeConstructor(), TypeProjectionImpl(type))
        substitutionContexts.add(context)
    }

    for (typeParameter in constraintSystem.getTypeVariables()) {
        if (typeParameter == firstConflictingParameter) continue

        val safeType = getSafeValue(constraintSystem, typeParameter)
        for (context in substitutionContexts) {
            val typeProjection = TypeProjectionImpl(safeType)
            context.put(typeParameter.getTypeConstructor(), typeProjection)
        }
    }
    val typeSubstitutors = Lists.newArrayList<TypeSubstitutor>()
    for (context in substitutionContexts) {
        typeSubstitutors.add(TypeSubstitutor.create(context))
    }
    return typeSubstitutors
}

public fun getSafeValue(constraintSystem: ConstraintSystem, typeParameter: TypeParameterDescriptor): JetType {
    val type = constraintSystem.getTypeBounds(typeParameter).value
    if (type != null) {
        return type
    }
    //todo may be error type
    return typeParameter.getUpperBoundsAsType()
}

public fun checkUpperBoundIsSatisfied(constraintSystem: ConstraintSystem, typeParameter: TypeParameterDescriptor, substituteOtherTypeParametersInBound: Boolean): Boolean {
    val type = constraintSystem.getTypeBounds(typeParameter).value ?: return true
    for (upperBound in typeParameter.getUpperBounds()) {
        if (!substituteOtherTypeParametersInBound && TypeUtils.dependsOnTypeParameters(upperBound, constraintSystem.getTypeVariables())) {
            continue
        }
        val substitutedUpperBound = constraintSystem.getResultingSubstitutor().substitute(upperBound, Variance.INVARIANT)

        assert(substitutedUpperBound != null) { "We wanted to substitute projections as a result for " + typeParameter }
        if (!JetTypeChecker.DEFAULT.isSubtypeOf(type, substitutedUpperBound)) {
            return false
        }
    }
    return true
}

public fun checkBoundsAreSatisfied(constraintSystem: ConstraintSystem, substituteOtherTypeParametersInBounds: Boolean): Boolean {
    for (typeVariable in constraintSystem.getTypeVariables()) {
        if (!checkUpperBoundIsSatisfied(constraintSystem, typeVariable, substituteOtherTypeParametersInBounds)) {
            return false
        }
    }
    return true
}

public fun getDebugMessageForStatus(status: ConstraintSystemStatus): String {
    val sb = StringBuilder()
    val interestingMethods = Lists.newArrayList<Method>()
    for (method in status.javaClass.getMethods()) {
        val name = method.getName()
        val isInteresting = name.startsWith("is") || name.startsWith("has") && name != "hashCode"
        if (method.getParameterTypes().size() == 0 && isInteresting) {
            interestingMethods.add(method)
        }
    }
    Collections.sort(interestingMethods, object : Comparator<Method> {
        override fun compare(method1: Method, method2: Method): Int {
            return method1.getName().compareTo(method2.getName())
        }
    })
    val iterator = interestingMethods.iterator()
    while (iterator.hasNext()) {
        val method = iterator.next()
        try {
            sb.append("-").append(method.getName()).append(": ").append(method.invoke(status))
            if (iterator.hasNext()) {
                sb.append("\n")
            }
        }
        catch (e: IllegalAccessException) {
            sb.append(e.getMessage())
        }
        catch (e: InvocationTargetException) {
            sb.append(e.getMessage())
        }

    }
    return sb.toString()
}
