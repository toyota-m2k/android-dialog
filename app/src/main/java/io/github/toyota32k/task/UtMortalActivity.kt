package io.github.toyota32k.task

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import io.github.toyota32k.dialog.toDialogOwner

/**
 * ImmortalTask と協調動作するActivityの基本実装
 */
abstract class UtMortalActivity : FragmentActivity() {
    protected abstract val immortalTaskNameList:Array<String>
    protected abstract val mortalActivityName:String
    protected abstract fun notifyImmortalTaskResult(taskInfo: UtImmortalTaskManager.ITaskInfo?)

    override fun onResume() {
        super.onResume()

        // ImmortalTask に接続する
        for(name in immortalTaskNameList) {
            UtImmortalTaskManager.onOwnerResumed(name, mortalActivityName, toDialogOwner()).let { observeImmortalTask(name,it.state) }
        }
    }

    override fun onPause() {
        super.onPause()
        for(name in immortalTaskNameList) {
            UtImmortalTaskManager.onOwnerPaused(name, mortalActivityName, toDialogOwner())
        }
    }

    /**
     * ImmortalTask の状態変化を受け取るハンドラ
     */
    open fun onImmortalTaskStateChanged(taskName:String, state:UtImmortalTaskState) {
        if(state.finished) {
            notifyImmortalTaskResult(UtImmortalTaskManager.taskOf(taskName))
            UtImmortalTaskManager.disposeTask(taskName, mortalActivityName, toDialogOwner())
        }
    }

    private fun observeImmortalTask(taskName:String, liveData: LiveData<UtImmortalTaskState>) {
        logger.debug("")
        liveData.observe(this) {
            if(it!=null) {
                onImmortalTaskStateChanged(taskName, it)
            }
        }
    }

    companion object {
        val logger = UtImmortalTaskManager.logger
    }
}