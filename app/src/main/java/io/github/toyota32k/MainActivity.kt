package io.github.toyota32k

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.StyleRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import io.github.toyota32k.binder.Binder
import io.github.toyota32k.binder.IIDValueResolver
import io.github.toyota32k.binder.activityActionBarBinding
import io.github.toyota32k.binder.activityStatusBarBinding
import io.github.toyota32k.binder.checkBinding
import io.github.toyota32k.binder.command.LiteUnitCommand
import io.github.toyota32k.binder.command.bindCommand
import io.github.toyota32k.databinding.ActivityMainBinding
import io.github.toyota32k.dialog.UtDialog
import io.github.toyota32k.dialog.UtDialog.WidthOption
import io.github.toyota32k.dialog.UtDialogConfig
import io.github.toyota32k.dialog.UtMessageBox
import io.github.toyota32k.dialog.UtSingleSelectionBox
import io.github.toyota32k.dialog.UtStandardString
import io.github.toyota32k.dialog.broker.pickers.UtFilePickerStore
import io.github.toyota32k.dialog.task.*
import io.github.toyota32k.sample.AutoScrollDialog
import io.github.toyota32k.sample.CompactDialog
import io.github.toyota32k.sample.Config
import io.github.toyota32k.sample.CustomDialog
import io.github.toyota32k.sample.FillDialog
import io.github.toyota32k.sample.HogeDialog
import io.github.toyota32k.sample.SamplePortalDialog
import io.github.toyota32k.utils.UtLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

class MainActivity : UtMortalActivity() {
    override val logger = UtLog("SAMPLE")
    override val immortalTaskNameList: Array<String> = arrayOf(SampleTask.TASK_NAME, FileTestTask.TASK_NAME)

    override fun notifyImmortalTaskResult(taskInfo: UtImmortalTaskManager.ITaskInfo) {
        logger.info("${taskInfo.name} ${taskInfo.state} ${taskInfo.result}")
        UtMessageBox.createForConfirm("Task Completed", "Task ${taskInfo.name} Result=${taskInfo.result}").show(this, "taskCompleted")
    }

    class MainViewModel : ViewModel() {
        val logger = UtLog("SAMPLE.ViewModel")
        val isDialogMode = MutableStateFlow(false)
        val edgeToEdgeEnabled = MutableStateFlow(false)
        val showStatusBar = MutableStateFlow(false)
        val showActionBar = MutableStateFlow(false)
        val dialogPosition = MutableStateFlow(DialogPosition.Right)
        val materialTheme = MutableStateFlow(MaterialTheme.Legacy)
        val commandCompactDialog = LiteUnitCommand(::showCompactDialog)
        val commandAutoScrollDialog = LiteUnitCommand(::showAutoScrollDialog)
        val commandFillDialog = LiteUnitCommand(::showFillHeightDialog)
        val commandCustomDialog = LiteUnitCommand(::showCustomDialog)

        enum class DialogPosition(@IdRes val id:Int) {
            Full(R.id.radio_fit_screen_width),
            Left(R.id.radio_left),
            Center(R.id.radio_center),
            Right(R.id.radio_right),
            ;
            object IdValueResolver : IIDValueResolver<DialogPosition> {
                override fun id2value(id: Int): DialogPosition {
                    return enumValues<DialogPosition>().find { it.id == id } ?: Right
                }
                override fun value2id(v: DialogPosition): Int {
                    return v.id
                }
            }
        }
        enum class MaterialTheme(@IdRes val id:Int, @StyleRes val themeId:Int) {
            Legacy(R.id.radio_material2, R.style.Theme_DialogSample_Legacy),
            Material3(R.id.radio_material3, R.style.Theme_DialogSample_Material3),
            DynamicColor(R.id.radio_dynamic_color, R.style.Theme_DialogSample_DynamicColor),
            ;
            object IdValueResolver : IIDValueResolver<MaterialTheme> {
                override fun id2value(id: Int): MaterialTheme {
                    return enumValues<MaterialTheme>().find { it.id == id } ?: Legacy
                }
                override fun value2id(v: MaterialTheme): Int {
                    return v.id
                }
            }
        }

