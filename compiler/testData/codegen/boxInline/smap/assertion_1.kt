import test.*

fun box(): String {
    massert(true)
    massert(true) {
        "test"
    }

    return "OK"
}

//SMAP
//assertion_1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 assertion_1.kt
//Assertion_1
//+ 2 assertion_2.kt
//test/Assertion_2
//*L
//1#1,25:1
//15#2,7:26
//6#2,7:33
//*E