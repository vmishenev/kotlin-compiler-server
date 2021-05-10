class Outer {
    inner class Inner
}

val o = Outer()
val boundInnerCtor = o::Inner