fun test1() {}

fun test2() {
    test1()
}

// 2 INVOKESTATIC A1.test1 \(\)V
// 1 INVOKESTATIC A1.test2 \(\)V
