package io.github.toyota32k.task

import androidx.lifecycle.LifecycleOwner
import io.github.toyota32k.dialog.UtDialogOwner
import io.github.toyota32k.dialog.UtDialogWeakOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

class UtDialogOwnerStack: IUiMortalInstanceSource {
    private val list = mutableListOf<UtOwner>()
    private val ownerFlow = MutableStateFlow<UtOwner?>(null)
    private val mutex = Mutex()
    inner class UtOwner(lifecycleOwner: LifecycleOwner): UtDialogWeakOwner(lifecycleOwner) {
        override fun dispose() {
            super.dispose()
            if(ownerFlow.value == this) {
                ownerFlow.value = null
            }
        }
    }
    fun push(owner:UtDialogOwner) {
        UtOwner(owner.lifecycleOwner).also {
            list.add(it)
            ownerFlow.value = it
        }
    }
    fun remove(owner: UtDialogOwner) {
        list.find {it.lifecycleOwner==owner.lifecycleOwner}?.dispose()
    }
    private fun latest(): UtDialogOwner? {
        while(list.size>0) {
            val v = list.last()
            val r = v.asDialogOwner
            if(r!=null) {
                return r
            }
            v.dispose()
        }
        return null
    }

    private suspend fun peekOne(): UtDialogOwner {
        return latest() ?: ownerFlow.mapNotNull { it?.asDialogOwner }.first()
    }

    private var currentClient = AtomicInteger(0)

    override suspend fun <T> withOwner(ticket:Any?, fn: suspend (Any, UtDialogOwner)->T):T {
        if(currentClient.get()==ticket) {
            return fn(ticket, peekOne())
        }
        return mutex.withLock {
            val result = fn(currentClient.incrementAndGet(), peekOne())
            result
        }
    }
}
