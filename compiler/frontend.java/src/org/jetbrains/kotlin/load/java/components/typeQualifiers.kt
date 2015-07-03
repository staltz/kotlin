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

package org.jetbrains.kotlin.load.java.components

import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.components.MutabilityQualifier.MUTABLE
import org.jetbrains.kotlin.load.java.components.MutabilityQualifier.READ_ONLY
import org.jetbrains.kotlin.load.java.components.NullabilityQualifier.NOT_NULL
import org.jetbrains.kotlin.load.java.components.NullabilityQualifier.NULLABLE
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.flexibility
import org.jetbrains.kotlin.types.isFlexible

enum class NullabilityQualifier {
    NULLABLE,
    NOT_NULL
}

val NULLABLE_ANNOTATIONS = listOf(
        JvmAnnotationNames.JETBRAINS_NULLABLE_ANNOTATION
)

val NOT_NULL_ANNOTATIONS = listOf(
        JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION
)

enum class MutabilityQualifier {
    READ_ONLY,
    MUTABLE
}

val READ_ONLY_ANNOTATIONS = listOf(
        JvmAnnotationNames.JETBRAINS_READONLY_ANNOTATION
)

val MUTABLE_ANNOTATIONS = listOf(
        JvmAnnotationNames.JETBRAINS_MUTABLE_ANNOTATION
)

class JavaTypeQualifiers(
        val nullability: NullabilityQualifier?,
        val mutability: MutabilityQualifier?
)

private fun JetType.extractQualifiers(): JavaTypeQualifiers {
    val (lower, upper) =
            if (this.isFlexible())
                flexibility().let { Pair(it.lowerBound, it.upperBound) }
            else Pair(this, this)

    val mapping = JavaToKotlinClassMap.INSTANCE
    return JavaTypeQualifiers(
            if (lower.isMarkedNullable()) NULLABLE else if (!upper.isMarkedNullable()) NOT_NULL else null,
            if (mapping.isReadOnly(lower)) READ_ONLY else if (mapping.isMutable(upper)) MUTABLE else null
    )
}

private fun Annotations.extractQualifiers(): JavaTypeQualifiers {
    fun <T: Any> List<FqName>.ifPresent(qualifier: T) = if (any { findAnnotation(it) != null}) qualifier else null
    fun <T: Any> singleNotNull(x: T?, y: T?) = if (x == null || y == null) x ?: y else null

    return JavaTypeQualifiers(
            singleNotNull(NULLABLE_ANNOTATIONS.ifPresent(NULLABLE), NOT_NULL_ANNOTATIONS.ifPresent(NOT_NULL)),
            singleNotNull(READ_ONLY_ANNOTATIONS.ifPresent(READ_ONLY), MUTABLE_ANNOTATIONS.ifPresent(MUTABLE))
    )
}

fun JetType.computeQualifiersForOverride(fromSupertypes: Collection<JetType>, isCovariant: Boolean): JavaTypeQualifiers {
    val nullabilityFromSupertypes = fromSupertypes.map { it.extractQualifiers().nullability }.filterNotNull().toSet()
    val mutabilityFromSupertypes = fromSupertypes.map { it.extractQualifiers().mutability }.filterNotNull().toSet()
    val own = getAnnotations().extractQualifiers()

    if (isCovariant) {
        fun <T : Any> Set<T>.selectCovariantly(low: T, high: T, own: T?): T? {
            val supertypeQualifier = if (low in this) low else if (high in this) high else null
            return if (supertypeQualifier == low && own == high) null else own ?: supertypeQualifier
        }
        return JavaTypeQualifiers(
                nullabilityFromSupertypes.selectCovariantly(NOT_NULL, NULLABLE, own.nullability),
                mutabilityFromSupertypes.selectCovariantly(MUTABLE, READ_ONLY, own.mutability)
        )
    }
    else {
        fun <T : Any> Set<T>.selectInvariantly(own: T?): T? {
            val effectiveSet = own?.let { (this + own).toSet() } ?: this
            // if this set contains exactly one element, it is the qualifier everybody agrees upon,
            // otherwise (no qualifiers, or multiple qualifiers), there's no single such qualifier
            // and all qualifiers are discarded
            return effectiveSet.singleOrNull()
        }
        return JavaTypeQualifiers(
                nullabilityFromSupertypes.selectInvariantly(own.nullability),
                mutabilityFromSupertypes.selectInvariantly(own.mutability)
        )
    }
}