        private fun <T:UtDialog> T.applyPosition():T {
            when(dialogPosition.value) {
                DialogPosition.Full -> widthOption = WidthOption.FULL
                DialogPosition.Left -> gravityOption = UtDialog.GravityOption.LEFT_TOP
                DialogPosition.Center -> gravityOption = UtDialog.GravityOption.CENTER
                DialogPosition.Right -> gravityOption = UtDialog.GravityOption.RIGHT_TOP
            }
            return this
        }

        private fun showCompactDialog() {
            UtImmortalSimpleTask.run {
                logger.debug("Showing: CompactDialog...")
                showDialog(CompactDialog::class.java.name) { CompactDialog(isDialogMode.value, edgeToEdgeEnabled.value).applyPosition() }
                logger.debug("Closed: CompactDialog")
                true
            }
        }

        private fun showAutoScrollDialog() {
            UtImmortalSimpleTask.run {
                logger.debug("Showing: AutoScrollDialog...")
                showDialog(AutoScrollDialog::class.java.name) { AutoScrollDialog(isDialogMode.value, edgeToEdgeEnabled.value).applyPosition() }
                logger.debug("Closed: AutoScrollDialog")
                true
            }
        }
        private fun showFillHeightDialog() {
            UtImmortalSimpleTask.run {
                logger.debug("Showing: FillDialog...")
                showDialog(FillDialog::class.java.name) { FillDialog(isDialogMode.value, edgeToEdgeEnabled.value).applyPosition() }
                logger.debug("Closed: FillDialog")
                true
            }
        }
        private fun showCustomDialog() {
            UtImmortalSimpleTask.run {
                logger.debug("Showing: CustomDialog...")
                showDialog(CustomDialog::class.java.name) { CustomDialog(isDialogMode.value, edgeToEdgeEnabled.value).applyPosition() }
                logger.debug("Closed: CustomDialog")
                true
            }
        }
    }

    private lateinit var controls: ActivityMainBinding
    private val binder = Binder()
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        UtStandardString.setContext(this, null)
        UtDialogConfig.solidBackgroundOnPhone = Config.solidBackgroundOnPhone       // true: Phoneのとき背景灰色(default) / false: tabletの場合と同じ
        UtDialogConfig.showInDialogModeAsDefault = Config.showInDialogModeAsDefault     // true: ダイアログモード / false:フラグメントモード(default)

        dialogHostManager["hoge"] = {
            logger.info("hoge:${it.status}")
        }

        controls = ActivityMainBinding.inflate(layoutInflater)
        setContentView(controls.root)
        ViewCompat.setOnApplyWindowInsetsListener(controls.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binder
            .owner(this)
            .activityStatusBarBinding(viewModel.showStatusBar)
            .activityActionBarBinding(viewModel.showActionBar)
            .checkBinding(controls.checkDialogMode, viewModel.isDialogMode)
            .checkBinding(controls.checkEdgeToEdge, viewModel.edgeToEdgeEnabled)
            .checkBinding(controls.checkStatusBar, viewModel.showStatusBar)
            .checkBinding(controls.checkActionBar, viewModel.showActionBar)
            .bindCommand(viewModel.commandCompactDialog, controls.compactDialogButton)
            .bindCommand(viewModel.commandAutoScrollDialog, controls.autoScrollDialogButton)
            .bindCommand(viewModel.commandFillDialog, controls.fullHeightDialogButton)
            .bindCommand(viewModel.commandCustomDialog, controls.customDialogButton)

//
//
//        findViewById<Button>(R.id.dialog_button).setOnClickListener {
//            //UtMultiSelectionBox.select(this,"hoge", "タイトル", arrayOf("hoge", "fuga", "piyo"), booleanArrayOf(true,false,true), cancelLabel = getString(R.string.cancel))
//            HogeDialog().show(this, "hoge")
//        }
//        findViewById<Button>(R.id.message_button).setOnClickListener {
//            //UtMultiSelectionBox.select(this,"hoge", "タイトル", arrayOf("hoge", "fuga", "piyo"), booleanArrayOf(true,false,true), cancelLabel = getString(R.string.cancel))
//            // UtMessageBox.createForOkCancel("UtMessageBox", "テストです").show(this, "utmessage")
//            UtImmortalSimpleTask.run {
//                val dlg = showDialog("hoge") { UtMessageBox.createForOkCancel("Title!!", "it's a message.").apply { cancellable = false } }
//                logger.debug("done (${dlg.status})")
//                true
//            }
//        }
//        findViewById<Button>(R.id.rx_dialog_button).setOnClickListener {
//            CoroutineScope(Dispatchers.Main).launch {
//                val r = RxDialog().showAsSuspendable(supportFragmentManager)
//                logger.info("$r")
//            }
//        }
//        findViewById<Button>(R.id.flow_test_button).setOnClickListener {
//            flowTest()
//        }
//        findViewById<Button>(R.id.immortal_task_button).setOnClickListener {
//            SampleTask().fire()
//        }
//        findViewById<Button>(R.id.catalogue).setOnClickListener {
//            SamplePortalDialog().show(this, "Catalogue")
//        }
//        findViewById<Button>(R.id.activity_call).setOnClickListener {
//            activityCallTestSelectionReceptor.showDialog(this) {
//                UtSingleSelectionBox().apply {
//                    title = "Activity Call Test"
//                    items = arrayOf(
//                        "Open File",
//                        "Open Multiple Files",
//                        "Create File",
//                        "Select Folder",
//                        "Process in ImmortalTask",
//                        "Pickers with ActivityBroker"
//                    )
//                }
//            }
//        }
    }

//    override fun onDestroy() {
//        super.onDestroy()
//    }

//    override fun queryDialogResultReceptor(tag: String): IUtDialogResultReceptor? {
//        return dialogHostManager.queryDialogResultReceptor(tag)
//    }

