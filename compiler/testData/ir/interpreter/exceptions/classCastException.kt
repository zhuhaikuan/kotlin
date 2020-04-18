// !LANGUAGE: +CompileTimeCalculations

@CompileTimeCalculation
fun classCastWithException(a: Any): String {
    return try {
        a as Int
        "Given value is $a and its doubled value is ${2 * a}"
    } catch (e: ClassCastException) {
        "Given value isnt't Int; Exception message: \"${e.message}\""
    }
}

@CompileTimeCalculation
fun safeClassCast(a: Any): Int {
    return (a as? String)?.length ?: -1
}

const val a1 = classCastWithException(10)
const val a2 = classCastWithException("10")

const val b1 = safeClassCast(10)
const val b2 = safeClassCast("10")