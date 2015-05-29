fun <T> foo(f: () -> Collection<T>, p: (T) -> Boolean): Collection<T> = throw Exception()

fun test() {
    val r: Collection<Int> = foo(<selection>{ emptyList() }</selection>, { x -> x > 0 })
}