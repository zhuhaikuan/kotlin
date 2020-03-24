// IGNORE_BACKEND_MULTI_MODULE: JVM_IR
// FILE: 1.kt
package test

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline fun <T, R> T.myLet(block: (T) -> R) = block(this)

// FILE: 2.kt
import test.*

fun box(): String {
    // should not have a line number for 1.kt:7; *should* have one for the lambda
    // in order to let the debugger break on it (KT-23064)
    return "O".myLet { it + "K" }
}

// FILE: 2.smap
SMAP
2.kt
Kotlin
*S Kotlin
*F
+ 1 2.kt
_2Kt
*L
1#1,10:1
7#1:11
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
7#1:11
*E
