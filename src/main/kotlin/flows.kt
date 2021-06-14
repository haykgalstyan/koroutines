import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull


/**
 * A suspending function asynchronously returns a single value,
 * but how can we return multiple asynchronously computed values?
 * This is where Kotlin Flows come in.
 */
fun runFlows() = runBlocking {
    println("runFlows start")

//    flowTest()
//    flowCancelTest()
    flowOperatorsAndOtherBuilders()

    println("runFlows end")
}


/**
 * To represent the stream of values that are being asynchronously computed, we can use a Flow<Int> type.
 * This does not block the thread.
 *
 * Note that the [doFlow()] function does not need to be marked with suspend modifier.
 * It is because calling the function call returns quickly and does not wait for anything.
 * The code inside a flow builder does not run until the flow is collected with [collect].
 */
suspend fun flowTest() {
    fun doFlow(): Flow<Int> = flow {
        for (i in 1..10) emit(calculate())
    }
    println("flowTest start")
    doFlow().collect { println("flowTest: calculation result - $it") }
    println("flowTest end")
}


/**
 * Flows can be canceled the usual way
 */
suspend fun flowCancelTest() {
    fun doFlow(): Flow<Int> = flow {
        for (i in 1..10) emit(calculate())
    }
    println("flowCancelTest start")
    withTimeoutOrNull(5000L) {
        doFlow().collect { println("flowTest: calculation result - $it") }
    }
}


/**
 * Operators like [map], [filter] of flows are lazy
 * Flows can be transformed with operators, just as you would with collections and sequences.
 * Intermediate operators are applied to an upstream flow and return a downstream flow.
 * These operators are cold, just like flows are. A call to such an operator is not a suspending function itself.
 * It works quickly, returning the definition of a new transformed flow.
 * The basic operators have familiar names like map and filter.
 * The important difference to sequences is that blocks of code inside these operators can call suspending functions.
 * For example, a flow of incoming requests can be mapped to the results with the map operator,
 * even when performing a request is a long-running operation that is implemented by a suspending function.
 *
 * Useful flow builders
 * [flowOf] and [asFlow] examples
 */
suspend fun flowOperatorsAndOtherBuilders() {
    val delay = 3000L

    val flowOf = flowOf("first", "second", "third", "forth", "fifth")
        .map { "$it ${calculate()}" }
    println("Going to start the flowOf collection in ${delay}ms")
    delay(delay)
    flowOf.collect { println("flowOf: $it") }

    val asFlow = (1..5)
        .asFlow()
        .map { "asFlow: $it: ${calculate()}" }
    println("Going to start the asFlow collection in ${delay}ms")
    delay(delay)
    asFlow.collect { println(it) }
}


/**
 *
 */
