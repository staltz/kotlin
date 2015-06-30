import zzz.*

fun box(): String {
    var (p, l) = A(1, 11)

    return if (p == 1 && l == 11) "OK" else "fail: $p"
}

//SMAP
//inlineComponent_1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 inlineComponent_1.kt
//InlineComponent_1
//+ 2 inlineComponent_2.kt
//zzz/InlineComponent_2
//*L
//1#1,21:1
//5#2,3:22
//*E