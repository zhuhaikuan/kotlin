// !LANGUAGE: +CompileTimeCalculations

@CompileTimeCalculation
class A

@CompileTimeCalculation
fun notNullAssertion(value: Int?): String {
    return try {
        value!!
        "Value isn't null"
    } catch (e: NullPointerException) {
        "Value is null"
    }
}

@CompileTimeCalculation
fun notNullAssertionForObject(value: A?): String {
    return try {
        value!!
        "Value isn't null"
    } catch (e: NullPointerException) {
        "Value is null"
    }
}

@CompileTimeCalculation
fun notNullAssertionForSomeWrapper(value: StringBuilder?): String {
    return try {
        value!!.toString()
    } catch (e: NullPointerException) {
        "Value is null"
    }
}

const val a1 = notNullAssertion(1)
const val a2 = notNullAssertion(null)
const val b1 = notNullAssertionForObject(A())
const val b2 = notNullAssertionForObject(null)
const val c1 = notNullAssertionForSomeWrapper(StringBuilder("Some text"))
const val c2 = notNullAssertionForSomeWrapper(null)