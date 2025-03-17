@file:Suppress("unused")

package io.github.toyota32k.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.ListAdapter
import io.github.toyota32k.dialog.task.UtImmortalSimpleTask
import io.github.toyota32k.dialog.task.UtImmortalTask
import io.github.toyota32k.dialog.task.showMultiSelectionBox
import io.github.toyota32k.dialog.task.showRadioSelectionBox
import io.github.toyota32k.dialog.task.showSingleSelectionBox

interface IUtSingleSelectionResult {
    val selectedIndex: Int
    val selectedItem: String?
}

open class UtSingleSelectionBox : UtDialogBase(), DialogInterface.OnClickListener,
    IUtSingleSelectionResult {
    var title:String? by bundle.stringNullable
    var items:Array<String> by bundle.stringArray

    override var selectedIndex: Int = -1
    override val selectedItem: String?
        get() = if (0 <= selectedIndex && selectedIndex < items.size) items[selectedIndex] else null

    /**
     * ListItemViewをカスタマイズするなら、このメソッドをオーバーライド
     */
    protected open fun listAdapter():ListAdapter? {
        return null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = cancellable
        return AlertDialog.Builder(requireContext()).also { builder->
            title?.let { builder.setTitle(it) }
            val adapter = listAdapter()
            if(adapter!=null) {
                builder.setAdapter(adapter,this)
            } else {
                builder.setItems(items, this)
            }
        }
        .create()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        if(0<=which && which<items.size) {
            selectedIndex = which
            complete(IUtDialog.Status.POSITIVE)
        }
    }

    companion object {
        fun create(title:String?, items:Array<String>) : UtSingleSelectionBox {
            return UtSingleSelectionBox().apply {
                this.items = items
                this.title = title
            }
        }
//        suspend fun open(title:String?, items:Array<String>) : Int {
//            return UtImmortalTask.awaitTaskResult<Int> {
//                showSingleSelectionBox(title,items)
//            }
//        }
    }
}

open class UtRadioSelectionBox : UtMessageBox(), DialogInterface.OnClickListener {
    var items:Array<String> by bundle.stringArray
    var selectedIndex: Int by bundle.intMinusOne
    val selectedItem: String?
        get() = if (0 <= selectedIndex && selectedIndex < items.size) items[selectedIndex] else null

    /**
     * ListItemViewをカスタマイズするなら、このメソッドをオーバーライド
     */
    protected open fun listAdapter():ListAdapter? {
        return null
    }

    override fun getAlertBuilder(): AlertDialog.Builder {
        return super.getAlertBuilder().also { builder ->
            val adapter = listAdapter()
            if (adapter != null) {
                builder.setSingleChoiceItems(adapter, selectedIndex, this)
            } else {
                builder.setSingleChoiceItems(items, selectedIndex, this)
            }
        }
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        if(0<=which && which<items.size) {
            selectedIndex = which
//            complete(IUtDialog.Status.POSITIVE)
        }
        super.onClick(dialog, which)
    }

    companion object {
        fun create(title:String?, items:Array<String>, initialSelection:Int, okLabel:String= UtStandardString.OK.text, cancelLabel:String?=UtStandardString.CANCEL.text) : UtRadioSelectionBox {
            return UtRadioSelectionBox().apply {
                this.title = title
                this.items = items
                this.selectedIndex = initialSelection
                this.okLabel = okLabel
                this.cancelLabel = cancelLabel
            }
        }
//        suspend fun open(title:String?, items:Array<String>, initialSelection:Int, okLabel:String= UtStandardString.OK.text, cancelLabel:String?=UtStandardString.CANCEL.text) : Int {
//            return UtImmortalTask.awaitTaskResult<Int> {
//                showRadioSelectionBox(title,items,initialSelection,okLabel,cancelLabel)
//            }
//        }
    }
}

class UtMultiSelectionBox
    : UtMessageBox(), DialogInterface.OnMultiChoiceClickListener {
    var items:Array<String> by bundle.stringArray
    var selectionFlags:BooleanArray by bundle.booleanArray
    val selectedItems:Array<String>
        get() = (items.indices).filter { selectionFlags[it] }.map { items[it] }.toTypedArray()

    override fun getAlertBuilder(): AlertDialog.Builder {
        return super.getAlertBuilder().setMultiChoiceItems(items, selectionFlags, this)
    }

    override fun onClick(dialog: DialogInterface?, which: Int, isChecked: Boolean) {
        if(0<=which && which<items.size) {
            selectionFlags[which] = isChecked
        } else {
            super.onClick(dialog, which)
        }
    }

    companion object {
        fun create(title:String?, items:Array<String>, initialSelections:BooleanArray?, okLabel:String= UtStandardString.OK.text, cancelLabel:String?=null) : UtMultiSelectionBox {
            return UtMultiSelectionBox().apply {
                this.title = title
                this.items = items
                this.selectionFlags = initialSelections ?: BooleanArray(items.size) { false }
                this.okLabel = okLabel
                this.cancelLabel = cancelLabel
            }
        }

//        suspend fun open(title:String?, items:Array<String>, initialSelections:BooleanArray?, okLabel:String= UtStandardString.OK.text, cancelLabel:String?=null): BooleanArray {
//            return UtImmortalTask.awaitTaskResult<BooleanArray> {
//                showMultiSelectionBox(title,items,initialSelections,okLabel,cancelLabel)
//            }
//        }
    }
}

