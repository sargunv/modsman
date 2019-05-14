package modsman

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowViaChannel
import java.util.concurrent.ExecutorService

@FlowPreview
internal inline fun <A, B> Collection<A>.toFlow(crossinline transform: suspend (A) -> B) = flow {
    forEach { a -> emit(transform(a)) }
}

@FlowPreview
internal inline fun <A, B> Collection<A>.parallelMapNotNullToFlow(
    executor: ExecutorService,
    crossinline transform: suspend (A) -> B?
): Flow<B> {
    return flowViaChannel { channel ->
        val jobs = map { a ->
            launch(executor.asCoroutineDispatcher()) {
                val b = transform(a) ?: return@launch
                channel.send(b)
            }
        }
        runBlocking { jobs.joinAll() }
        channel.close()
    }
}

internal suspend inline fun <T> io(noinline block: suspend CoroutineScope.() -> T): T {
    return withContext(Dispatchers.IO, block)
}
