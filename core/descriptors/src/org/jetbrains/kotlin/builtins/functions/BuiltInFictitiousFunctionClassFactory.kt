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

package org.jetbrains.kotlin.builtins.functions

import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor.Kind
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.ClassDescriptorFactory
import org.jetbrains.kotlin.storage.StorageManager
import kotlin.platform.platformStatic

/**
 * Produces descriptors representing the fictitious classes for function types, such as kotlin.Function1 or kotlin.reflect.KMemberFunction0.
 */
public class BuiltInFictitiousFunctionClassFactory(
        private val storageManager: StorageManager,
        private val module: ModuleDescriptor
) : ClassDescriptorFactory {

    private data class KindWithArity(val kind: Kind, val arity: Int)

    companion object {
        platformStatic public fun parseClassName(className: String, packageFqName: FqName): KindWithArity? {
            for (kind in FunctionClassDescriptor.Kinds.byPackage(packageFqName)) {
                val prefix = kind.classNamePrefix
                if (!className.startsWith(prefix)) continue

                val arity = toInt(className.substring(prefix.length())) ?: continue

                // TODO: validate arity, should be <= 255 for functions, <= 254 for members/extensions
                return KindWithArity(kind, arity)
            }

            return null
        }

        private fun toInt(s: String): Int? {
            if (s.isEmpty()) return null

            var result = 0
            for (c in s) {
                val d = c - '0'
                if (d !in 0..9) return null
                result = result * 10 + d
            }
            return result
        }
    }

    override fun createClass(classId: ClassId): ClassDescriptor? {
        if (classId.isLocal() || classId.isNestedClass()) return null

        val className = classId.getRelativeClassName().asString()
        if ("Function" !in className) return null // An optimization

        val packageFqName = classId.getPackageFqName()
        val kindWithArity = parseClassName(className, packageFqName) ?: return null
        val (kind, arity) = kindWithArity // KT-5100

        val containingPackageFragment = module.getPackage(packageFqName).fragments.single()

        return FunctionClassDescriptor(storageManager, containingPackageFragment, kind, arity)
    }
}
