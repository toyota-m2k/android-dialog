package io.github.toyota32k.dialog.broker.pickers

import io.github.toyota32k.dialog.broker.IUtActivityBroker
import io.github.toyota32k.dialog.broker.UtActivityBrokerStore

class UtFilePickerStore : UtActivityBrokerStore() {
    val openFilePicker = UtOpenFilePicker()
    val openMultiFilePicker = UtOpenMultiFilePicker()
    val openReadOnlyFilePicker = UtOpenReadOnlyFilePicker()
    val openReadOnlyMultiFilePicker = UtOpenReadOnlyMultiFilePicker()
    val createFilePicker = UtCreateFilePicker()
    val directoryPicker = UtDirectoryPicker()

    override val brokerList: List<IUtActivityBroker>
        = listOf(openFilePicker, openMultiFilePicker, openReadOnlyFilePicker, openReadOnlyMultiFilePicker, createFilePicker, directoryPicker)
}