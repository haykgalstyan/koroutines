import kotlinx.coroutines.runBlocking

fun runThreadSafety() = runBlocking {
    println("runThreadSafety start")


    println("runThreadSafety end")
}