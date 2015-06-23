// FILE: A.java

public @interface A {
    int x();

    String y();
}

// FILE: B.kt

A(x = 1, y = "2")
fun test() {}
