// This test checks that backend doesn't fail when enumerating ancestors of class C, where the interface A can be found both in the sources
// of the current module, and in dependencies.
// In particular, JVM IR might "resolve" fake overrides to determine whether it needs to generate a DefaultImpls accessor
// (which in itself is probably incorrect), but without proper disambiguation, this fails on class C, since two real declarations
// for `foo` are visible there: one from the source class A, and another from the class A from dependency.

// FILE: A.kt

interface A {
    fun foo() {}
}

interface AImpl : A

// FILE: B.kt

var state = "Fail"

interface A {
    fun foo() {
        state = "OK"
    }
}

interface B : A, AImpl

interface C : B

fun box(): String {
    object : C {}.foo()
    return state
}
