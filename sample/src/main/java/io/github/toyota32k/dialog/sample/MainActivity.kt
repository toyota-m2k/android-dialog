package io.github.toyota32k.dialog.sample

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import io.github.toyota32k.binder.Binder
import io.github.toyota32k.binder.IIDValueResolver
import io.github.toyota32k.binder.command.LiteUnitCommand
import io.github.toyota32k.binder.command.bindCommand
import io.github.toyota32k.binder.materialRadioButtonGroupBinding
import io.github.toyota32k.binder.observe
import io.github.toyota32k.binder.textBinding
import io.github.toyota32k.dialog.UtDialogConfig
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
import io.github.toyota32k.dialog.task.createViewModel
import io.github.toyota32k.dialog.task.showConfirmMessageBox
import io.github.toyota32k.dialog.task.showYesNoMessageBox
import io.github.toyota32k.logger.UtLogConfig
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : UtMortalActivity(), IUtActivityBrokerStoreProvider {
    class MainActivityViewModel : ViewModel() {
        val outputString = MutableStateFlow("")
//        var count:Int = 0
//        var flowSample = MutableStateFlow<String>("")
        val commandMessageBox = LiteUnitCommand {
//            CoroutineScope(Dispatchers.Main+SupervisorJob()).launch {
//                flowSample.disposableObserve(this.coroutineContext) {
//                    logger.debug("value=$it")
//                }
//            }
//            CoroutineScope(Dispatchers.Main).launch {
//                withContext(Dispatchers.IO) {
//                    val job = CoroutineScope(SupervisorJob()).launch {
//                        flowSample.collect {
//                            logger.debug("value = $it")
//                        }
//                    }
//                    logger.debug("withContext: end")
//                    delay(1000)
////                    job.cancel()
//                }
//                logger.debug("completed")
//            }
            launchTask {
                outputString.value = "MessageBox opening"
                showConfirmMessageBox("MessageBox", "Hello world.")
//                showSingleSelectionBox("SingleSelection", arrayOf("aaa", "bbb", "ccc"))
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
                showDialog<AutoScrollDialog.AutoScrollDialogViewModel,AutoScrollDialog>()
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
        val isDialog = MutableStateFlow(UtDialogConfig.showInDialogModeAsDefault)

        object IsDialogIDResolver : IIDValueResolver<Boolean> {
            override fun id2value(id: Int): Boolean? {
                return when (id) {
                    R.id.dialog_mode_dialog -> true
                    R.id.dialog_mode_fragment -> false
                    else -> null
                }
            }

            override fun value2id(v: Boolean): Int {
                return when (v) {
                    true -> R.id.dialog_mode_dialog
                    false -> R.id.dialog_mode_fragment
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

        UtLogConfig.logLevel = 2 // Log.VERBOSE
//        UtDialogConfig.showInDialogModeAsDefault = true
//        UtDialogConfig.solidBackgroundOnPhone = true
        UtDialogConfig.dialogTheme = io.github.toyota32k.dialog.R.style.UtDialogThemeTertiary
        //UtDialogConfig.showInDialogModeAsDefault = true


        WindowCompat.setDecorFitsSystemWindows(window, false)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, maxOf(systemBars.bottom, ime.bottom))
//            insets
//        }
        setupWindowInsetsListener(controls.root)

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
            .materialRadioButtonGroupBinding(controls.dialogMode, viewModel.isDialog, MainActivityViewModel.IsDialogIDResolver)
            .observe(viewModel.isDialog) { UtDialogConfig.showInDialogModeAsDefault = it }
    }

    // LinearLayout に、layout_gravity=center_vertical を設定していると、portraitではいい感じだが、
    // landscape で、上端がScrollViewの上限より上に配置されてしまって操作できなくなる。
    // 当然、layout.xml を分けるのが王道だが、LinearLayoutのプロパティ1個を変えるだけなので、
    // コードでやってしまうことにした。
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val childLayout = controls.buttonContainer
        val layoutParams = childLayout.layoutParams as FrameLayout.LayoutParams

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 横向きの場合は上揃え
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL
        } else {
            // 縦向きの場合は中央揃え
            layoutParams.gravity = Gravity.CENTER
        }
        childLayout.layoutParams = layoutParams
    }

    fun startOptionActivity() {
        startActivity(Intent(this, OptionActivity::class.java))
    }

//    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
//        return super.onKeyDown(keyCode, event)
//    }
}