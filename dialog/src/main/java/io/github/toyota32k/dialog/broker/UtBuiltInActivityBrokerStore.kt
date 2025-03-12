package io.github.toyota32k.dialog.broker

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import io.github.toyota32k.dialog.broker.pickers.UtCreateFilePicker
import io.github.toyota32k.dialog.broker.pickers.UtDirectoryPicker
import io.github.toyota32k.dialog.broker.pickers.UtOpenFilePicker
import io.github.toyota32k.dialog.broker.pickers.UtOpenMultiFilePicker
import io.github.toyota32k.dialog.broker.pickers.UtOpenReadOnlyFilePicker
import io.github.toyota32k.dialog.broker.pickers.UtOpenReadOnlyMultiFilePicker

interface IUtBuiltInActivityBrokerStoreProvider {
    val activityBrokers: UtBuiltInActivityBrokerStore
}

class UtBuiltInActivityBrokerStore : UtActivityBrokerStore() {
    /**
     * 読み書き用にファイルを選択
     */
    private var _openFilePicker: UtOpenFilePicker? = null
    val openFilePicker get() = _openFilePicker ?: throw IllegalStateException("openFilePicker is not initialized.")

    /**
     * 読み書き用に複数ファイルを選択
     */
    private var _openMultiFilePicker: UtOpenMultiFilePicker? = null
    val openMultiFilePicker get() = _openMultiFilePicker ?: throw IllegalStateException("openMultiFilePicker is not initialized.")

    /**
     * 読み取り専用にファイルを選択
     */
    private var _openReadOnlyFilePicker: UtOpenReadOnlyFilePicker? = null
    val openReadOnlyFilePicker get() = _openReadOnlyFilePicker ?: throw IllegalStateException("openReadOnlyFilePicker is not initialized.")
    /**
     * 読み取り専用に複数ファイルを選択
     */
    private var _openReadOnlyMultiFilePicker: UtOpenReadOnlyMultiFilePicker? = null
    val openReadOnlyMultiFilePicker get() = _openReadOnlyMultiFilePicker ?: throw IllegalStateException("openReadOnlyMultiFilePicker is not initialized.")

    /**
     * 名前を付けてファイルを作成
     */
    private var _createFilePicker: UtCreateFilePicker? = null
    val createFilePicker get() = _createFilePicker ?: throw IllegalStateException("createFilePicker is not initialized.")

    /**
     * ディレクトリを選択
     */
    private var _directoryPicker: UtDirectoryPicker? = null
    val directoryPicker get() = _directoryPicker ?: throw IllegalStateException("directoryPicker is not initialized.")

    /**
     * Permission
     */
    private var _permissionBroker: UtPermissionBroker? = null
    val permissionBroker get() = _permissionBroker ?: throw IllegalStateException("permissionBroker is not initialized.")

    /**
     * MultiPermission
     */
    private var _multiPermissionBroker: UtMultiPermissionsBroker? = null
    val multiPermissionBroker get() = _multiPermissionBroker ?: throw IllegalStateException("multiPermissionBroker is not initialized.")

    private fun activate(vararg brokers: IUtActivityBroker) : UtBuiltInActivityBrokerStore {
        for (broker in brokers) {
            when (broker) {
                is UtOpenFilePicker -> _openFilePicker = broker
                is UtOpenMultiFilePicker -> _openMultiFilePicker = broker
                is UtOpenReadOnlyFilePicker -> _openReadOnlyFilePicker = broker
                is UtOpenReadOnlyMultiFilePicker -> _openReadOnlyMultiFilePicker = broker
                is UtCreateFilePicker -> _createFilePicker = broker
                is UtDirectoryPicker -> _directoryPicker = broker
                is UtPermissionBroker -> _permissionBroker = broker
                is UtMultiPermissionsBroker -> _multiPermissionBroker = broker
                else -> continue
            }
        }
        return this
    }

    fun activate(activity: FragmentActivity, vararg brokers: IUtActivityBroker): UtBuiltInActivityBrokerStore {
        activate(*brokers).register(activity)
        return this;
    }
    fun activateAll(activity: FragmentActivity): UtBuiltInActivityBrokerStore {
        activate(UtOpenFilePicker(), UtOpenMultiFilePicker(), UtOpenReadOnlyFilePicker(), UtOpenReadOnlyMultiFilePicker(), UtCreateFilePicker(), UtDirectoryPicker()).register(activity)
        return this
    }

    fun activate(fragment: Fragment, vararg brokers: IUtActivityBroker): UtBuiltInActivityBrokerStore {
        activate(*brokers).register(fragment)
        return this
    }
    fun activateAll(fragment: Fragment): UtBuiltInActivityBrokerStore {
        activate(UtOpenFilePicker(), UtOpenMultiFilePicker(), UtOpenReadOnlyFilePicker(), UtOpenReadOnlyMultiFilePicker(), UtCreateFilePicker(), UtDirectoryPicker()).register(fragment)
        return this
    }

    override val brokerList: List<IUtActivityBroker> get() = arrayOf(_openFilePicker, _openMultiFilePicker, _openReadOnlyFilePicker, _openReadOnlyMultiFilePicker, _createFilePicker, _directoryPicker, _permissionBroker, _multiPermissionBroker).filterNotNull()
}
