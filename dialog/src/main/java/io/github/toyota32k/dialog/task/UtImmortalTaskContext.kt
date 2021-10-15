package io.github.toyota32k.dialog.task

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

interface IUtImmortralTaskContextSource {
    val immortalTaskContext:IUtImmortalTaskContext
    val immortalCoroutineScope get() = immortalTaskContext.coroutineScope
}
interface IUtImmortralTaskMutableContextSource : IUtImmortralTaskContextSource {
    override var immortalTaskContext:IUtImmortalTaskContext
}

interface IUtImmortalTaskContext: ViewModelStoreOwner {
    val taskName:String
    val coroutineScope:CoroutineScope
}

class UtImmortalTaskContext(override val taskName:String) : IUtImmortalTaskContext {
    private var mScope: CoroutineScope? = null
    override val coroutineScope:CoroutineScope
        get() {
            return mScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main).apply { mScope=this }
        }

    private var mViewModelStore: ViewModelStore? = null
    override fun getViewModelStore(): ViewModelStore {
        return mViewModelStore ?: ViewModelStore().apply { mViewModelStore = this }
    }

    fun close() {
        mScope?.cancel()
        mScope = null
        mViewModelStore?.clear()
        mViewModelStore = null
    }
}