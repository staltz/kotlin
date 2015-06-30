fun box(): String {
    return test {
        "K"
    }
}

inline fun test(p: () -> String): String {
    var pd = ""
    pd = "O"
    return pd + p()
}
//TODO should be empty
//SMAP
//oneFile_1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 oneFile_1.kt
//OneFile_1
//*L
//1#1,22:1
//*E