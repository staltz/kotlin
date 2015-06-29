package test

annotation class Ann(
        val b1: Double,
        val b2: Double,
        val b3: Double,
        val b4: Double
)

Ann(1.0, 1.toDouble(), 1.7976931348623157E309.toDouble(), 1.7976931348623157E309) class MyClass

// EXPECTED: Ann(b1 = 1.0.toDouble(): kotlin.Double, b2 = 1.0.toDouble(): kotlin.Double, b3 = Infinity.toDouble(): kotlin.Double, b4 = Infinity.toDouble(): kotlin.Double)