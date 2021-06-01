package io.github.toyota32k.dialog.connector

import androidx.fragment.app.FragmentActivity
import io.github.toyota32k.dialog.UtDialogOwner

interface IUtActivityConnectorStore {
    fun getActivityConnector(immortalTaskName:String, connectorName:String): UtActivityConnector<*, *>?
}

class UtActivityConnectorStore(bank: UtActivityConnectorFactoryBank, activity:FragmentActivity) :
    IUtActivityConnectorStore {
    private val map = bank.createConnectors(activity)

    override fun getActivityConnector(immortalTaskName:String, connectorName:String): UtActivityConnector<*, *>? {
        return map[UtActivityConnectorKey(immortalTaskName,connectorName)]
    }
}

fun UtDialogOwner.asActivityConnectorStore() : IUtActivityConnectorStore? =
    lifecycleOwner as? IUtActivityConnectorStore
