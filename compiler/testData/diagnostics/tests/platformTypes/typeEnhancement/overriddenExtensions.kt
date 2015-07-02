// FILE: A.kt
open class A {
    open fun String.foo(y: String?): Int = 1
    open fun String?.bar(y: String): Int = 1
}

// FILE: B.java
import org.jetbrains.annotations.*;

class B extends A {
    @Override
    int foo(String x, String y);
    @Override
    int bar(String x, String y);
}

// FILE: B1.java
import org.jetbrains.annotations.*;

class B1 extends A {
    @Override
    int foo(@NotNull String x, String y);
    @Override
    int bar(@Nullable String x, String y);
}

// FILE: C.java
import org.jetbrains.annotations.*;

class C extends A {
    @Override
    int foo(@Nullable String x, @NotNull String y);
    @Override
    int bar(@NotNull String x, @Nullable String y);
}

// FILE: D.java
import org.jetbrains.annotations.*;

class D extends B {
    @Override
    int foo(@Nullable String x, @Nullable String y);
    @Override
    int bar(@NotNull String x, @NotNull String y);
}
