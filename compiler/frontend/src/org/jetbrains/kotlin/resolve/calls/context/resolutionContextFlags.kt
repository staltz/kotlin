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

package org.jetbrains.kotlin.resolve.calls.context

public enum class ContextDependency {
    /*
    A type of an expression can depend on expected type.
    E.g. 1. val i: List<Int> = emptyList() - type of 'emptyList()' depends on what's expected;
         2. fun foo(l: Long); foo(1) - type of '1' constant depends on the parameter type, it might be any number type.

    In some cases the expected type is known when an expression is analyzed, sometimes it's not (when we analyze function arguments).
    An expression is analyzed in DEPENDENT mode if it's type might be made more precise later.
        In the example 1 above we analyze an expression 'emptyList()' in the INDEPENDENT mode, we know the expected type.
        In the example 2 we analyze an expression '1' in the DEPENDENT mode, because it's expected type isn't known before resolving 'foo'.

    PARTLY_DEPENDENT means the dependence on the context except number constants. This mode is used to analyze function literals:
    E.g. 3. fun <T, R> foo(first: () -> T, second: (T) -> R): R; foo( { 4 }, { it } )
        The type of '4' might be made more precise later, but it influences on other type parameters, so we make it exact sooner.

    For expressions analyzed in DEPENDENT (and PARTLY_DEPENDENT) mode we complete an inference in CallCompleter.
     */
    INDEPENDENT, DEPENDENT, PARTLY_DEPENDENT;
}

public enum class ResolveArgumentsMode {
    DISABLED, RESOLVE_FUNCTION_ARGUMENTS, SHAPE_FUNCTION_ARGUMENTS;

    fun safeReplace(newArgumentsMode: ResolveArgumentsMode) = when (this) {
        DISABLED -> DISABLED
        else -> newArgumentsMode
    }
}

