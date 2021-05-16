package io.github.toyota32k

import io.github.toyota32k.dialog.UtMessageBox
import io.github.toyota32k.task.UtImmortalTaskBase
import kotlinx.coroutines.delay

class SampleTask : UtImmortalTaskBase(TASK_NAME) {
    companion object {
        val TASK_NAME = SampleTask::class.java.name
    }

    override var taskResult:Any? = null

    override suspend fun execute(): Boolean {
        logger.info("waiting")
        delay(3*1000)
        logger.info("dialog")
        val r = showDialog(TASK_NAME) {
            UtMessageBox.createForOkCancel("Suspend Dialog", "Are you ready?")
        }
        delay(3*1000)
        taskResult = r?.status?.ok ?: false
        return true
    }
}