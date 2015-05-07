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

package org.jetbrains.kotlin.descriptors.impl

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.ChainedScope
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.types.TypeSubstitutor

public class PackageViewDescriptorImpl(
        module: ModuleDescriptor,
        fqName: FqName,
        private val fragments: List<PackageFragmentDescriptor>
) : AbstractPackageViewDescriptor(fqName, module) {
    private val memberScope: JetScope = run {
        assert(fragments.isNotEmpty()) { "$fqName in module" }

        val scopes = fragments.map { it.getMemberScope() } + SubpackagesScope(this)
        ChainedScope(this, "package view scope for $fqName in ${module.getName()}", *scopes.toTypedArray())
    }

    override fun getMemberScope(): JetScope = memberScope

    override fun getFragments() = fragments
}

public abstract class AbstractPackageViewDescriptor(private val fqName: FqName, private val module: ModuleDescriptor) :
        DeclarationDescriptorImpl(Annotations.EMPTY, fqName.shortNameOrSpecial()), PackageViewDescriptor {

    override fun getContainingDeclaration(): PackageViewDescriptor? = getModule().packageViewManager.getParentView(this)

    override fun equals(other: Any?): Boolean {
        val that = other as? AbstractPackageViewDescriptor ?: return false
        return this.getFqName() == that.getFqName() && this.getModule() == that.getModule()
    }

    override fun hashCode(): Int {
        var result = getModule().hashCode()
        result = 31 * result + getFqName().hashCode()
        return result
    }

    override fun substitute(substitutor: TypeSubstitutor): DeclarationDescriptor? = this

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R = visitor.visitPackageViewDescriptor(this, data)

    override fun getFqName(): FqName = fqName

    override fun getModule(): ModuleDescriptor = module
}