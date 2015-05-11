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

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemImpl.ConstraintKind.EQUAL
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemImpl.ConstraintKind.SUB_TYPE
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.Bound
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.EXACT_BOUND
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.LOWER_BOUND
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.UPPER_BOUND
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.CompoundConstraintPosition
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.Variance.INVARIANT
import org.jetbrains.kotlin.types.Variance.OUT_VARIANCE
import java.util.ArrayList

fun ConstraintSystemImpl.incorporateConstraint(variable: JetType, constrainingBound: Bound) {
    val typeBounds = getTypeBounds(variable)
    val bounds = ArrayList(typeBounds.bounds)
    for (variableBound in bounds) {
        addConstraintFromBounds(variableBound, constrainingBound)
    }

    val constrainingType = constrainingBound.constrainingType
    if (isMyTypeVariable(constrainingType)) {
        val bound = Bound(variable, constrainingBound.kind.reverse(), constrainingBound.position, pure = false)
        addBound(constrainingType, bound)
        return
    }
    constrainingType.forEachTypeArgument {
        typeProjection ->
        val argument = typeProjection.getType()
        val variance = typeProjection.getProjectionKind()
        val typeVariable = getMyTypeVariable(argument)
        if (typeVariable != null) {
            for (variableBound in getTypeBounds(typeVariable).bounds) {
                val newKind = computeNewKind(variance, variableBound.kind, constrainingBound.kind)
                if (newKind != null && variableBound != constrainingBound) {
                    val newTypeProjection = TypeProjectionImpl(variance, variableBound.constrainingType)
                    val substitutor = TypeSubstitutor.create(mapOf(typeVariable.getTypeConstructor() to newTypeProjection))
                    val newConstrainingType = substitutor.substitute(constrainingType, Variance.INVARIANT)!!
                    val pure = with (this) { newConstrainingType.isPure() }
                    val position = CompoundConstraintPosition(variableBound.position, constrainingBound.position)
                    addBound(variable, Bound(newConstrainingType, newKind, position, pure))
                }
            }
        }
    }
}

fun ConstraintSystemImpl.addConstraintFromBounds(first: Bound, second: Bound) {
    if (first == second) return
    if (first.pure && second.pure) return

    val firstType = first.constrainingType
    val secondType = second.constrainingType
    val position = CompoundConstraintPosition(first.position, second.position)

    when (first.kind to second.kind) {
        LOWER_BOUND to UPPER_BOUND, LOWER_BOUND to EXACT_BOUND, EXACT_BOUND to UPPER_BOUND ->
            addConstraint(SUB_TYPE, firstType, secondType, position)

        UPPER_BOUND to LOWER_BOUND, UPPER_BOUND to EXACT_BOUND, EXACT_BOUND to LOWER_BOUND ->
            addConstraint(SUB_TYPE, secondType, firstType, position)

        EXACT_BOUND to EXACT_BOUND ->
            addConstraint(EQUAL, firstType, secondType, position)
    }
}

fun computeNewKind(variance: Variance, variableKind: BoundKind, constrainingKind: BoundKind): BoundKind? {
    if (variableKind == EXACT_BOUND) return constrainingKind
    if (variance == INVARIANT) return null

    fun BoundKind.reverse(variance: Variance) = if (variance == OUT_VARIANCE) this else this.reverse()

    if (constrainingKind == EXACT_BOUND) return variableKind.reverse(variance)
    if (constrainingKind.reverse(variance) == variableKind) return constrainingKind
    return null
}

fun JetType.forEachTypeArgument(f: (TypeProjection) -> Unit) = TypeProjectionImpl(this).forEachTypeArgument(f)

fun TypeProjection.forEachTypeArgument(f: (TypeProjection) -> Unit) {
    if (isStarProjection()) return
    val type = getType()
    val arguments = type.getArguments()
    if (arguments.isEmpty()) {
        f(this)
        return
    }
    type.getConstructor().getParameters().zip(arguments).forEach {
        val (parameter, argument) = it
        val typeProjection = if (argument.getProjectionKind() == INVARIANT && parameter.getVariance() != INVARIANT) {
            TypeProjectionImpl(parameter.getVariance(), argument.getType())
        }
        else {
            argument
        }
        typeProjection.forEachTypeArgument(f)
    }
}