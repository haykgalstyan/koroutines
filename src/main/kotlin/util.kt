import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * @delay is a suspending function that suspends the coroutine for specified time.
 * Suspending a coroutine does not block the thread and allows other coroutines to run in it.
 */
suspend fun calculate(): Int {
    delay(2000L)
    return Random.nextInt(100)
}