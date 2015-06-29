package test

annotation class Ann(
        val b1: Boolean,
        val b2: Boolean,
        val b3: Boolean,
        val b4: Boolean,
        val b5: Boolean
)

Ann(1 == 2, 1.0 == 2.0, 'b' == 'a', "a" == "b", "a" == "a") class MyClass

// EXPECTED: Ann(b1 = false: kotlin.Boolean, b2 = false: kotlin.Boolean, b3 = false: kotlin.Boolean, b4 = false: kotlin.Boolean, b5 = true: kotlin.Boolean)
