package test

annotation class Ann(
        val b: Byte,
        val s: Short,
        val i: Int,
        val l: Long
)

Ann(1 + 1, 1 + 1, 1 + 1, 1 + 1) class MyClass

// EXPECTED: Ann(b = IntegerValueType(2): kotlin.Byte, i = IntegerValueType(2): kotlin.Int, l = IntegerValueType(2): kotlin.Long, s = IntegerValueType(2): kotlin.Short)
