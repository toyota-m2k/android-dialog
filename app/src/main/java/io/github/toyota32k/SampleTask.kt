package io.github.toyota32k

import io.github.toyota32k.dialog.task.UtImmortalTaskBase
import kotlinx.coroutines.delay

class SampleTask : UtImmortalTaskBase(TASK_NAME) {
    companion object {
        val TASK_NAME: String = SampleTask::class.java.name
    }

    override var taskResult:Any? = null

    override suspend fun execute(): Boolean {
        logger.info("waiting")
        delay(3*1000L)
        logger.info("dialog")
        val r = showDialog(TASK_NAME) {
            io.github.toyota32k.dialog.UtMessageBox.createForOkCancel("Suspend Dialog", "Are you ready?")
        }
        delay(3*1000L)
        taskResult = r.status.ok
        return true
    }
}