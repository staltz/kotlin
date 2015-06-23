public fun <T> Iterable<T>.myforEach(operation: (T) -> Unit) : Unit {
    for (element in this) operation(element)
}

fun foo1() {
    (1..5).myforEach {
        println(it)
    }
}

// METHOD : ItInLambda$foo1$1.invoke(I)V
// VARIABLE : NAME=this TYPE=LItInLambda$foo1$1; INDEX=0
// VARIABLE : NAME=it TYPE=I INDEX=1
