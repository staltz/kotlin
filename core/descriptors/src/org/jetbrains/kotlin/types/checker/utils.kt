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

package org.jetbrains.kotlin.types.checker

import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedureCallbacks
import java.util.*

data class SubtypePathNode(val type: JetType, val parent: SubtypePathNode?) {
    override fun equals(other: Any?) = this `identityEquals` other
    override fun hashCode(): Int = System.identityHashCode(this)
}

public fun findCorrespondingSupertype(
        subtype: JetType, supertype: JetType,
        typeCheckingProcedureCallbacks: TypeCheckingProcedureCallbacks
): JetType? {
    val queue = ArrayDeque<SubtypePathNode>()
    queue.add(SubtypePathNode(subtype, null))

    val supertypeConstructor = supertype.getConstructor()

    while (!queue.isEmpty()) {
        val (currentSubtype, parent) = queue.poll()
        val constructor = currentSubtype.getConstructor()

        if (typeCheckingProcedureCallbacks.assertEqualTypeConstructors(constructor, supertypeConstructor)) {
            var substituted = currentSubtype
            var currentPathNode = parent
            while (currentPathNode != null) {
                substituted = TypeSubstitutor.create(currentPathNode.type).safeSubstitute(substituted, Variance.INVARIANT)
                currentPathNode = currentPathNode.parent
            }

            return substituted
        }

        val currentPathNode = SubtypePathNode(currentSubtype, parent)

        for (immediateSupertype in constructor.getSupertypes()) {
            queue.add(SubtypePathNode(immediateSupertype, currentPathNode))
        }
    }

    return null
}
