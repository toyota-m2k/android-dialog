package io.github.toyota32k

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.IdRes
import androidx.annotation.StyleRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.toyota32k.binder.Binder
import io.github.toyota32k.binder.BoolConvert
import io.github.toyota32k.binder.IIDValueResolver
import io.github.toyota32k.binder.activityActionBarBinding
import io.github.toyota32k.binder.activityStatusBarBinding
import io.github.toyota32k.binder.checkBinding
import io.github.toyota32k.binder.clickBinding
import io.github.toyota32k.binder.command.LiteUnitCommand
import io.github.toyota32k.binder.command.bindCommand
import io.github.toyota32k.binder.enableBinding
import io.github.toyota32k.binder.materialRadioButtonGroupBinding
import io.github.toyota32k.binder.visibilityBinding
import io.github.toyota32k.databinding.ActivityMainBinding
import io.github.toyota32k.dialog.UtDialog
import io.github.toyota32k.dialog.UtDialog.WidthFlag
import io.github.toyota32k.dialog.UtDialogConfig
import io.github.toyota32k.dialog.UtRadioSelectionBox
import io.github.toyota32k.dialog.UtStandardString
import io.github.toyota32k.dialog.task.UtImmortalSimpleTask
import io.github.toyota32k.dialog.mortal.UtMortalActivity
import io.github.toyota32k.dialog.task.showYesNoMessageBox
import io.github.toyota32k.sample.AutoScrollDialog
import io.github.toyota32k.sample.CompactDialog
import io.github.toyota32k.sample.Config
import io.github.toyota32k.sample.CustomDialog
import io.github.toyota32k.sample.FillDialog
import io.github.toyota32k.utils.ApplicationViewModelStoreOwner
import io.github.toyota32k.utils.UtLog
import io.github.toyota32k.utils.disposableObserve
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : UtMortalActivity() {
    override val logger = UtLog("SAMPLE")
//    override val immortalTaskNameList: Array<String> = arrayOf(SampleTask.TASK_NAME, FileTestTask.TASK_NAME)
//
//    override fun notifyImmortalTaskResult(taskInfo: UtImmortalTaskManager.ITaskInfo) {
//        logger.info("${taskInfo.name} ${taskInfo.state} ${taskInfo.result}")
//        UtMessageBox.createForConfirm("Task Completed", "Task ${taskInfo.name} Result=${taskInfo.result}").show(this, "taskCompleted")
//    }

    class MainViewModel : ViewModel() {
        val logger = UtLog("SAMPLE.ViewModel")
        val isDialogMode = MutableStateFlow(true)
        val noActionBarTheme = MutableStateFlow(false)
        val edgeToEdgeEnabled = MutableStateFlow(true)
        val hideStatusBarOnDialog = MutableStateFlow(true)
        val cancellable = MutableStateFlow(true)
        val draggable = MutableStateFlow(true)
        val showStatusBar = MutableStateFlow(false)
        val showActionBar = MutableStateFlow(false)
        val dialogPosition = MutableStateFlow(DialogPosition.Center)
        val materialTheme = MutableStateFlow(MaterialTheme.Legacy)
        val guardColor = MutableStateFlow(GuardColorEx.None)

        val commandCompactDialog = LiteUnitCommand(::showCompactDialog)
        val commandAutoScrollDialog = LiteUnitCommand(::showAutoScrollDialog)
        val commandFillDialog = LiteUnitCommand(::showFillHeightDialog)
        val commandCustomDialog = LiteUnitCommand(::showCustomDialog)
        val commandMessageBox = LiteUnitCommand(::showRadioSelectionBox)

        var currentTheme = R.style.Theme_DialogSample_Legacy
        val selectedTheme:Int get() = if(noActionBarTheme.value) materialTheme.value.noActionBarThemeId else materialTheme.value.themeId
        var currentEdgeToEdgeEnabled = true

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
        enum class MaterialTheme(@IdRes val id:Int, @StyleRes val themeId:Int, @StyleRes val noActionBarThemeId:Int) {
            Legacy(R.id.radio_material2, R.style.Theme_DialogSample_Legacy, R.style.Theme_DialogSample_Legacy_NoActionBar),
            Material3(R.id.radio_material3, R.style.Theme_DialogSample_Material3, R.style.Theme_DialogSample_Material3_NoActionBar),
            DynamicColor(R.id.radio_dynamic_color, R.style.Theme_DialogSample_DynamicColor, R.style.Theme_DialogSample_DynamicColor_NoActionBar),
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
        enum class GuardColorEx(@IdRes val id:Int, val color:Int) {
            None(R.id.radio_guard_color_none, UtDialog.GuardColor.INVALID.color),
            Dim(R.id.radio_guard_color_dim, UtDialog.GuardColor.DIM.color),
            SeeThrough(R.id.radio_guard_color_see_through, UtDialog.GuardColor.SEE_THROUGH.color),
            AutoDim(R.id.radio_guard_color_auto, UtDialog.GuardColor.THEME_DIM.color),
            AutoST(R.id.radio_guard_color_auto_s, UtDialog.GuardColor.THEME_SEE_THROUGH.color)
            ;
            object IdValueResolver : IIDValueResolver<GuardColorEx> {
                override fun id2value(id: Int): GuardColorEx {
                    return enumValues<GuardColorEx>().find { it.id == id } ?: None
                }
                override fun value2id(v: GuardColorEx): Int {
                    return v.id
                }
            }

        }

        private fun <T:UtDialog> T.applyDialogParams():T {
            if(materialTheme.value == MaterialTheme.Legacy) {
                UtDialogConfig.dialogFrameId = io.github.toyota32k.dialog.R.layout.dialog_frame_legacy
            } else {
                UtDialogConfig.dialogFrameId = io.github.toyota32k.dialog.R.layout.dialog_frame
            }
//            setLimitWidth(400)
            isDialog = this@MainViewModel.isDialogMode.value
            edgeToEdgeEnabled = this@MainViewModel.edgeToEdgeEnabled.value
            this.hideStatusBarOnDialogMode = this@MainViewModel.hideStatusBarOnDialog.value
            cancellable = this@MainViewModel.cancellable.value
            draggable = this@MainViewModel.draggable.value
            guardColor = this@MainViewModel.guardColor.value.color
            when(dialogPosition.value) {
                DialogPosition.Full -> {
                    gravityOption = UtDialog.GravityOption.CENTER
                    widthOption = WidthFlag.FULL
                }
                DialogPosition.Left -> {
//                    widthOption = WidthOption.COMPACT
                    setLimitWidth(400)
                    gravityOption = UtDialog.GravityOption.LEFT_TOP
                }
                DialogPosition.Center -> {
                    setLimitWidth(400)
                    gravityOption = UtDialog.GravityOption.CENTER
                }
                DialogPosition.Right -> {
                    setFixedWidth(400)
                    gravityOption = UtDialog.GravityOption.RIGHT_TOP
                }
            }
            return this
        }

        private fun showCompactDialog() {
            UtImmortalSimpleTask.run("CompactDialog") {
                logger.debug("Showing: CompactDialog...")
                showDialog(CompactDialog::class.java.name) { CompactDialog().applyDialogParams() }
                logger.debug("Closed: CompactDialog")
                true
            }
        }

        private fun showAutoScrollDialog() {
            UtImmortalSimpleTask.run("AutoScrollDialog") {
                logger.debug("Showing: AutoScrollDialog...")
                showDialog(AutoScrollDialog::class.java.name) { AutoScrollDialog().applyDialogParams() }
                logger.debug("Closed: AutoScrollDialog")
                true
            }
        }
        private fun showFillHeightDialog() {
            UtImmortalSimpleTask.run("ShowFillHeightDialog") {
                logger.debug("Showing: FillDialog...")
                showDialog(FillDialog::class.java.name) { FillDialog().applyDialogParams() }
                logger.debug("Closed: FillDialog")
                true
            }
        }
        private fun showCustomDialog() {
            UtImmortalSimpleTask.run("ShowCustomDialog"){
                logger.debug("Showing: CustomDialog...")
                showDialog(CustomDialog::class.java.name) { CustomDialog().applyDialogParams() }
                logger.debug("Closed: CustomDialog")
                true
            }
        }
        private fun showMessageBox() {
            UtImmortalSimpleTask.run("MessageBox"){
                logger.debug("Message Box...")
                showYesNoMessageBox("Message Box", "Final Answer?")
                logger.debug("Closed: MessageBox")
                true
            }
        }
        private fun showRadioSelectionBox() {
            UtImmortalSimpleTask.run("RadioSelectionBox") {
                logger.debug("Radio Selection Box...")
                val sel = showDialog(taskName) {
                    UtRadioSelectionBox.create(
                        title = "Radio Selection Box",
                        items = arrayOf("Confirm", "OkCancel", "YesNo", "MultiSelection"),
                        initialSelection = 0)
                }.selectedItem
                logger.debug("Closed: RadioSelectionBox ($sel)")
                if(sel=="Confirm") {
                    showMessageBox()
                }
                true
            }
        }
    }

    private lateinit var controls: ActivityMainBinding
    private val binder = Binder()
    // theme を切り替えるたびに startActivityするので、Activityのライフサイクルではなくアプリのライフサイクルでビューモデルを構築しておく。
    private val viewModel:MainViewModel = ViewModelProvider(ApplicationViewModelStoreOwner.viewModelStore, ViewModelProvider.NewInstanceFactory())[MainViewModel::class.java]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(viewModel.edgeToEdgeEnabled.value) {
            enableEdgeToEdge()
        }
        UtStandardString.setContext(this, null)
        UtDialogConfig.solidBackgroundOnPhone = Config.solidBackgroundOnPhone       // true: Phoneのとき背景灰色(default) / false: tabletの場合と同じ
        UtDialogConfig.showInDialogModeAsDefault = Config.showInDialogModeAsDefault     // true: ダイアログモード / false:フラグメントモード(default)

        setTheme(viewModel.selectedTheme)
        viewModel.currentTheme = viewModel.selectedTheme

        controls = ActivityMainBinding.inflate(layoutInflater)
        setContentView(controls.root)

        if(viewModel.edgeToEdgeEnabled.value) {
            ViewCompat.setOnApplyWindowInsetsListener(controls.main) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                logger.debug("WindowInsets:left=${systemBars.left},top=${systemBars.top},right=${systemBars.right},bottom=${systemBars.bottom}")
                insets
            }
        }
        binder
            .owner(this)
            .activityStatusBarBinding(viewModel.showStatusBar)
            .activityActionBarBinding(viewModel.showActionBar)
            .checkBinding(controls.checkDialogMode, viewModel.isDialogMode)
            .checkBinding(controls.checkEdgeToEdge, viewModel.edgeToEdgeEnabled)
            .checkBinding(controls.checkNoActionBarTheme, viewModel.noActionBarTheme)
            .checkBinding(controls.checkHideStatusBarOnDialog, viewModel.hideStatusBarOnDialog)
            .checkBinding(controls.checkCancellable, viewModel.cancellable)
            .checkBinding(controls.checkDraggable, viewModel.draggable)
            .checkBinding(controls.checkStatusBar, viewModel.showStatusBar)
            .checkBinding(controls.checkActionBar, viewModel.showActionBar)
            .materialRadioButtonGroupBinding(controls.radioDialogPosition, viewModel.dialogPosition, MainViewModel.DialogPosition.IdValueResolver)
            .materialRadioButtonGroupBinding(controls.radioMaterialTheme, viewModel.materialTheme, MainViewModel.MaterialTheme.IdValueResolver)
            .materialRadioButtonGroupBinding(controls.radioGuardColor, viewModel.guardColor, MainViewModel.GuardColorEx.IdValueResolver)
            .visibilityBinding(controls.checkHideStatusBarOnDialog, viewModel.isDialogMode)
//            .combinatorialVisibilityBinding(viewModel.isDialogMode) {
//                straightGone(controls.checkHideStatusBarOnDialog)
//                inverseGone(controls.checkEdgeToEdge)
//            }
            .enableBinding(controls.checkActionBar, viewModel.noActionBarTheme, BoolConvert.Inverse)
            .bindCommand(viewModel.commandCompactDialog, controls.compactDialogButton)
            .bindCommand(viewModel.commandAutoScrollDialog, controls.autoScrollDialogButton)
            .bindCommand(viewModel.commandFillDialog, controls.fullHeightDialogButton)
            .bindCommand(viewModel.commandCustomDialog, controls.customDialogButton)
            .bindCommand(viewModel.commandMessageBox, controls.messageBoxButton)
            .clickBinding(controls.dummyActivityButton) {
                startActivity(Intent(this, DummyActivity::class.java))
            }
            .add(combine(viewModel.materialTheme, viewModel.noActionBarTheme) { theme, noActionBar -> if(noActionBar) theme.noActionBarThemeId else theme.themeId }.disposableObserve(this) {
                if(viewModel.currentTheme!=it) {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
            })
            .add(viewModel.edgeToEdgeEnabled.disposableObserve(this){
                if(viewModel.currentEdgeToEdgeEnabled!=it) {
                    viewModel.currentEdgeToEdgeEnabled = it
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
            })

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

//    val filePickers = UtFilePickerStore(this)

//    class FileTestTask:UtImmortalTaskBase(TASK_NAME) {
//        companion object {
//            const val TASK_NAME = "FileTestTask"
//            const val OPEN_DIRECTORY_CONNECTOR = "OpenDirectory"
//            const val OPEN_FILE_CONNECTOR = "OpenFile"
//        }
//
//        override suspend fun execute(): Boolean {
//            val dirUri = (getActivity() as? MainActivity)?.filePickers?.directoryPicker?.selectDirectory() ?: return false
//            withOwner { owner ->
//                val dir = DocumentFile.fromTreeUri(owner.asContext(), dirUri)
//                val file = dir?.createFile("text/plain", "xxx.txt")
//                if(file!=null) {
//                    withContext(Dispatchers.IO) {
//                        runCatching {
//                            owner.asContext().contentResolver.openOutputStream(file.uri)
//                                ?.use { stream ->
//                                    stream.write("piyopiyo".toByteArray())
//                                }
//                        }
//                    }
//                }
//            }
//
//            val fileUrl = (getActivity() as? MainActivity)?.filePickers?.openReadOnlyFilePicker?.selectFile() ?: return false
//            withOwner { owner->
//                withContext(Dispatchers.IO) {
//                    runCatching {
//                        owner.asContext().contentResolver.openInputStream(fileUrl)?.use { stream ->
//                            val line = stream.bufferedReader().readLine()
//                            logger.info(line)
//                        }
//                    }
//                    val file = DocumentFile.fromSingleUri(owner.asContext(),fileUrl)
//                    file?.delete()
//                }
//            }
//
//            logger.info("task completed")
//            return true
//        }
//    }

//    class BrokerTestTask:UtImmortalTaskBase(TASK_NAME) {
//        companion object {
//            const val TASK_NAME = "BrokerTestTask"
//        }
//
//        override suspend fun execute(): Boolean {
//            withOwner(MainActivity::class.java) {
//                logger.info("openFilePicker")
//                val activity = it.asActivity() as MainActivity
//                val uri = activity.filePickers.openFilePicker.selectFile(arrayOf("image/png", "image/jpeg", "application/pdf"))
//                logger.info("openFilePicker: $uri")
//            }
//            withOwner(MainActivity::class.java) {
//                logger.info("openReadOnlyFilePicker")
//                val activity = it.asActivity() as MainActivity
//                val uri = activity.filePickers.openReadOnlyFilePicker.selectFile("image/png")
//                logger.info("openReadOnlyFilePicker: $uri")
//            }
//            withOwner(MainActivity::class.java) {
//                logger.info("openMultiFilePicker")
//                val activity = it.asActivity() as MainActivity
//                val uris = activity.filePickers.openMultiFilePicker.selectFiles(arrayOf("image/png", "image/jpeg", "application/pdf"))
//                logger.info("openMultiFilePicker: ${
//                    uris.fold(StringBuilder()){ builder, uri->
//                        builder.append("\n")
//                        builder.append(uri.toString())
//                    }
//                }")
//            }
//            withOwner(MainActivity::class.java) {
//                logger.info("openReadOnlyMultiFilePicker")
//                val activity = it.asActivity() as MainActivity
//                val uris = activity.filePickers.openReadOnlyMultiFilePicker.selectFiles("image/jpeg")
//                logger.info("openReadOnlyMultiFilePicker: ${uris.fold(StringBuilder()){builder,uri->
//                    builder.append("\n")
//                    builder.append(uri.toString())
//                }}")
//            }
//            withOwner(MainActivity::class.java) {
//                logger.info("createFilePicker")
//                val activity = it.asActivity() as MainActivity
//                val uri = activity.filePickers.createFilePicker.selectFile("hoge.png","image/png")
//                logger.info("createFilePicker: $uri")
//            }
//            withOwner(MainActivity::class.java) {
//                logger.info("directoryPicker")
//                val activity = it.asActivity() as MainActivity
//                val uri = activity.filePickers.directoryPicker.selectDirectory()
//                logger.info("directoryPicker: $uri")
//            }
//            return true
//        }
//    }
//
//    private val activityCallTestSelectionReceptor = dialogHostManager.register<UtSingleSelectionBox>("activityCallTestSelectionReceptor") {
//        if(it.dialog.status.ok) {
//            when (it.dialog.selectedIndex) {
//                0 -> UtImmortalSimpleTask.run { filePickers.openFilePicker.selectFile() != null }
//                1 -> UtImmortalSimpleTask.run { filePickers.openMultiFilePicker.selectFiles().isNotEmpty() }
//                2 -> UtImmortalSimpleTask.run { filePickers.createFilePicker.selectFile("test.txt")!=null }
//                3 -> UtImmortalSimpleTask.run { filePickers.directoryPicker.selectDirectory()!=null }
//                4 -> FileTestTask().fire()
//                5 -> BrokerTestTask().fire()
//                else -> {}
//            }
//        }
//    }

}