package io.github.toyota32k.dialog.broker

import io.github.toyota32k.dialog.broker.pickers.UtOpenFilePicker
import kotlin.enums.EnumEntries

class UtActivityBrokerGenericStore : UtActivityBrokerStore() {
    class Builder {
        private val list = mutableListOf<IUtActivityBroker>()

        enum class Broker(val creator: () -> IUtActivityBroker) {
            OpenFilePicker(creator = { UtOpenFilePicker() }),
            OpenMultiFilePicker { UtOpenMultiFilePicker() },
            OpenReadOnlyFilePicker,
            OpenReadOnlyMultiFilePicker,
            CreateFilePicker,
            PermissionBroker,
        }

        fun reserve(broker:Broker) {
            list.add()
        }
        fun reserve(brokers: EnumEntries<Broker>) {
            for(broker in brokers) {
                reserve(broker)
            }
        }
    }




    override val brokerList: List<IUtActivityBroker>
        get() = mBrokerList
}