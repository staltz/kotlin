// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE

fun <T, R> foo(first: () -> T, second: (T) -> R): R = throw Exception()
fun test() {
    val r = foo( { val x = 4; x }, { "${it + 1}" } )
    r checkType { _<String>() }
}
