// !DIAGNOSTICS: -UNUSED_VARIABLE

interface A
interface B

interface C: A, B
interface D: A, B
interface E: A, B

fun foo(c: C?, d: D?, e: E?) {
    val a: A? = c ?: d ?: e

    val b: B? = if (false) if (true) c else d else e

    //outer elvis operator and if-expression have error types
}