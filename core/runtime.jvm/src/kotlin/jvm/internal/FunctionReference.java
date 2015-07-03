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

package kotlin.jvm.internal;

import kotlin.jvm.KotlinReflectionNotSupportedError;
import kotlin.reflect.KDeclarationContainer;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;

import java.util.List;

public class FunctionReference extends FunctionImpl implements KFunction {
    private final int arity;

    public FunctionReference(int arity) {
        this.arity = arity;
    }

    @Deprecated // preserved for binary compatibility with M12 release
    public FunctionReference() {
        this.arity = 0;
    }

    @Override
    public int getArity() {
        return arity;
    }

    // See CallableReference

    public KDeclarationContainer getOwner() {
        throw error();
    }

    @Override
    public String getName() {
        throw error();
    }

    public String getSignature() {
        throw error();
    }

    public List<KParameter> getParameters() {
        throw error();
    }

    private static Error error() {
        throw new KotlinReflectionNotSupportedError();
    }
}
