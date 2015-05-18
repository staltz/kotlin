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

package org.jetbrains.kotlin.resolve;

import org.jetbrains.annotations.Mutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.scopes.JetScope;

import java.util.Collection;
import java.util.Map;

public interface BodiesResolveContext {
    @ReadOnly
    Collection<JetFile> getFiles();

    @Mutable
    Map<JetClassOrObject, ClassDescriptorWithResolutionScopes> getDeclaredClasses();
    @Mutable
    Map<JetClassInitializer, ClassDescriptorWithResolutionScopes> getAnonymousInitializers();
    @Mutable
    Map<JetSecondaryConstructor, ConstructorDescriptor> getSecondaryConstructors();
    @Mutable
    Map<JetScript, ScriptDescriptor> getScripts();

    @Mutable
    Map<JetProperty, PropertyDescriptor> getProperties();
    @Mutable
    Map<JetNamedFunction, SimpleFunctionDescriptor> getFunctions();

    @Nullable
    JetScope getDeclaringScope(@NotNull JetDeclaration declaration);

    @NotNull
    DataFlowInfo getOuterDataFlowInfo();

    @NotNull
    TopDownAnalysisMode getTopDownAnalysisMode();
}
