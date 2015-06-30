import builders.*
import kotlin.InlineOption.*

fun test(): String {
    var res = "Fail"

    call {
        object {
            fun run () {
                res = "OK"
            }
        }.run()
    }

    return res
}


fun box(): String {
    return test()
}
//NO_CHECK_LAMBDA_INLINING

//SMAP
//objectOnCallSite_1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 objectOnCallSite_1.kt
//ObjectOnCallSite_1
//+ 2 objectOnCallSite_2.kt
//builders/ObjectOnCallSite_2
//*L
//1#1,36:1
//4#2:37
//*E