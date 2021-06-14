import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking


/**
 * A suspending function asynchronously returns a single value,
 * but how can we return multiple asynchronously computed values?
 * This is where Kotlin Flows come in.
 */
fun runFlows() = runBlocking {
    println("runFlows start")

    flowTest()

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
suspend fun flowTest() = coroutineScope {
    fun doFlow(): Flow<Int> = flow {
        for (i in 1..10) emit(calculate())
    }
    println("flowTest start")
    doFlow().collect { println("flowTest: calculation result - $it") }
    println("flowTest end")
}


/**
 *
 */
