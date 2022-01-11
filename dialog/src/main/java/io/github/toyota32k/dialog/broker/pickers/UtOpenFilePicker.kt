package io.github.toyota32k.dialog.broker.pickers

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import io.github.toyota32k.dialog.broker.UtActivityBroker
import io.github.toyota32k.dialog.task.UtImmortalTaskContext
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 読み書き用にファイルを選択する
 * ACTION_OPEN_DOCUMENT
 * データの編集を行うなど、長期間の永続的なアクセスが必要な場合に使用。
 */
open class UtOpenFilePicker : UtActivityBroker<Array<String>,Uri?>() {
    companion object {
        val defaultMimeTypes: Array<String> = arrayOf("*/*")
    }

    protected open fun prepareChooserIntent(intent:Intent):Intent {
        return Intent.createChooser(intent, "Choose a file")
    }

    protected inner class Contract : ActivityResultContracts.OpenDocument() {
        override fun createIntent(context: Context, input: Array<out String>): Intent {
            val intent = super.createIntent(context, input)
            return prepareChooserIntent(intent)
        }
    }

    override val contract: ActivityResultContract<Array<String>, Uri?>
        get() = Contract()

    suspend fun selectFile(context: UtImmortalTaskContext, mimeTypes:Array<String> = defaultMimeTypes):Uri? {
        return invoke(context, mimeTypes)
    }
}

