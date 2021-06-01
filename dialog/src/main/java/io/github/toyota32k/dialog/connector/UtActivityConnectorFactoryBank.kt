package io.github.toyota32k.dialog.connector

import androidx.fragment.app.FragmentActivity

class UtActivityConnectorFactoryBank(val factoryList:Array<ActivityConnectorFactory<*, *>>) {

    abstract class ActivityConnectorFactory<I,O>(
        val key: UtActivityConnectorKey,
        val defArg:I
    ) {
        abstract fun createPicker(activity: FragmentActivity): UtActivityConnector<I, O>
    }

    fun createConnectors(activity: FragmentActivity) : Map<UtActivityConnectorKey, UtActivityConnector<*, *>> {
        return mutableMapOf<UtActivityConnectorKey, UtActivityConnector<*, *>>().also { map ->
            for (g in factoryList) {
                map[g.key] = g.createPicker(activity)
            }
        }
    }

    fun createConnectorStore(activity:FragmentActivity): UtActivityConnectorStore {
        return UtActivityConnectorStore(this,activity)
    }
}