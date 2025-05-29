package io.github.toyota32k.dialog.sample.dialog

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.TextView
import io.github.toyota32k.binder.command.LiteUnitCommand
import io.github.toyota32k.binder.command.bindCommand
import io.github.toyota32k.binder.list.ObservableList
import io.github.toyota32k.binder.recyclerViewBinding
import io.github.toyota32k.binder.recyclerViewBindingEx
import io.github.toyota32k.binder.textBinding
import io.github.toyota32k.dialog.UtDialogEx
import io.github.toyota32k.dialog.broker.asActivityBrokerStore
import io.github.toyota32k.dialog.sample.R
import io.github.toyota32k.dialog.sample.databinding.DialogNestedBinding
import io.github.toyota32k.dialog.sample.databinding.ItemStringListBinding
import io.github.toyota32k.dialog.task.UtDialogViewModel
import io.github.toyota32k.dialog.task.createViewModel
import io.github.toyota32k.dialog.task.getViewModel
import io.github.toyota32k.utils.asConstantLiveData
import io.github.toyota32k.utils.letOnTrue
import java.io.File

class NestedDialog : UtDialogEx() {
    class NestedDialogViewModel : UtDialogViewModel() {
        val observableList = ObservableList<String>()
        val commandAddText = LiteUnitCommand {
            launchSubTask {
                val vm = createViewModel<CompactDialog.CompactDialogViewModel>()
                if(showDialog(CompactDialog()).status.ok) {
                    observableList.add(vm.yourName.value)
                }
            }
        }
        val commandAddFile = LiteUnitCommand {
            launchSubTask {
                withOwner { owner->
                    val activityBrokers = owner.asActivityBrokerStore()
                    val uri = activityBrokers.openReadOnlyFilePicker.selectFile()
                    if (uri != null) {
                        observableList.add(getFileName(owner.asContext(), uri))
                    }
                }
            }
        }
        val commandAddFiles = LiteUnitCommand {
            launchSubTask {
                withOwner { owner->
                    val activityBrokers = owner.asActivityBrokerStore()
                    val uris = activityBrokers.openReadOnlyMultiFilePicker.selectFiles()
                    if (uris.isNotEmpty()) {
                        observableList.addAll(uris.map { getFileName(owner.asContext(), it) })
                    }
                }
            }
        }
        fun getFileName(context:Context, uri:Uri):String {
            return when(uri.scheme) {
                ContentResolver.SCHEME_FILE -> uri.path?.let { File(it).name }
                ContentResolver.SCHEME_CONTENT -> context.contentResolver.query(uri,null,null,null,null,null)?.use { cursor ->
                    cursor.moveToFirst().letOnTrue {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx != -1) {
                            cursor.getString(idx)
                        } else null
                    }
                }
                else -> null
            } ?: "unknown file"
        }
    }

    override fun preCreateBodyView() {
        title="Fill Height"
        heightOption = HeightOption.FULL
        widthOption = WidthOption.LIMIT(400)
        leftButtonType = ButtonType.CANCEL
        rightButtonType = ButtonType.DONE
    }

    lateinit var controls: DialogNestedBinding
    val viewModel by lazy { getViewModel<NestedDialogViewModel>() }

    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        controls = DialogNestedBinding.inflate(inflater.layoutInflater)
        binder
            .bindCommand(viewModel.commandAddText, controls.addTextButton)
            .bindCommand(viewModel.commandAddFile, controls.addFileButton)
            .bindCommand(viewModel.commandAddFiles, controls.multiFileButton)
            .recyclerViewBindingEx(controls.recyclerView) {
                list(viewModel.observableList)
                inflate { parent-> ItemStringListBinding.inflate(inflater.layoutInflater, parent, false) }
                bindView { itemControls, itemBinder, _, text->
                    itemBinder.textBinding(this@NestedDialog, itemControls.textView, text.asConstantLiveData())
                }
            }
//            .recyclerViewGestureBinding(controls.recyclerView, viewModel.observableList, R.layout.item_string_list, dragToMove = true, swipeToDelete=true, deletionHandler = null) {
//                listBinder, view, text->
//                val textView = view.findViewById<TextView>(R.id.text_view)
//                listBinder.textBinding(this@NestedDialog, textView, text.asConstantLiveData())
//            }
        return controls.root
    }


}