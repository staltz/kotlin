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

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.stubs.KotlinAnnotationModifierReferenceExpressionStub
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes

// This node represents "fake" reference expression for annotation(arguments) modifier syntax
// It always use the real constructor name of "annotation"
public class JetAnnotationModifierReferenceExpression:
        JetExpressionImplStub<KotlinAnnotationModifierReferenceExpressionStub>, JetSimpleNameExpression {

    public constructor(node: ASTNode) : super(node)

    public constructor(stub: KotlinAnnotationModifierReferenceExpressionStub) :
    super(stub, JetStubElementTypes.ANNOTATION_MODIFIER_REFERENCE_EXPRESSION)

    // It is PsiElement(annotation) just before the associated annotation entry
    private val referencedElement: PsiElement
        get() = calcReferencedElement()!!

    private fun calcReferencedElement(): PsiElement? {
        val owner: PsiElement? = this.getStrictParentOfType<JetAnnotationEntry>() as? JetAnnotationEntry
        return owner?.getPrevSiblingIgnoringWhitespaceAndComments()
    }

    override fun getReferencedName(): String {
        return JetTokens.ANNOTATION_KEYWORD.getValue()
    }

    override fun getReferencedNameAsName(): Name {
        return Name.identifier(getReferencedName())
    }

    override fun getReferencedNameElement(): PsiElement {
        return referencedElement
    }

    override fun getIdentifier(): PsiElement? {
        return null
    }

    override fun getReferencedNameElementType(): IElementType {
        return getReferencedNameElement().getNode()!!.getElementType()
    }

    override fun <R, D> accept(visitor: JetVisitor<R, D>, data: D): R {
        return visitor.visitSimpleNameExpression(this, data)
    }
}
