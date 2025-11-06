package io.github.toyota32k.dialog.broker

import androidx.fragment.app.FragmentActivity
import io.github.toyota32k.dialog.UtDialogOwner
import io.github.toyota32k.dialog.broker.pickers.UtCreateFilePicker
import io.github.toyota32k.dialog.broker.pickers.UtDirectoryPicker
import io.github.toyota32k.dialog.broker.pickers.UtMediaFilePicker
import io.github.toyota32k.dialog.broker.pickers.UtOpenFilePicker
import io.github.toyota32k.dialog.broker.pickers.UtOpenMultiFilePicker
import io.github.toyota32k.dialog.broker.pickers.UtOpenReadOnlyFilePicker
import io.github.toyota32k.dialog.broker.pickers.UtOpenReadOnlyMultiFilePicker

interface IUtActivityBrokerStoreProvider {
    val activityBrokers: UtActivityBrokerStore
}

fun UtDialogOwner.activityBrokerStoreProviderOrNull(): IUtActivityBrokerStoreProvider?
    = asTypeOrNull()
fun UtDialogOwner.asActivityBrokerStoreProvider(): IUtActivityBrokerStoreProvider
    = asType()
fun UtDialogOwner.asActivityBrokerStoreOrNull(): UtActivityBrokerStore?
    = asTypeOrNull<IUtActivityBrokerStoreProvider>()?.activityBrokers
fun UtDialogOwner.asActivityBrokerStore(): UtActivityBrokerStore
    = asType<IUtActivityBrokerStoreProvider>().activityBrokers

open class UtActivityBrokerStore(activity: FragmentActivity, vararg brokers: IUtActivityBroker) {
    val brokerMap = mutableMapOf<String,IUtActivityBroker>()
    init {
        activate(activity, *brokers)
    }

    fun activate(activity: FragmentActivity, vararg brokers: IUtActivityBroker): UtActivityBrokerStore {
        for(broker in brokers) {
            brokerMap[broker.javaClass.name] = broker
            broker.register(activity)
        }
        return this
    }

    inline fun <reified T> broker() : T where T: IUtActivityBroker {
        return brokerMap[T::class.java.name] as? T ?: throw IllegalStateException("broker is not initialized.") as Throwable
    }

    // BuiltIn Brokers

    val openFilePicker get() = broker<UtOpenFilePicker>()
    val openMultiFilePicker get() = broker<UtOpenMultiFilePicker>()
    val openReadOnlyFilePicker get() = broker<UtOpenReadOnlyFilePicker>()
    val openReadOnlyMultiFilePicker get() = broker<UtOpenReadOnlyMultiFilePicker>()
    val createFilePicker get() = broker<UtCreateFilePicker>()
    val directoryPicker get() = broker<UtDirectoryPicker>()
    val mediaFilePicker get() = broker<UtMediaFilePicker>()
    val permissionBroker get() = broker<UtPermissionBroker>()
    val multiPermissionBroker get() = broker<UtMultiPermissionsBroker>()

    // Custom Broker を作ったら、UtActivityBrokerStore をオーバーライドして、そのブローカーのGetterを用意すると便利
    // val cameraBroker get() = broker<MyCameraBroker>()
}

