package io.github.toyota32k.dialog.broker

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

abstract class UtActivityBrokerStore {
    abstract val brokerList:List<IUtActivityBroker>

    fun onCreate(activity:FragmentActivity) {
        for(broker in brokerList) {
            broker.register(activity)
        }
    }

    fun onCreate(fragment: Fragment) {
        for(broker in brokerList) {
            broker.register(fragment)
        }
    }
}