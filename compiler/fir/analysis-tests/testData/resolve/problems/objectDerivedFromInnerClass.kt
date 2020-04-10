class Outer { inner class Inner }
fun test() {
    val x = object : Outer.Inner() { }
}