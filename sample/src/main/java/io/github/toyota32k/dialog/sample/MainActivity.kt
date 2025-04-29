package io.github.toyota32k.dialog.sample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import io.github.toyota32k.binder.Binder
import io.github.toyota32k.binder.command.LiteUnitCommand
import io.github.toyota32k.binder.command.bindCommand
import io.github.toyota32k.binder.textBinding
import io.github.toyota32k.dialog.UtDialogConfig
import io.github.toyota32k.dialog.UtMessageBox
import io.github.toyota32k.dialog.UtStandardString
import io.github.toyota32k.dialog.broker.IUtActivityBrokerStoreProvider
import io.github.toyota32k.dialog.broker.UtActivityBrokerStore
import io.github.toyota32k.dialog.broker.UtMultiPermissionsBroker
import io.github.toyota32k.dialog.broker.asActivityBrokerStore
import io.github.toyota32k.dialog.broker.pickers.UtOpenFilePicker
import io.github.toyota32k.dialog.broker.pickers.UtOpenReadOnlyFilePicker
import io.github.toyota32k.dialog.broker.pickers.UtOpenReadOnlyMultiFilePicker
import io.github.toyota32k.dialog.mortal.UtMortalActivity
import io.github.toyota32k.dialog.sample.broker.ImageCameraBroker
import io.github.toyota32k.dialog.sample.databinding.ActivityMainBinding
import io.github.toyota32k.dialog.sample.dialog.AutoScrollDialog
import io.github.toyota32k.dialog.sample.dialog.CompactDialog
import io.github.toyota32k.dialog.sample.dialog.CustomHeightDialog
import io.github.toyota32k.dialog.sample.dialog.FullHeightDialog
import io.github.toyota32k.dialog.sample.dialog.NestedDialog
import io.github.toyota32k.dialog.task.UtImmortalTask.Companion.launchTask
import io.github.toyota32k.dialog.task.UtDialogViewModel
import io.github.toyota32k.dialog.task.UtImmortalTask
import io.github.toyota32k.dialog.task.createViewModel
import io.github.toyota32k.dialog.task.showConfirmMessageBox
import io.github.toyota32k.dialog.task.showYesNoMessageBox
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : UtMortalActivity(), IUtActivityBrokerStoreProvider {
    class MainActivityViewModel : ViewModel() {
        val outputString = MutableStateFlow("")
        val commandMessageBox = LiteUnitCommand {
            launchTask {
                outputString.value = "MessageBox opening"
                showConfirmMessageBox("MessageBox", "Hello world.")
                outputString.value = "MessageBox closed"
            }
        }
        val commandOkCancel = LiteUnitCommand {
            launchTask {
                outputString.value = "Yes/No MessageBox opening"
                if (showYesNoMessageBox("Yes/No", "Are you ok?")) {
                    outputString.value = "You are ok."
                } else {
                    outputString.value = "You aren't ok."
                    showConfirmMessageBox(null, "Take care!")
                }
            }
        }
        val commandCompactDialog = LiteUnitCommand {
            launchTask {
                outputString.value = "Compact Dialog opening"
                val vm = createViewModel<CompactDialog.CompactDialogViewModel>()
                if(showDialog(CompactDialog()).status.ok) {
                    outputString.value = "Your name is ${vm.yourName.value}."
                } else {
                    outputString.value = "Canceled."
                }
            }
        }
        val commandAutoScrollDialog = LiteUnitCommand {
            launchTask {
                createViewModel<AutoScrollDialog.AutoScrollDialogViewModel>()
                showDialog(AutoScrollDialog())
            }
        }
        val commandFullHeightDialog = LiteUnitCommand {
            launchTask {
                createViewModel<FullHeightDialog.FullHeightDialogViewModel>()
                showDialog(FullHeightDialog())
            }
        }
        val commandCustomHeightDialog = LiteUnitCommand {
            launchTask {
                createViewModel<CustomHeightDialog.CustomHeightDialogViewModel>()
                showDialog(CustomHeightDialog())
            }
        }
        val commandFileSelection = LiteUnitCommand {
            launchTask {
                withOwner {
                val activity = it.asActivityBrokerStore()
                    val uri = activity.openFilePicker.selectFile()
                    if (uri!=null) {
                       outputString.value = "selected: $uri"
                    } else {
                        outputString.value = "file not selected."
                    }
                }
            }
        }
        val commandNestedDialog = LiteUnitCommand {
            launchTask {
                createViewModel<NestedDialog.NestedDialogViewModel>()
                if(showDialog(NestedDialog()).status.ok) {
                    outputString.value = "NestedDialog: OK"
                } else {
                    outputString.value = "NestedDialog: Canceled"
                }
            }
        }
    }

    override val activityBrokers = UtActivityBrokerStore(this, UtOpenFilePicker(), UtOpenReadOnlyFilePicker(),UtOpenReadOnlyMultiFilePicker(),UtMultiPermissionsBroker(), ImageCameraBroker())
    private lateinit var controls: ActivityMainBinding
    private val binder = Binder()
    private val viewModel by viewModels<MainActivityViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        controls = ActivityMainBinding.inflate(layoutInflater)
        setContentView(controls.root)

//        UtDialogConfig.showInDialogModeAsDefault = true
//        UtDialogConfig.solidBackgroundOnPhone = true
        UtDialogConfig.dialogTheme = io.github.toyota32k.dialog.R.style.UtDialogThemeTertiary
        UtDialogConfig.showInDialogModeAsDefault = false


        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, maxOf(systemBars.bottom, ime.bottom))
            insets
        }

        binder
            .owner(this)
            .bindCommand(viewModel.commandMessageBox, controls.btnMessageBox)
            .bindCommand(viewModel.commandOkCancel, controls.btnOkCancel)
            .bindCommand(viewModel.commandCompactDialog, controls.btnCompactDialog)
            .bindCommand(viewModel.commandAutoScrollDialog, controls.btnAutoScrollDialog)
            .bindCommand(viewModel.commandFullHeightDialog, controls.btnFullHeightDialog)
            .bindCommand(viewModel.commandCustomHeightDialog, controls.btnCustomHeightDialog)
            .bindCommand(viewModel.commandFileSelection, controls.btnFileSelection)
            .bindCommand(viewModel.commandNestedDialog, controls.btnNestedDialog)
            .bindCommand(LiteUnitCommand(::startOptionActivity), controls.navigateOptionActivity)
            .textBinding(controls.outputText, viewModel.outputString)
    }

    fun startOptionActivity() {
        startActivity(Intent(this, OptionActivity::class.java))
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
//        if(keyCode == KeyEvent.KEYCODE_BACK) {
//            return false
//        }
        return super.onKeyDown(keyCode, event)
    }
}