import builders.*
import kotlin.InlineOption.*

fun test(): String {
    var res = "Fail"

    call {
        {
            res = "OK"
        }()
    }

    return res
}


fun box(): String {
    return test()
}


//SMAP
//lambdaOnCallSite_1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 lambdaOnCallSite_1.kt
//LambdaOnCallSite_1
//+ 2 lambdaOnCallSite_2.kt
//builders/LambdaOnCallSite_2
//*L
//1#1,34:1
//4#2:35
//*E