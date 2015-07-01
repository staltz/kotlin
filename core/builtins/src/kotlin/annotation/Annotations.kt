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

package kotlin.annotation

public enum class AnnotationTarget {
    PACKAGE,
    CLASSIFIER,
    ANNOTATION_CLASS,
    TYPE_PARAMETER,
    PROPERTY,
    FIELD,
    LOCAL_VARIABLE,
    VALUE_PARAMETER,
    CONSTRUCTOR,
    FUNCTION,
    // TODO: should we join them together?
    PROPERTY_GETTER,
    PROPERTY_SETTER,
    TYPE,
    EXPRESSION,
    FILE
}

public enum class AnnotationRetention {
    SOURCE,
    BINARY,
    RUNTIME
}

target(AnnotationTarget.CLASSIFIER)
public annotation class target(vararg val allowedTargets: AnnotationTarget)

target(AnnotationTarget.CLASSIFIER)
public annotation class annotation (
        val retention: AnnotationRetention = AnnotationRetention.RUNTIME,
        val repeatable: Boolean = false
)
