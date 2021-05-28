package io.github.toyota32k.dialog.task

import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

class UtDialogOwnerStack: IUiMortalInstanceSource {
    private val list = mutableListOf<UtOwner>()
    private val ownerFlow = MutableStateFlow<UtOwner?>(null)
    private val mutex = Mutex()
    inner class UtOwner(lifecycleOwner: LifecycleOwner): io.github.toyota32k.dialog.UtDialogWeakOwner(lifecycleOwner) {
        override fun dispose() {
            super.dispose()
            list.remove(this)
            if(ownerFlow.value == this) {
                ownerFlow.value = null
            }
        }
    }
    fun push(owner: io.github.toyota32k.dialog.UtDialogOwner) {
        UtOwner(owner.lifecycleOwner).also {
            list.add(it)
            ownerFlow.value = it
        }
    }
    fun remove(owner: io.github.toyota32k.dialog.UtDialogOwner) {
        list.find {it.lifecycleOwner==owner.lifecycleOwner}?.dispose()
    }
    private fun latest(): io.github.toyota32k.dialog.UtDialogOwner? {
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

    private suspend fun peekOne(): io.github.toyota32k.dialog.UtDialogOwner {
        return latest() ?: ownerFlow.mapNotNull { it?.asDialogOwner }.first()
    }

    private var currentClient = AtomicInteger(0)

    override suspend fun <T> withOwner(ticket:Any?, fn: suspend (Any, io.github.toyota32k.dialog.UtDialogOwner)->T):T {
        if(currentClient.get()==ticket) {
            return fn(ticket, peekOne())
        }
        return mutex.withLock {
            val result = fn(currentClient.incrementAndGet(), peekOne())
            result
        }
    }

    private suspend fun peekOne(clazz:Class<*>): io.github.toyota32k.dialog.UtDialogOwner {
        return latest() ?: ownerFlow.mapNotNull{
            val lo = it?.lifecycleOwner
            if(lo!=null && lo::class.java==clazz) it.asDialogOwner else null }.first()
    }

    override suspend fun <T> withOwner(clazzSpecified:Class<*>, ticket:Any?, fn: suspend (Any, io.github.toyota32k.dialog.UtDialogOwner)->T):T {
        if(currentClient.get()==ticket) {
            return fn(ticket, peekOne(clazzSpecified))
        }
        return mutex.withLock {
            val result = fn(currentClient.incrementAndGet(), peekOne(clazzSpecified))
            result
        }
    }
}
