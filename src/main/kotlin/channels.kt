import kotlinx.coroutines.runBlocking

/**
 * Deferred values provide a convenient way to transfer a single value between coroutines.
 * Channels provide a way to transfer a stream of values.
 */
fun runChannels() = runBlocking {
    println("runChannels start")

    channelsTest()

    println("runChannels end")
}

fun channelsTest() {
    TODO("Not yet implemented")
}
