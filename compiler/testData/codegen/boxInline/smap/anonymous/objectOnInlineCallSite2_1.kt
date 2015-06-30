import builders.*
import kotlin.InlineOption.*

fun box(): String {
    return test()
}
//NO_CHECK_LAMBDA_INLINING

//SXMAP
//objectOnInlineCallSite2_1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 objectOnInlineCallSite2_1.kt
//_DefaultPackage
//+ 2 objectOnInlineCallSite2_2.kt
//builders/BuildersPackage
//*L
//1#1,32:1
//8#2,11:33
//*E
//
//SXMAP
//objectOnInlineCallSite2_2.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 objectOnInlineCallSite2_2.kt
//builders/BuildersPackage$objectOnInlineCallSite2_2$HASH$test$1$1
//*L
//1#1,42:1
//*E