package test

val key = JavaClass.Key<Unit>()

fun box(): String {
    val n1 = JavaClass.getNull(key)
    if (n1 != null) return "Fail 1: $n1"

    val n2 = JavaClass.get(key, null)
    if (n2 != null) return "Fail 2: $n2"

    val n3 = JavaClass.get(key, Unit)
    if (n3 == null) return "Fail 3.0: $n3"
    if (n3 != Unit) return "Fail 3.1: $n3"

    return "OK"
}