package io.github.toyota32k.dialog.connector

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import io.github.toyota32k.dialog.task.UtImmortalTaskManager
import java.lang.IllegalStateException

abstract  class UtActivityConnector<I,O>(private val launcher: ActivityResultLauncher<I>, val defArg:I) {
    @JvmOverloads
    fun launch(arg:I = defArg) {
        launcher.launch(arg)
    }

    class ImmortalResultCallback<O>(private val immortalTaskName:String): ActivityResultCallback<O> {
        override fun onActivityResult(result: O) {
            val entry = UtImmortalTaskManager.taskOf(immortalTaskName) ?: throw IllegalStateException("no task named '$immortalTaskName'")
            val task = entry.task ?: throw IllegalStateException("no task attached to '$immortalTaskName'")
            task.resumeTask(result)
        }
    }
}
