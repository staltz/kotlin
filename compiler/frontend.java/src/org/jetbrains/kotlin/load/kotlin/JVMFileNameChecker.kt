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

package org.jetbrains.kotlin.load.kotlin

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.checkers.FileNameChecker
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

class JVMFileNameChecker : FileNameChecker {
    override fun check(file: JetFile, trace: BindingTrace) {
        val nameWithExtension = file.getName()
        if (nameWithExtension.toLowerCase().endsWith(".kts")) {
            return
        }
        assert(nameWithExtension.toLowerCase().endsWith(".kt")) { "File name should end with .kt, but was $nameWithExtension" }

        var simpleName = nameWithExtension.dropLast(3)

        if (!StringUtil.isJavaIdentifier(simpleName)) {
            trace.report(ErrorsJvm.INVALID_FILE_NAME.on(file, simpleName))
        }
    }
}