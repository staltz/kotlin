package test

annotation class Ann(p1: Int,
                     p2: Short,
                     p3: Byte,
                     p4: Int,
                     p5: Int,
                     p6: Int
                     )

Ann(1 or 1, 1 and 1, 1 xor 1, 1 shl 1, 1 shr 1, 1 ushr 1) class MyClass

// EXPECTED: Ann(p1 = IntegerValueType(1): kotlin.Int, p2 = IntegerValueType(1): kotlin.Short, p3 = IntegerValueType(0): kotlin.Byte, p4 = IntegerValueType(2): kotlin.Int, p5 = IntegerValueType(0): kotlin.Int, p6 = IntegerValueType(0): kotlin.Int)
