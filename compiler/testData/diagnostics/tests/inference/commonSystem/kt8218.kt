// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

import java.util.Collections

fun foo<T> (f: () -> List<T>) : T = throw Exception()
fun test() {
    val b: Int = foo { Collections.emptyList() }
}