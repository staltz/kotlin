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

package org.jetbrains.kotlin.codegen.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.OwnerKind;
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;

import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.anonymousClassForCallable;

public class PropertyReferenceContext extends ClassContext {
    // NOTE: this is not the target property referenced by the double colon operator, but rather a temporary variable
    private final VariableDescriptor variableDescriptor;

    public PropertyReferenceContext(
            @NotNull JetTypeMapper typeMapper,
            @NotNull VariableDescriptor variableDescriptor,
            @Nullable CodegenContext parentContext,
            @Nullable LocalLookup localLookup
    ) {
        super(typeMapper, anonymousClassForCallable(typeMapper.getBindingContext(), variableDescriptor),
              OwnerKind.IMPLEMENTATION, parentContext, localLookup);

        this.variableDescriptor = variableDescriptor;
    }

    @NotNull
    public VariableDescriptor getVariableDescriptor() {
        return variableDescriptor;
    }

    @Override
    public String toString() {
        return "Property reference: " + variableDescriptor;
    }
}
