package test

public inline fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()

class A

open var <caret>A.p: Int
    get() = 1
    set(value: Int) {}

fun test() {
    val t = A().p
    A().p = 1
}