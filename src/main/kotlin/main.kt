/**
 * Coroutine examples
 * Read more here:
 * https://kotlinlang.org/docs/coroutines-guide.html
 *
 * CoroutineScope
 * Coroutines follow a principle of structured concurrency which means that new coroutines can be only launched in a
 * specific CoroutineScope which delimits the lifetime of the coroutine.
 * In the real application, you will be launching a lot of coroutines. Structured concurrency ensures that they are not
 * lost and do not leak. An outer scope cannot complete until all its children coroutines complete.
 * Structured concurrency also ensures that any errors in the code are properly reported and are never lost.
 * In addition to the coroutine scope provided by different builders, it is possible to declare your own scope using the
 * coroutineScope builder. It creates a coroutine scope and does not complete until all launched children complete.
 * runBlocking and coroutineScope builders may look similar because they both wait for their body and all its children
 * to complete. The main difference is that the runBlocking method blocks the current thread for waiting, while
 * coroutineScope just suspends, releasing the underlying thread for other usages. Because of that difference,
 * runBlocking is a regular function and coroutineScope is a suspending function.
 * You can use coroutineScope from any suspending function.
 *
 * CoroutineContext
 * Coroutines always execute in some context represented by a value of the CoroutineContext type, defined in the Kotlin
 * standard library.
 * The coroutine context is a set of various elements. The main elements are the Job of the coroutine and its dispatcher.
 * When a coroutine is launched in the CoroutineScope of another coroutine, it inherits its context via
 * CoroutineScope.coroutineContext and the Job of the new coroutine becomes a child of the parent coroutine's job.
 * When the parent coroutine is cancelled, all its children are recursively cancelled, too.
 * However, this parent-child relation can be explicitly overriden in one of two ways:
 * When a different scope is explicitly specified when launching a coroutine (for example, GlobalScope.launch ),
 * then it does not inherit a Job from the parent scope.
 * When a different Job object is passed as the context for the new coroutine,
 * then it overrides the Job of the parent scope.
 * In both cases, the launched coroutine is not tied to the scope it was launched from and operates independently.
 * A parent coroutine always waits for completion of all its children.
 * A parent does not have to explicitly track all the children it launches,
 * and it does not have to use Job.join to wait for them at the end:
 *
 * Dispatcher
 * The coroutine context includes a coroutine dispatcher (see CoroutineDispatcher) that determines what thread or threads
 * the corresponding coroutine uses for its execution. The coroutine dispatcher can confine coroutine execution to a
 * specific thread, dispatch it to a thread pool, or let it run unconfined.
 * All coroutine builders like launch and async accept an optional CoroutineContext parameter that can be used to
 * explicitly specify the dispatcher for the new coroutine and other context elements.
 *
 *
 */
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
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
//    asyncVsLaunchConcurrencyTest()
//    dispatchersAndThreadsTest()
    scopeCancel()

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
 * Dispatchers specify mapping to threads
 * Results:
 * Unspecified, Thread: main
 * Unconfined,  Thread: main
 * Default,     Thread: DefaultDispatcher-worker-1
 * IO,          Thread: DefaultDispatcher-worker-1
 *
 * Unspecified (Nothing passed)
 * When launch is used without parameters, it inherits the context (and thus dispatcher) from the CoroutineScope
 * it is being launched from. In this case, it inherits the context of the main runBlocking coroutine
 * which runs in the main thread.
 *
 * Unconfined
 * Dispatchers.Unconfined is a special dispatcher that also appears to run in the main thread,
 * but it is, in fact, a different mechanism that is explained later.
 *
 * Default
 * The default dispatcher that is used when no other dispatcher is explicitly specified in the scope.
 * It is represented by Dispatchers.Default and uses a shared background pool of threads.
 *
 * newSingleThreadContext creates a thread for the coroutine to run.
 * A dedicated thread is a very expensive resource.
 * In a real application it must be either released, when no longer needed, using the close function,
 * or stored in a top-level variable and reused throughout the application.
 */
suspend fun dispatchersAndThreadsTest() = coroutineScope {
    launch { println("Dispatchers: Unspecified, Thread: ${Thread.currentThread().name}") }
    launch(Dispatchers.Unconfined) { println("Dispatchers: Unconfined, Thread: ${Thread.currentThread().name}") }
    launch(Dispatchers.Default) { println("Dispatchers: Default, Thread: ${Thread.currentThread().name}") }
    launch(Dispatchers.IO) { println("Dispatchers: IO, Thread: ${Thread.currentThread().name}") }
    launch(newSingleThreadContext("SingleThread")) { println("Dispatchers: newSingleThreadContext, I'm a thread") }
    // OS Specific
    // launch(Dispatchers.Main) { println("Dispatchers: Main, Thread: ${Thread.currentThread().name}") }
}

/**
 * Canceling a scope
 * We can manage the lifecycles of our coroutines by creating an instance of CoroutineScope tied to the lifecycle of
 * our activity.
 * A CoroutineScope instance can be created by the CoroutineScope() or MainScope() factory functions.
 * The former creates a general-purpose scope, while the latter creates a scope for UI applications
 * and uses Dispatchers.Main as the default dispatcher
 */
suspend fun scopeCancel() {
    class LifeCycle {
        val scope = CoroutineScope(Dispatchers.IO)
        fun create() {
            println("scopeCancel.create")
            repeat(10000) {
                scope.launch {
                    println("scopeCancel doing my job $it")
                    delay(Random.nextLong(10))

                    // fix this exapmle

                }
            }
        }

        fun destroy() {
            println("scopeCancel.destroy")
            scope.cancel()
        }
    }

    coroutineScope {
        LifeCycle().apply {
            create()
            delay(5000L)
            destroy()
        }
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