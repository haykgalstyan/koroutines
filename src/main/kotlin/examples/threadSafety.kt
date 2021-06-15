import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis


/**
 * Coroutines can be executed on multiple threads using a multi-threaded dispatcher like the Dispatchers.Default.
 * It presents all the usual parallelism problems.
 * The main problem being synchronization of access to shared mutable state.
 * Some solutions to this problem in the land of coroutines are similar to the solutions in the multi-threaded world,
 * but others are unique.
 *
 * There is also actors support which is not included here
 * See docs: https://kotlinlang.org/docs/shared-mutable-state-and-concurrency.html#actors
 */
fun runThreadSafety() = runBlocking {
    println("runThreadSafety start")

    testConcurrentModificationOfSharedMutableState()
    testConcurrentModificationOfSharedMutableVolatileState()
    testSolutionAtomic()
    testSolutionThreadConfinementFineGrained()
    testSolutionThreadConfinementCoarseGrained()
    testSolutionMutex()

    println("runThreadSafety end")
}


/**
 * Launch 100 coroutines using multi-threaded context (Dispatchers.Default),
 * modifying (incrementing) shared mutable state 100 times each.
 *
 * Expected result is 10000 (100 * 100), but it is highly unlikely because hundred coroutines increment the counter
 * concurrently from multiple threads without any synchronization.
 */
suspend fun testConcurrentModificationOfSharedMutableState() = coroutineScope {
    val n = 100
    var count = 0
    repeat(n) { launch(Dispatchers.Default) { repeat(n) { count++ } } }
    println("Concurrent modification to shared mutable state: Result: $count, expected: ${n * n}")
}


/**
 * There is a common misconception that making a variable volatile solves the problem.
 *
 * This code works slower, but we still don't get the expected result, because volatile variables guarantee atomic
 * reads and writes to the corresponding variable, but do not provide atomicity on larger actions like increment.
 */
@Volatile
var count = 0
suspend fun testConcurrentModificationOfSharedMutableVolatileState() = coroutineScope {
    val n = 100
    repeat(n) { launch(Dispatchers.Default) { repeat(n) { count++ } } }
    println("Concurrent modification to shared mutable volatile state: Result: $count, expected: ${n * n}")
}


/**
 * Thread-safe Data structures
 *
 * The general solution that works both for threads and for coroutines is to use a thread-safe (aka synchronized,
 * linearizable, or atomic) data structure that provides all the necessary synchronization for the corresponding
 * operations that needs to be performed on a shared state. In the case of a simple counter we can use AtomicInteger
 * class which has atomic incrementAndGet operations:
 */
suspend fun testSolutionAtomic() = coroutineScope {
    val n = 100
    val count = AtomicInteger()
    val time = measureTimeMillis {
        repeat(n) { launch(Dispatchers.Default) { repeat(n) { count.getAndIncrement() } } }
    }
    println("Concurrent modification to atomic: Result: $count, expected: ${n * n}, time: ${time}ms")
}


/**
 * Thread confinement (fine-grained)
 *
 * In practice, thread confinement is performed in large chunks, e.g. big pieces of state-updating business logic are
 * confined to the single thread. The following example does it like that, running each coroutine in the
 * single-threaded context to start with.
 */
suspend fun testSolutionThreadConfinementFineGrained() = coroutineScope {
    val singleThreadContext = newSingleThreadContext("")
    val n = 100
    var count = 0
    val time = measureTimeMillis {
        withContext(Dispatchers.IO) {
            repeat(n) {
                launch {
                    repeat(n) {
                        withContext(singleThreadContext) {
                            count++
                        }
                    }
                }
            }
        }
    }
    println("Concurrent modification with fine grained thread confinement: $count, expected: ${n * n}, time: ${time}ms")
}


/**
 * Thread confinement (coarse-grained)
 *
 * Using single-threaded context.
 * This is very slow, because it does fine-grained thread-confinement. Each individual increment switches from
 * multi-threaded context to the single-threaded context using withContext(counterContext) block.
 *
 * This is much faster and produces the correct result.
 */
suspend fun testSolutionThreadConfinementCoarseGrained() = coroutineScope {
    val singleThreadContext = newSingleThreadContext("")
    val n = 100
    var count = 0
    val time = measureTimeMillis {
        withContext(singleThreadContext) {
            repeat(n) {
                launch {
                    repeat(n) {
                        count++
                    }
                }
            }
        }
    }
    println("Concurrent modification with coarse grained thread confinement: $count, expected: ${n * n}, time: ${time}ms")
}


/**
 * Mutual exclusion (Mutex)
 * Mutual exclusion solution to the problem is to protect all modifications of the shared state with a critical section
 * that is never executed concurrently. In a blocking world you'd typically use synchronized or ReentrantLock for that.
 * Coroutine's alternative is called Mutex. It has lock and unlock functions to delimit a critical section.
 * The key difference is that Mutex.lock() is a suspending function. It does not block a thread.
 *
 * There is also withLock extension function that conveniently represents mutex.lock();
 * try { ... } finally { mutex.unlock() } pattern
 *
 * The locking in this example is fine-grained, so it pays the price.
 * However, it is a good choice for some situations where you absolutely must modify some shared state periodically,
 * but there is no natural thread that this state is confined to.
 */
suspend fun testSolutionMutex() = coroutineScope {
    val mutex = Mutex()
    val n = 100
    var count = 0
    val time = measureTimeMillis {
        withContext(Dispatchers.Default) {
            repeat(n) {
                launch {
                    repeat(n) {
                        mutex.withLock { count++ }
                    }
                }
            }
        }
    }
    println("Concurrent modification with fine grained thread confinement: $count, expected: ${n * n}, time: ${time}ms")
}