    private fun flowTest() {
        val flow = MutableStateFlow(0)
        CoroutineScope(Dispatchers.Default).launch {
            val v = flow.filter { it>=5 }.first()
            logger.info("flow:$v")
        }

        CoroutineScope(Dispatchers.Default).launch {
            delay(500)
            for (n in 1..10) {
                logger.info("emit:$n")
                flow.value = n
                delay(100)
            }
        }
    }


//    private val openFilePicker = UtFileOpenPicker(this.toDialogOwner(), arrayOf("text/*")) { uri->
//        logger.info("OpenFile: $uri")
//        if(uri!=null) {
//            contentResolver.openInputStream(uri)?.use { stream->
//                val line = stream.bufferedReader().readLine()
//                logger.info(line)
//            }
//        }
//
//    }
//    private val openMultiFilePicker = UtMultiFileOpenPicker(this.toDialogOwner(), arrayOf("application/*")) {
//        logger.info("OpenMultipleFile: $it")
//    }
//    private val createFilePicker = UtFileCreatePicker(this.toDialogOwner(), "test.txt", null) { uri->
//        logger.info("CreateFile: $uri")
//        if(uri!=null) {
//            contentResolver.openOutputStream(uri)?.use { stream->
//                stream.write("hogehoge".toByteArray())
//            }
//        }
//    }
//    private var directoryUri:Uri? = null
//    private val direcotryPicker = UtDirectoryPicker(this.toDialogOwner(), directoryUri) { uri->
//        logger.info("Directory: $uri")
//        if(uri!=null) {
//            directoryUri = uri
//            val dir = DocumentFile.fromTreeUri(this, uri) ?: return@UtDirectoryPicker
//            val file = dir.createFile("text/plain", "x.txt") ?: return@UtDirectoryPicker
//            contentResolver.openOutputStream(file.uri)?.use { stream->
//                stream.write("fugafuga".toByteArray())
//            }
//
//        }
//    }

    companion object {
//        val activityConnectorFactoryBank = UtActivityConnectorFactoryBank(
//            arrayOf(
//                UtFileOpenPicker.Factory(FileTestTask.TASK_NAME, FileTestTask.OPEN_FILE_CONNECTOR, arrayOf("text/*")),
//                UtDirectoryPicker.Factory(FileTestTask.TASK_NAME, FileTestTask.OPEN_DIRECTORY_CONNECTOR, null),
//            ))
    }

//    private val activityConnectorStore = activityConnectorFactoryBank.createConnectorStore(this.toDialogOwner())
//    override fun getActivityConnector(immortalTaskName: String,connectorName: String): UtActivityConnector<*, *>? {
//        return activityConnectorStore.getActivityConnector(immortalTaskName,connectorName)
//    }
    val filePickers = UtFilePickerStore(this)

    class FileTestTask:UtImmortalTaskBase(TASK_NAME) {
        companion object {
            const val TASK_NAME = "FileTestTask"
            const val OPEN_DIRECTORY_CONNECTOR = "OpenDirectory"
            const val OPEN_FILE_CONNECTOR = "OpenFile"
        }

