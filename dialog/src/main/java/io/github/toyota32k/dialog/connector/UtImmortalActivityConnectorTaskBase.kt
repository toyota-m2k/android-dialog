package io.github.toyota32k.dialog.connector

import io.github.toyota32k.dialog.task.UtImmortalTaskBase
import io.github.toyota32k.dialog.task.UtImmortalTaskManager
import kotlinx.coroutines.withContext
import kotlin.coroutines.suspendCoroutine

abstract class UtImmortalActivityConnectorTaskBase(taskName:String) : UtImmortalTaskBase(taskName) {

    private suspend fun <I,O> launchActivityConnector(connectorName:String, launch:(UtActivityConnector<I, O>)->Unit) : O? {
        val running = UtImmortalTaskManager.taskOf(taskName)
        if(running == null || running.task != this) {
            throw IllegalStateException("task($taskName) is not running")
        }
        logger.debug("dialog opening...")
        @Suppress("UNCHECKED_CAST")
        val r = withContext(UtImmortalTaskManager.immortalTaskScope.coroutineContext) {
            withOwner { owner->
                val store = owner.asActivityConnectorStore() ?: throw IllegalStateException("task owner must be IUtActivityConnectorStore.")
                val connector = store.getActivityConnector(taskName, connectorName) ?: throw IllegalStateException("no such connector: '$connectorName'")
                suspendCoroutine<Any?> {
                    continuation = it
                    launch(connector as UtActivityConnector<I, O>)
                } as O?
            }
        }
        logger.debug("dialog closed")
        return r
    }

    suspend fun <I,O> launchActivityConnector(connectorName: String) : O? {
        return launchActivityConnector<I,O>(connectorName) { connector->
            connector.launch()
        }
    }
    @Suppress("unused")
    suspend fun <I, O> launchActivityConnector(connectorName: String, arg:I) : O? {
        return launchActivityConnector<I,O>(connectorName) { connector->
            connector.launch(arg)
        }
    }

}