import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.ticker
import kotlin.random.Random

/**
 * Deferred values provide a convenient way to transfer a single value between coroutines.
 * Channels provide a way to transfer a stream of values.
 */
fun runChannels() = runBlocking {
    println("runChannels start")

//    channelsTest()
//    channelsClosingAndIterating()
//    producer()
//    buffered()
    tickerChannel()

    println("runChannels end")
}


/**
 * A Channel is conceptually very similar to BlockingQueue.
 * One key difference is that instead of a blocking put operation it has a suspending send,
 * and instead of a blocking take operation it has a suspending receive.
 */
suspend fun channelsTest() = coroutineScope {
    val channel = Channel<Int>()
    launch {
        repeat(100) { channel.send(it) }
    }
    repeat(100) { println(channel.receive()) }
}


/**
 * Unlike a queue, a channel can be closed to indicate that no more elements are coming.
 * On the receiver side it is convenient to use a regular for loop to receive elements from the channel.
 * Conceptually, a close is like sending a special close token to the channel.
 * The iteration stops as soon as this close token is received,
 * so there is a guarantee that all previously sent elements before the close are received.
 */
suspend fun channelsClosingAndIterating() = coroutineScope {
    Channel<Int>().apply {
        launch {
            repeat(100) { send(it) }
            close()
        }
        for (a in this) println(a)
    }
}

/**
 * The pattern where a coroutine is producing a sequence of elements is quite common.
 * This is a part of producer-consumer pattern that is often found in concurrent code.
 * You could abstract such a producer into a function that takes channel as its parameter,
 * but this goes contrary to common sense that results must be returned from functions.
 *
 * There is a convenient coroutine builder named produce that makes it easy to do it right on producer side,
 * and an extension function consumeEach, that replaces a for loop on the consumer side:
 */
suspend fun producer() = coroutineScope {
    val channel = produce { repeat(100) { send(it) } }
    channel.consumeEach { println("consumed $it") }
}


/**
 * The channels shown so far had no buffer. Unbuffered channels transfer elements when sender and receiver meet each
 * other (aka rendezvous). If send is invoked first, then it is suspended until receive is invoked,
 * if receive is invoked first, it is suspended until send is invoked.
 *
 * Both Channel() factory function and produce builder take an optional capacity parameter to specify buffer size.
 * Buffer allows senders to send multiple elements before suspending, similar to the BlockingQueue with a specified
 * capacity, which blocks when buffer is full.
 *
 * The first 10 elements are added to the buffer and only then the sender suspends
 */
suspend fun buffered() = coroutineScope {
    val channel = Channel<Int>(10)
    val senderJob = launch {
        repeat(15) {
            println("sending to buffered channel $it")
            channel.send(it) // will suspend when the buffer is full
        }
    }
    delay(1000) // not receiving from the channel, just waiting
    senderJob.cancel()
}


/**
 * Ticker channel is a special rendezvous channel that produces Unit every time given delay passes since last
 * consumption from this channel.
 *
 * To create such channel use a factory method ticker.
 * To indicate that no further elements are needed use ReceiveChannel.cancel method on it.
 */
suspend fun tickerChannel() = coroutineScope {
    val channel = ticker(delayMillis = 1000, initialDelayMillis = 0)
    var i = 0
    channel.consumeEach {
        println("consumed a tick $it ${i++}")
    }
}
