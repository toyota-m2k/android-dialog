package io.github.toyota32k

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.documentfile.provider.DocumentFile
import io.github.toyota32k.dialog.*
import io.github.toyota32k.dialog.connector.*
import io.github.toyota32k.sample.HogeDialog
import io.github.toyota32k.sample.SamplePortalDialog
import io.github.toyota32k.dialog.task.UtImmortalTaskManager
import io.github.toyota32k.dialog.task.UtMortalActivity
import io.github.toyota32k.utils.UtLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

class MainActivity : UtMortalActivity(), IUtActivityConnectorStore {
    override val logger = UtLog("SAMPLE")
//    private val dialogHostManager = UtDialogHostManager()

    override val immortalTaskNameList: Array<String> = arrayOf(SampleTask.TASK_NAME, FileTestTask.TASK_NAME)

    override fun notifyImmortalTaskResult(taskInfo: UtImmortalTaskManager.ITaskInfo) {
        logger.info("${taskInfo.name} ${taskInfo.state.value} ${taskInfo.result}")
        UtMessageBox.createForConfirm("Task Completed", "Task ${taskInfo.name} Result=${taskInfo.result}").show(this, "taskCompleted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UtStandardString.setContext(this)
        dialogHostManager["hoge"] = {
            logger.info("hoge:${it.status}")
        }
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.dialog_button).setOnClickListener {
            //UtMultiSelectionBox.select(this,"hoge", "タイトル", arrayOf("hoge", "fuga", "piyo"), booleanArrayOf(true,false,true), cancelLabel = getString(R.string.cancel))
            HogeDialog().show(this, "hoge")
        }
        findViewById<Button>(R.id.message_button).setOnClickListener {
            //UtMultiSelectionBox.select(this,"hoge", "タイトル", arrayOf("hoge", "fuga", "piyo"), booleanArrayOf(true,false,true), cancelLabel = getString(R.string.cancel))
            UtMessageBox.createForOkCancel("UtMessageBox", "テストです").show(this, "utmessage")
        }
        findViewById<Button>(R.id.rx_dialog_button).setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val r = RxDialog().showAsSuspendable(supportFragmentManager)
                logger.info("$r")
            }
        }
        findViewById<Button>(R.id.flow_test_button).setOnClickListener {
            flowTest()
        }
        findViewById<Button>(R.id.immortal_task_button).setOnClickListener {
            SampleTask().fire()
        }
        findViewById<Button>(R.id.catalogue).setOnClickListener {
            SamplePortalDialog().show(this, "Catalogue")
        }
        findViewById<Button>(R.id.activity_call).setOnClickListener {
            activityCallTestSelectionReceptor.showDialog(this) {
                UtSingleSelectionBox().apply {
                    title = "Activity Call Test"
                    items = arrayOf(
                        "Open File",
                        "Open Multiple Files",
                        "Create File",
                        "Select Folder",
                        "Process in ImmortalTask"
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

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


    private val openFilePicker = UtFileOpenPicker(this.toDialogOwner(), arrayOf("text/*")) { uri->
        logger.info("OpenFile: $uri")
        if(uri!=null) {
            contentResolver.openInputStream(uri)?.use { stream->
                val line = stream.bufferedReader().readLine()
                logger.info(line)
            }
        }

    }
    private val openMultiFilePicker = UtMultiFileOpenPicker(this.toDialogOwner(), arrayOf("application/*")) {
        logger.info("OpenMultipleFile: $it")
    }
    private val createFilePicker = UtFileCreatePicker(this.toDialogOwner(), "test.txt", null) { uri->
        logger.info("CreateFile: $uri")
        if(uri!=null) {
            contentResolver.openOutputStream(uri)?.use { stream->
                stream.write("hogehoge".toByteArray())
            }
        }
    }
    private var directoryUri:Uri? = null
    private val direcotryPicker = UtDirectoryPicker(this.toDialogOwner(), directoryUri) { uri->
        logger.info("Directory: $uri")
        if(uri!=null) {
            directoryUri = uri
            val dir = DocumentFile.fromTreeUri(this, uri) ?: return@UtDirectoryPicker
            val file = dir.createFile("text/plain", "x.txt") ?: return@UtDirectoryPicker
            contentResolver.openOutputStream(file.uri)?.use { stream->
                stream.write("fugafuga".toByteArray())
            }

        }
    }

    companion object {
        val activityConnectorFactoryBank = UtActivityConnectorFactoryBank(
            arrayOf(
                UtFileOpenPicker.Factory(FileTestTask.TASK_NAME, FileTestTask.OPEN_FILE_CONNECTOR, arrayOf("text/*")),
                UtDirectoryPicker.Factory(FileTestTask.TASK_NAME, FileTestTask.OPEN_DIRECTORY_CONNECTOR, null),
            ))
    }

    private val activityConnectorStore = activityConnectorFactoryBank.createConnectorStore(this.toDialogOwner())
    override fun getActivityConnector(immortalTaskName: String,connectorName: String): UtActivityConnector<*, *>? {
        return activityConnectorStore.getActivityConnector(immortalTaskName,connectorName)
    }

    class FileTestTask:UtActivityConnectorImmortalTaskBase(TASK_NAME) {
        companion object {
            const val TASK_NAME = "FileTestTask"
            const val OPEN_DIRECTORY_CONNECTOR = "OpenDirectory"
            const val OPEN_FILE_CONNECTOR = "OpenFile"
        }

        override suspend fun execute(): Boolean {
            val dirUri = launchDirectoryPicker(OPEN_DIRECTORY_CONNECTOR) ?: return false

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

            val fileUrl = launchFileOpenPicker(OPEN_FILE_CONNECTOR) ?: return false
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

    private val activityCallTestSelectionReceptor = dialogHostManager.register<UtSingleSelectionBox>("activityCallTestSelectionReceptor") {
        if(it.dialog.status.ok) {
            when (it.dialog.selectedIndex) {
                0 -> openFilePicker.launch()
                1 -> openMultiFilePicker.launch()
                2 -> createFilePicker.launch()
                3 -> direcotryPicker.launch()
                4 -> FileTestTask().fire()
                else -> {}
            }
        }
    }

}