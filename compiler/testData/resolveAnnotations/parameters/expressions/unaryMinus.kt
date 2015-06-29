package test

annotation class Ann(
        val b1: Byte,
        val b2: Short,
        val b3: Int,
        val b4: Long,
        val b5: Double,
        val b6: Float,
        val b7: Char
)

Ann(-1, -1, -1, -1, -1.0, -1.0.toFloat(), -'c') class MyClass

// EXPECTED: Ann(b1 = IntegerValueType(-1): kotlin.Byte, b2 = IntegerValueType(-1): kotlin.Short, b3 = IntegerValueType(-1): kotlin.Int, b4 = IntegerValueType(-1): kotlin.Long, b5 = -1.0.toDouble(): kotlin.Double, b6 = -1.0.toFloat(): kotlin.Float, b7 = IntegerValueType(-99): kotlin.Char)
