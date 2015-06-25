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

package org.jetbrains.kotlin.container

import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Type
import java.util.ArrayList

public interface ValueResolver {
    fun resolve(request: Class<*>, context: ValueResolveContext): ValueDescriptor?
}

public interface ValueResolveContext {
    fun resolve(registration: Class<*>): ValueDescriptor?
}

internal class ComponentResolveContext(val container: StorageComponentContainer, val requestingDescriptor: ValueDescriptor) : ValueResolveContext {
    override fun resolve(registration: Class<*>): ValueDescriptor? = container.resolve(registration, this)
    public override fun toString(): String = "for $requestingDescriptor in $container"
}

public class ConstructorBinding(val constructor: Constructor<*>, val argumentDescriptors: List<ValueDescriptor>)

public class MethodBinding(val method: Method, val argumentDescriptors: List<ValueDescriptor>) {
    fun invoke(instance: Any) {
        val arguments = computeArguments(argumentDescriptors).toTypedArray()
        method.invoke(instance, *arguments)
    }
}

public fun computeArguments(argumentDescriptors: List<ValueDescriptor>): List<Any> = argumentDescriptors.map { it.getValue() }

fun Class<*>.bindToConstructor(context: ValueResolveContext): ConstructorBinding {
    val constructorInfo = getInfo().constructorInfo!!
    val candidate = constructorInfo.constructor
    val (bound, unsatisfied) = bindArguments(constructorInfo.parameters, context)

    if (unsatisfied == null)
        return ConstructorBinding(candidate, bound)

    throw UnresolvedDependenciesException("Dependencies for type `$this` cannot be satisfied:\n  $unsatisfied")
}

fun Method.bindToMethod(context: ValueResolveContext): MethodBinding {
    val parameters = getParameterTypes().toList()
    val (bound, unsatisfied) = bindArguments(parameters, context)

    if (unsatisfied == null) // constructor is satisfied with arguments
        return MethodBinding(this, bound)

    throw UnresolvedDependenciesException("Dependencies for method `$this` cannot be satisfied:\n  $unsatisfied")
}

private fun bindArguments(parameters: List<Class<*>>, context: ValueResolveContext): Pair<List<ValueDescriptor>, MutableList<Type>?> {
    val bound = ArrayList<ValueDescriptor>(parameters.size())
    var unsatisfied: MutableList<Type>? = null

    for (parameter in parameters) {
        val descriptor = context.resolve(parameter)
        if (descriptor == null) {
            if (unsatisfied == null)
                unsatisfied = ArrayList<Type>()
            unsatisfied.add(parameter)
        }
        else {
            bound.add(descriptor)
        }
    }
    return Pair(bound, unsatisfied)
}

class UnresolvedDependenciesException(message: String) : Exception(message)
