package test

annotation class Ann(
        val b: Byte,
        val s: Short,
        val i: Int,
        val l: Long
)

Ann(1 - 1, 1 - 1, 1 - 1, 1 - 1) class MyClass

// EXPECTED: Ann(b = IntegerValueType(0): kotlin.Byte, i = IntegerValueType(0): kotlin.Int, l = IntegerValueType(0): kotlin.Long, s = IntegerValueType(0): kotlin.Short)
