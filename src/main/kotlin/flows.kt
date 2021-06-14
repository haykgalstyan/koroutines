import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.random.Random
import kotlin.system.measureTimeMillis


/**
 * A suspending function asynchronously returns a single value,
 * but how can we return multiple asynchronously computed values?
 * This is where Kotlin Flows come in.
 */
fun runFlows() = runBlocking {
    println("runFlows start")

//    flowTest()
//    flowCancelTest()
//    flowOperatorsAndOtherBuilders()
//    limitSizeTest()
//    terminateFlowOperators()
//    flowSequentialByDefaultTest()
//    flowContextChange()
    bufferingTest()

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
 * Intermediate flow Operators like [map], [filter] of flows are lazy.
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
suspend fun intermediateFlowOperatorsAndOtherBuilders() {
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
 * Limiting size
 */
suspend fun limitSizeTest() {
    val flowOf = flowOf("first", "second", "third", "forth", "fifth")
    val limit = 2
    println("limitSizeTest: taking only $limit")
    flowOf.take(limit).collect { println(it) }
}


/**
 * Terminal operators on flows are suspending functions that start a collection of the flow.
 * The [collect] operator is the most basic one, but there are other terminal operators, which can make it easier to:
 *   - Convert to various collections like toList and toSet.
 *   - Get the first value and ensure that a flow emits a single value.
 *   - Reduce a flow to a value with reduce and fold.
 */
suspend fun terminateFlowOperators() {
    val squareSum = (1..11)
        .asFlow()
        .map { it * it }
        .reduce { a, b -> a + b }

    println("Flow termination by [reduce]: result $squareSum")
}


/**
 * Flows are sequential by default
 *
 * Each individual collection of a flow is performed sequentially unless special operators that operate on multiple
 * flows are used. The collection works directly in the coroutine that calls a terminal operator.
 * No new coroutines are launched by default. Each emitted value is processed by all the intermediate operators from
 * upstream to downstream and is then delivered to the terminal operator after.
 */
suspend fun flowSequentialByDefaultTest() = (1..10).asFlow()
    .filter {
        println("filtering $it")
        it % 2 == 0
    }
    .map {
        println("mapping $it")
        it.toString()
    }
    .collect { println("collecting $it") }


/**
 * Flow context
 *
 * Collection of a flow always happens in the context of the calling coroutine (Context preservation)
 * regardless of the implementation details of the flow function.
 * This is the perfect default for fast-running or asynchronous code that does not care about the execution context
 * and does not block the caller.
 * However, the long-running CPU-consuming code might need to be executed in the context of Dispatchers.Default
 * and UI-updating code might need to be executed in the context of Dispatchers.Main.
 * Usually, withContext is used to change the context in the code using Kotlin coroutines, but code in the flow { ... }
 * builder has to honor the context preservation property and is not allowed to emit from a different context.
 * Hence an exception will be thrown: java.lang.IllegalStateException: Flow invariant is violated... Please refer to
 * 'flow' documentation or use 'flowOn' instead.
 * So the [flowOn] function needs to be used to change the context of the flow emission.
 *
 * Notice how flow { ... } works in the background thread, while collection happens in the main thread:
 */
suspend fun flowContextChange() {
    println("flowContextChange: start on thread: ${Thread.currentThread().name}")

    val blockingFlow = flow {
        println("flowContextChange: flow started on thread: ${Thread.currentThread().name}")
        Thread.sleep(3000L)
        println("flowContextChange: flow ended on thread: ${Thread.currentThread().name}")
        emit(Random.nextInt())
    }.flowOn(Dispatchers.IO)

    println("flowContextChange: code execution continued on thread: ${Thread.currentThread().name}")

    // collect on current context
    blockingFlow.collect {
        println("flowContextChange: collected on thread: ${Thread.currentThread().name} - result: $it ")
    }
}


/**
 *  Buffering
 *  If flow takes shorter than te processing, we can buffer it to finish sooner
 */
suspend fun bufferingTest() {
    val flowTime = 100L
    val collectionTime = 300L
    fun newFlow() = flow {
        for (i in 1..10) {
            delay(flowTime)
            emit(i)
        }
    }

    val timeUnbuffered = measureTimeMillis {
        newFlow()
            .collect {
                delay(collectionTime)
                println(it)
            }
    }
    println("Unbuffered took: $timeUnbuffered")

    val timeBuffered = measureTimeMillis {
        newFlow()
            .buffer()
            .collect {
                delay(collectionTime)
                println(it)
            }
    }
    println("Buffered took: $timeBuffered")

    println("Buffered was faster by ${timeUnbuffered - timeBuffered}")
}