        override suspend fun execute(): Boolean {
            val dirUri = (getActivity() as? MainActivity)?.filePickers?.directoryPicker?.selectDirectory() ?: return false
            withOwner { owner ->
                val dir = DocumentFile.fromTreeUri(owner.asContext(), dirUri)
                val file = dir?.createFile("text/plain", "xxx.txt")
                if(file!=null) {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            owner.asContext().contentResolver.openOutputStream(file.uri)
                                ?.use { stream ->
                                    stream.write("piyopiyo".toByteArray())
                                }
                        }
                    }
                }
            }

            val fileUrl = (getActivity() as? MainActivity)?.filePickers?.openReadOnlyFilePicker?.selectFile() ?: return false
            withOwner { owner->
                withContext(Dispatchers.IO) {
                    runCatching {
                        owner.asContext().contentResolver.openInputStream(fileUrl)?.use { stream ->
                            val line = stream.bufferedReader().readLine()
                            logger.info(line)
                        }
                    }
                    val file = DocumentFile.fromSingleUri(owner.asContext(),fileUrl)
                    file?.delete()
                }
            }

            logger.info("task completed")
            return true
        }
    }

    class BrokerTestTask:UtImmortalTaskBase(TASK_NAME) {
        companion object {
            const val TASK_NAME = "BrokerTestTask"
        }

        override suspend fun execute(): Boolean {
            withOwner(MainActivity::class.java) {
                logger.info("openFilePicker")
                val activity = it.asActivity() as MainActivity
                val uri = activity.filePickers.openFilePicker.selectFile(arrayOf("image/png", "image/jpeg", "application/pdf"))
                logger.info("openFilePicker: $uri")
            }
            withOwner(MainActivity::class.java) {
                logger.info("openReadOnlyFilePicker")
                val activity = it.asActivity() as MainActivity
                val uri = activity.filePickers.openReadOnlyFilePicker.selectFile("image/png")
                logger.info("openReadOnlyFilePicker: $uri")
            }
            withOwner(MainActivity::class.java) {
                logger.info("openMultiFilePicker")
                val activity = it.asActivity() as MainActivity
                val uris = activity.filePickers.openMultiFilePicker.selectFiles(arrayOf("image/png", "image/jpeg", "application/pdf"))
                logger.info("openMultiFilePicker: ${
                    uris.fold(StringBuilder()){ builder, uri->
                        builder.append("\n")
                        builder.append(uri.toString())
                    }
                }")
            }
            withOwner(MainActivity::class.java) {
                logger.info("openReadOnlyMultiFilePicker")
                val activity = it.asActivity() as MainActivity
                val uris = activity.filePickers.openReadOnlyMultiFilePicker.selectFiles("image/jpeg")
                logger.info("openReadOnlyMultiFilePicker: ${uris.fold(StringBuilder()){builder,uri->
                    builder.append("\n")
                    builder.append(uri.toString())
                }}")
            }
            withOwner(MainActivity::class.java) {
                logger.info("createFilePicker")
                val activity = it.asActivity() as MainActivity
                val uri = activity.filePickers.createFilePicker.selectFile("hoge.png","image/png")
                logger.info("createFilePicker: $uri")
            }
            withOwner(MainActivity::class.java) {
                logger.info("directoryPicker")
                val activity = it.asActivity() as MainActivity
                val uri = activity.filePickers.directoryPicker.selectDirectory()
                logger.info("directoryPicker: $uri")
            }
            return true
        }
    }

    private val activityCallTestSelectionReceptor = dialogHostManager.register<UtSingleSelectionBox>("activityCallTestSelectionReceptor") {
        if(it.dialog.status.ok) {
            when (it.dialog.selectedIndex) {
                0 -> UtImmortalSimpleTask.run { filePickers.openFilePicker.selectFile() != null }
                1 -> UtImmortalSimpleTask.run { filePickers.openMultiFilePicker.selectFiles().isNotEmpty() }
                2 -> UtImmortalSimpleTask.run { filePickers.createFilePicker.selectFile("test.txt")!=null }
                3 -> UtImmortalSimpleTask.run { filePickers.directoryPicker.selectDirectory()!=null }
                4 -> FileTestTask().fire()
                5 -> BrokerTestTask().fire()
                else -> {}
            }
        }
    }

}