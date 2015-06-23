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

public trait ValueResolver {
    fun resolve(request: Class<*>, context: ValueResolveContext): ValueDescriptor?
}

public trait ValueResolveContext {
    fun resolve(registration: Class<*>): ValueDescriptor?
}

internal class ComponentResolveContext(val container: StorageComponentContainer, val requestingDescriptor: ValueDescriptor) : ValueResolveContext {
    override fun resolve(registration: Class<*>): ValueDescriptor? = container.resolve(registration, this)
    public override fun toString(): String = "for $requestingDescriptor in $container"
}

fun ComponentContainer.createInstance(klass: Class<*>): Any {
    val context = createResolveContext(DynamicComponentDescriptor)
    return klass.bindToConstructor(context).createInstance()
}

public class ConstructorBinding(val constructor: Constructor<*>, val argumentDescriptors: List<ValueDescriptor>) {
    fun createInstance(): Any = constructor.createInstance(argumentDescriptors)
}

public class MethodBinding(val method: Method, val argumentDescriptors: List<ValueDescriptor>) {
    fun invoke(instance: Any) {
        val arguments = bindArguments(argumentDescriptors).toTypedArray()
        method.invoke(instance, *arguments)
    }
}

fun Constructor<*>.createInstance(argumentDescriptors: List<ValueDescriptor>) = newInstance(bindArguments(argumentDescriptors))!!

public fun bindArguments(argumentDescriptors: List<ValueDescriptor>): List<Any> = argumentDescriptors.map { it.getValue() }

fun Class<*>.bindToConstructor(context: ValueResolveContext): ConstructorBinding {
    val constructorInfo = getInfo().constructorInfo!!
    val candidate = constructorInfo.constructor
    val arguments = ArrayList<ValueDescriptor>(constructorInfo.parameters.size())
    var unsatisfied: MutableList<Type>? = null

    for (parameter in constructorInfo.parameters) {
        val descriptor = context.resolve(parameter)
        if (descriptor == null) {
            if (unsatisfied == null)
                unsatisfied = ArrayList<Type>()
            unsatisfied.add(parameter)
        }
        else {
            arguments.add(descriptor)
        }
    }

    if (unsatisfied == null) // constructor is satisfied with arguments
        return ConstructorBinding(candidate, arguments)

    throw UnresolvedDependenciesException("Dependencies for type `$this` cannot be satisfied:\n  ${unsatisfied}")
}

fun Method.bindToMethod(context: ValueResolveContext): MethodBinding {
    val parameters = getParameterTypes()!!
    val arguments = ArrayList<ValueDescriptor>(parameters.size())
    var unsatisfied: MutableList<Type>? = null

    for (parameter in parameters) {
        val descriptor = context.resolve(parameter)
        if (descriptor == null) {
            if (unsatisfied == null)
                unsatisfied = ArrayList<Type>()
            unsatisfied.add(parameter)
        }
        else {
            arguments.add(descriptor)
        }
    }

    if (unsatisfied == null) // constructor is satisfied with arguments
        return MethodBinding(this, arguments)

    throw UnresolvedDependenciesException("Dependencies for method `$this` cannot be satisfied:\n  ${unsatisfied}")
}

class UnresolvedDependenciesException(message: String) : Exception(message)

