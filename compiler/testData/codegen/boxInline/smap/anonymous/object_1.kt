import builders.*
import kotlin.InlineOption.*

fun test(): String {
    var res = "Fail"

    call {
        res = "OK"
    }

    return res
}


fun box(): String {
    return test()
}
//NO_CHECK_LAMBDA_INLINING
//SMAP
//object_1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 object_1.kt
//Object_1
//+ 2 object_2.kt
//builders/Object_2
//*L
//1#1,45:1
//4#2,5:46
//*E
//
//SMAP
//object_2.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 object_2.kt
//builders/Object_2$call$1
//+ 2 object_1.kt
//Object_1
//*L
//1#1,21:1
//8#2:22
//*E