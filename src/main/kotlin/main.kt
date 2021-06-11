/**
 * Coroutine examples
 * Read more here:
 * https://kotlinlang.org/docs/coroutines-guide.html
 */
import kotlinx.coroutines.*
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * We can launch coroutines only in a coroutine scope (structured concurrency).
 * In this case we use a blocking scope to block the current thread until all the children coroutines finish.
 */
fun main(): Unit = runBlocking {
    println("main start")

//    launch {
//        val result = calculate()
//        println("main result 1: $result")
//    }
//    launch {
//        val result = calculate()
//        println("main result 2: $result")
//    }
//    doSameInMainJustInAFunction()
//    launchAndJobWait()
//    launchAndJobCancel()
//    launchAndCancelCooperatively()
//    launchWithTimeout()
    asyncVsLaunchConcurrencyTest()

    println("main end")
}


/**
 * We can launch coroutines only in a coroutine scope (structured concurrency).
 * In this case we use a non-blocking scope which just suspends, releasing the underlying thread for other usages.
 */
suspend fun doSameInMainJustInAFunction() = coroutineScope {
    println("func start")
    launch {
        val result = calculate()
        println("func result 1: $result")
    }
    launch {
        val result = calculate()
        println("func result 2: $result")
    }
    println("func end")
}

/**
 * @launch is a coroutine builder which returns a job object
 * which is a handle to the launched coroutine and can be used to explicitly wait for its completion.
 */
suspend fun launchAndJobWait() = coroutineScope {
    val job = launch {
        println("job wait start")
        calculate()
    }
    job.join()
    println("job wait done")
}

/**
 * @launch is a coroutine builder which returns a job object
 * which is a handle to the launched coroutine and can be used to cancel the running coroutine.
 * There is also a [kotlinx.coroutines.Job] extension cancelAndJoin()
 *
 * We use [delay] instead of [calculate] here because coroutine cancellation is cooperative and needs to be implemented
 * if a coroutine is working in a computation and does not check for cancellation, then it cannot be cancelled.
 * It is implemented in all the suspending functions in [kotlinx.coroutines]
 */
suspend fun launchAndJobCancel() = coroutineScope {
    println("job cancel start")
    val job = launch {
        repeat(10_000_000) {
            println("job cancel - just doing my job")
            delay(500L)
        }
    }
    delay(1300)
    job.cancelAndJoin()
    job.join()
    println("job cancel done")
}

/**
 * There are two approaches to making computation code cancellable.
 * The first one is to periodically invoke a suspending function that checks for cancellation.
 * The [yield] function is a good choice for that purpose.
 * The other one is to explicitly check the cancellation status.
 * Here is the latter approach.
 */
suspend fun launchAndCancelCooperatively() = coroutineScope {
    val job = launch {
        while (isActive) {
            println("Just doing my job cooperatively until canceled")
            calculate()
        }
    }
    delay(5000L)
    println("Cancelling cooperative")
    job.cancelAndJoin()
    println("Cooperative respectfully canceled")
}

/**
 * Cancel execution of a coroutine on timeout with [withTimeout] which throws a [TimeoutCancellationException]
 * and [withTimeoutOrNull] which returns null on timeout or the returned result otherwise.
 *
 * Note that timeout can happen at any part of the executing code block, for example before return.
 * See doc for more info on that:
 * https://kotlinlang.org/docs/cancellation-and-timeouts.html#asynchronous-timeout-and-resources
 */
suspend fun launchWithTimeout() = coroutineScope {
    val timeout = Random.nextLong(3000L)
    val delay = Random.nextLong(3000L)
    try {
        withTimeout(timeout) {
            delay(delay)
            println("WithTimeout finished until timeout")
        }
    } catch (e: TimeoutCancellationException) {
        println("WithTimeout timed out")
    }

    val result = withTimeoutOrNull(timeout) {
        delay(delay)
        "WithTimeoutOrNull finished until timeout"
    }
    println(result ?: "WithTimeoutOrNull timed out")
}

/**
 * There is a common misconception that coroutines built with [async] builder are concurrent
 * while the ones built with [launch] are sequential. that is incorrect, here's a test.
 *
 * The difference is with async you get a result returned, also there are differences how they handle exceptions
 * See more in the documentation.
 */
suspend fun asyncVsLaunchConcurrencyTest() {
    println("Async vs Launch test")
    val timeAsync = measureTimeMillis { doAsync() }
    val timeLaunch = measureTimeMillis { doLaunch() }
    println("Async: $timeAsync\nLaunch: $timeLaunch")
}

suspend fun doAsync() = coroutineScope {
    val calculations = mutableListOf<Deferred<Int>>()
    repeat(10) {
        val calculation = async { calculate() }
        calculations.add(calculation)
    }
    calculations.forEach {
        val result = it.await()
        println("Async result $result")
    }
}

suspend fun doLaunch() = coroutineScope {
    val calculations = mutableListOf<Job>()
    repeat(10) {
        val calculation = launch { calculate() }
        calculations.add(calculation)
    }
    calculations.forEach {
        it.join()
        println("Launch result done")
    }
}

/**
 * @delay is a suspending function that suspends the coroutine for specified time.
 * Suspending a coroutine does not block the thread and allows other coroutines to run in it.
 */
suspend fun calculate(): Int {
    delay(2000L)
    return Random.nextInt(100)
}