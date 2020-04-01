fun box() {
    bar {
        foo()
    }
}

inline fun foo() {
    null!!
}

inline fun bar(crossinline x: () -> Unit) = object {
    fun run() = x()
}.run()

// NAVIGATE_TO_CALL_SITE
// FILE: inlineFunCallSiteInLambdaInlinedIntoObject.kt
// LINE: 3
