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
 * 読み取り専用に複数のファイルを選択
 * ACTION_GET_CONTENT
 */
open class UtOpenReadOnlyMultiFilePicker : UtActivityBroker<String, List<Uri>?>() {

    protected open fun prepareChooserIntent(intent: Intent): Intent {
        return Intent.createChooser(intent,"Choose files")
    }

    protected  inner class Contract : ActivityResultContracts.GetMultipleContents() {
        override fun createIntent(context: Context, input: String): Intent {
            val intent = super.createIntent(context, input)
            return prepareChooserIntent(intent)
        }
    }

    override val contract: ActivityResultContract<String, List<Uri>?>
        get() = Contract()

    suspend fun selectFiles(context:UtImmortalTaskContext, mimeType:String = UtOpenReadOnlyFilePicker.defaultMimeType): List<Uri>? {
        return invoke(context, mimeType)
    }
}