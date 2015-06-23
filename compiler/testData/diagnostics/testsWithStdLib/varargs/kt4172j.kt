// FILE: A.java
public class A {
    public static void main(String[] args) {}
}

// FILE: B.kt
fun main(args: Array<String>) {
    A.main(arrayOf())
}