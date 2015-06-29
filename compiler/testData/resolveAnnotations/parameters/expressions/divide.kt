package test

annotation class Ann(
        val b: Byte,
        val s: Short,
        val i: Int,
        val l: Long
)

Ann(1 / 1, 1 / 1, 1 / 1, 1 / 1) class MyClass

// EXPECTED: Ann(b = IntegerValueType(1): kotlin.Byte, i = IntegerValueType(1): kotlin.Int, l = IntegerValueType(1): kotlin.Long, s = IntegerValueType(1): kotlin.Short)
