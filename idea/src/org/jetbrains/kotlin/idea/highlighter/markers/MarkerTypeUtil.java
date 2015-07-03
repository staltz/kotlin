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

package org.jetbrains.kotlin.idea.highlighter.markers;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.daemon.DaemonBundle;
import com.intellij.codeInsight.daemon.impl.GutterIconTooltipHelper;
import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator;
import com.intellij.codeInsight.navigation.ListBackgroundUpdaterTask;
import com.intellij.ide.util.PsiClassListCellRenderer;
import com.intellij.ide.util.PsiClassOrFunctionalExpressionListCellRenderer;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFunctionalExpression;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.PsiElementProcessorAdapter;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.search.searches.FunctionalExpressionSearch;
import com.intellij.util.ArrayUtil;
import com.intellij.util.CommonProcessors;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Comparator;

public class MarkerTypeUtil {
    static String getSubclassedClassTooltip(@NotNull PsiClass aClass) {
        PsiElementProcessor.CollectElementsWithLimit<PsiClass> processor = new PsiElementProcessor.CollectElementsWithLimit<PsiClass>(5, new THashSet<PsiClass>());
        ClassInheritorsSearch.search(aClass, true).forEach(new PsiElementProcessorAdapter<PsiClass>(processor));

        if (processor.isOverflow()) {
            return aClass.isInterface()
                   ? DaemonBundle.message("interface.is.implemented.too.many")
                   : DaemonBundle.message("class.is.subclassed.too.many");
        }

        PsiClass[] subclasses = processor.toArray(PsiClass.EMPTY_ARRAY);
        if (subclasses.length == 0) {
            final PsiElementProcessor.CollectElementsWithLimit<PsiFunctionalExpression> functionalImplementations =
                    new PsiElementProcessor.CollectElementsWithLimit<PsiFunctionalExpression>(2, new THashSet<PsiFunctionalExpression>());
            FunctionalExpressionSearch.search(aClass).forEach(new PsiElementProcessorAdapter<PsiFunctionalExpression>(functionalImplementations));
            if (!functionalImplementations.getCollection().isEmpty()) {
                return "Has functional implementations";
            }
            return null;
        }

        Comparator<PsiClass> comparator = new PsiClassListCellRenderer().getComparator();
        Arrays.sort(subclasses, comparator);

        String start = aClass.isInterface()
                       ? DaemonBundle.message("interface.is.implemented.by.header")
                       : DaemonBundle.message("class.is.subclassed.by.header");
        @NonNls String pattern = "&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"#javaClass/{0}\">{0}</a>";
        return composeText(subclasses, start, pattern, IdeActions.ACTION_GOTO_IMPLEMENTATION);
    }

    static void navigateToSubclassedClass(MouseEvent e, @NotNull final PsiClass aClass) {
        if (DumbService.isDumb(aClass.getProject())) {
            DumbService.getInstance(aClass.getProject()).showDumbModeNotification("Navigation to overriding methods is not possible during index update");
            return;
        }

        final PsiElementProcessor.CollectElementsWithLimit<PsiClass> collectProcessor = new PsiElementProcessor.CollectElementsWithLimit<PsiClass>(2, new THashSet<PsiClass>());
        final PsiElementProcessor.CollectElementsWithLimit<PsiFunctionalExpression> collectExprProcessor = new PsiElementProcessor.CollectElementsWithLimit<PsiFunctionalExpression>(2, new THashSet<PsiFunctionalExpression>());
        if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
            @Override
            public void run() {
                ClassInheritorsSearch.search(aClass, true).forEach(new PsiElementProcessorAdapter<PsiClass>(collectProcessor));
                if (collectProcessor.getCollection().isEmpty()) {
                    FunctionalExpressionSearch.search(aClass).forEach(new PsiElementProcessorAdapter<PsiFunctionalExpression>(collectExprProcessor));
                }
            }
        }, SEARCHING_FOR_OVERRIDDEN_METHODS, true, aClass.getProject(), (JComponent)e.getComponent())) {
            return;
        }

        NavigatablePsiElement[] inheritors = ArrayUtil.mergeArrays(collectProcessor.toArray(PsiClass.EMPTY_ARRAY),
                                                                   collectExprProcessor.toArray(PsiFunctionalExpression.EMPTY_ARRAY));
        if (inheritors.length == 0) return;
        final PsiClassOrFunctionalExpressionListCellRenderer renderer = new PsiClassOrFunctionalExpressionListCellRenderer();
        final SubclassUpdater subclassUpdater = new SubclassUpdater(aClass, renderer);
        Arrays.sort(inheritors, renderer.getComparator());
        PsiElementListNavigator.openTargets(e, inheritors, subclassUpdater.getCaption(inheritors.length),
                                            CodeInsightBundle.message("goto.implementation.findUsages.title", aClass.getName()), renderer,
                                            subclassUpdater);
    }

    @NotNull
    private static String composeText(@NotNull PsiElement[] methods, @NotNull String start, @NotNull String pattern, @NotNull String actionId) {
        Shortcut[] shortcuts = ActionManager.getInstance().getAction(actionId).getShortcutSet().getShortcuts();
        Shortcut shortcut = ArrayUtil.getFirstElement(shortcuts);
        String postfix = "<br><div style='margin-top: 5px'><font size='2'>Click";
        if (shortcut != null) postfix += " or press " + KeymapUtil.getShortcutText(shortcut);
        postfix += " to navigate</font></div>";
        return GutterIconTooltipHelper.composeText(Arrays.asList(methods), start, pattern, postfix);
    }

    private static final String SEARCHING_FOR_OVERRIDDEN_METHODS = "Searching for Overridden Methods";

    private static class SubclassUpdater extends ListBackgroundUpdaterTask {
        private final PsiClass myClass;
        private final PsiClassOrFunctionalExpressionListCellRenderer myRenderer;

        private SubclassUpdater(@NotNull PsiClass aClass, @NotNull PsiClassOrFunctionalExpressionListCellRenderer renderer) {
            super(aClass.getProject(), SEARCHING_FOR_OVERRIDDEN_METHODS);
            myClass = aClass;
            myRenderer = renderer;
        }

        @Override
        public String getCaption(int size) {
            return myClass.isInterface()
                   ? CodeInsightBundle.message("goto.implementation.chooserTitle", myClass.getName(), size)
                   : DaemonBundle.message("navigation.title.subclass", myClass.getName(), size);
        }

        @Override
        public void run(@NotNull final ProgressIndicator indicator) {
            super.run(indicator);
            ClassInheritorsSearch.search(myClass, ApplicationManager.getApplication().runReadAction(new Computable<SearchScope>() {
                @Override
                public SearchScope compute() {
                    return myClass.getUseScope();
                }
            }), true).forEach(new CommonProcessors.CollectProcessor<PsiClass>() {
                @Override
                public boolean process(final PsiClass o) {
                    if (!updateComponent(o, myRenderer.getComparator())) {
                        indicator.cancel();
                    }
                    indicator.checkCanceled();
                    return super.process(o);
                }
            });

            FunctionalExpressionSearch.search(myClass).forEach(new CommonProcessors.CollectProcessor<PsiFunctionalExpression>() {
                @Override
                public boolean process(final PsiFunctionalExpression expr) {
                    if (!updateComponent(expr, myRenderer.getComparator())) {
                        indicator.cancel();
                    }
                    indicator.checkCanceled();
                    return super.process(expr);
                }
            });
        }
    }
}
