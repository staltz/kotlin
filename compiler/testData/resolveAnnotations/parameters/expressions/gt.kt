package test

annotation class Ann(
        val b1: Boolean,
        val b2: Boolean,
        val b3: Boolean,
        val b4: Boolean,
        val b5: Boolean,
        val b6: Boolean
)

val a = 1
val b = 2

Ann(1 > 2, 1.0 > 2.0, 2 > a, b > a, 'b' > 'a', "a" > "b") class MyClass

// EXPECTED: Ann(b1 = false: kotlin.Boolean, b2 = false: kotlin.Boolean, b3 = true: kotlin.Boolean, b4 = true: kotlin.Boolean, b5 = true: kotlin.Boolean, b6 = false: kotlin.Boolean)
