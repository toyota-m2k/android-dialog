package io.github.toyota32k.dialog.connector

import io.github.toyota32k.dialog.task.UtImmortalTaskBase
import io.github.toyota32k.dialog.task.UtImmortalTaskManager
import kotlinx.coroutines.withContext
import kotlin.coroutines.suspendCoroutine

/**
 * ActivityConnectorを利用する ImmortalTaskのベースクラス
 */
abstract class UtActivityConnectorImmortalTaskBase(taskName:String) : UtImmortalTaskBase(taskName) {

    /**
     * ImmortalTask内で、名前で指定したコネクタを実行する。
     *
     */
    suspend fun launchActivityConnector(connectorName:String, launch:(UtActivityConnector<*,*>)->Unit) : Any? {
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
                    launch(connector)
                }
            }
        }
        logger.debug("dialog closed")
        return r
    }

    protected suspend inline fun <reified O> launchActivityConnector(connectorName: String) : O? {
        return launchActivityConnector(connectorName) { connector->
            connector.launch()
        } as? O
    }

    protected suspend inline fun <I, reified O> launchActivityConnector(connectorName: String, arg:I) : O? {
        return launchActivityConnector(connectorName) { connector->
            @Suppress("UNCHECKED_CAST")
            (connector as UtActivityConnector<I,O>).launch(arg)
        } as? O
    }
}