package io.github.toyota32k.dialog.task

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import io.github.toyota32k.dialog.UtDialogOwner
import io.github.toyota32k.dialog.UtDialogWeakOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull

class UtDialogOwnerStack: IUtMortalInstanceSource {
    private val list = mutableListOf<UtOwner>()
    private val ownerFlow = MutableStateFlow<UtOwner?>(null)
    inner class UtOwner(lifecycleOwner: LifecycleOwner): UtDialogWeakOwner(lifecycleOwner) {
        override fun dispose() {
            super.dispose()
            list.remove(this)
            if (ownerFlow.value === this) {
                ownerFlow.value = latest()
            }
        }
    }
    @MainThread
    fun register(owner: UtDialogOwner) {
        UtOwner(owner.lifecycleOwner).also { uo->
            if(list.find { it.lifecycleOwner === owner.lifecycleOwner }==null) {
                list.add(uo)
            }
            ownerFlow.value = uo
        }
    }
    @MainThread
    fun unregister(owner: UtDialogOwner) {
        list.find { it.lifecycleOwner === owner.lifecycleOwner }?.dispose()
    }

    @MainThread
    private fun latest(): UtOwner? {
        while(list.size>0) {
            val v = list.last()
            if(v.asDialogOwner!=null) {
                return v
            }
            v.dispose()
        }
        return null
    }

    override suspend fun getOwner(): UtDialogOwner {
        return ownerFlow.mapNotNull { it?.asDialogOwner }.first()
    }

    override suspend fun getOwnerOf(clazz:Class<*>): UtDialogOwner {
        return ownerFlow.mapNotNull {
            val owner = it?.asDialogOwner
            if (owner != null && clazz.isAssignableFrom(owner.lifecycleOwner::class.java)) owner else null
        }.first()
    }

    override suspend fun getOwnerBy(filter: (LifecycleOwner) -> Boolean) : UtDialogOwner {
        return ownerFlow.mapNotNull {
            val owner = it?.asDialogOwner
            if (owner != null && filter(owner.lifecycleOwner)) owner else null
        }.first()
    }

    @MainThread
    override fun getOwnerOrNull(): UtDialogOwner? {
        return ownerFlow.value?.asDialogOwner
    }
}
