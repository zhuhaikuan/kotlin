// !DIAGNOSTICS: -UNUSED_VARIABLE

fun foo() {
    <!EXPOSED_FUNCTION_RETURN_TYPE, EXPOSED_FUNCTION_RETURN_TYPE!>enum class A {
        FOO,
        BAR
    }<!>
    val foo = A.FOO
    val b = object {
        <!EXPOSED_FUNCTION_RETURN_TYPE, EXPOSED_FUNCTION_RETURN_TYPE!>enum class B {}<!>
    }
    class C {
        <!EXPOSED_FUNCTION_RETURN_TYPE, EXPOSED_FUNCTION_RETURN_TYPE!>enum class D {}<!>
    }
    val f = {
        <!EXPOSED_FUNCTION_RETURN_TYPE, EXPOSED_FUNCTION_RETURN_TYPE!>enum class E {}<!>
    }

    <!EXPOSED_FUNCTION_RETURN_TYPE, EXPOSED_FUNCTION_RETURN_TYPE!>enum class<!SYNTAX!><!> {}<!>
}
