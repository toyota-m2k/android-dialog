package io.github.toyota32k.dialog.broker.pickers

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo
import androidx.fragment.app.FragmentActivity
import io.github.toyota32k.dialog.broker.IUtActivityLauncher
import io.github.toyota32k.dialog.broker.UtActivityBroker

open class UtMediaFilePicker(owner: ActivityResultCaller? = null) : UtActivityBroker<PickVisualMediaRequest, Uri?>(owner) {
    companion object {
        @JvmStatic
        fun launcher(owner: FragmentActivity, callback: ActivityResultCallback<Uri?>) : IUtActivityLauncher<PickVisualMediaRequest> {
            return UtMediaFilePicker().apply {
                register(owner, callback)
            }
        }
    }
    protected open fun prepareChooserIntent(intent:Intent):Intent {
        return Intent.createChooser(intent, "Choose a media file")
    }

    protected inner class Contract : ActivityResultContracts.PickVisualMedia() {
        override fun createIntent(context: Context, input: PickVisualMediaRequest): Intent {
            val intent = super.createIntent(context, input)
            return prepareChooserIntent(intent)
        }
    }

    override val contract: ActivityResultContract<PickVisualMediaRequest, Uri?>
        get() = Contract()

    suspend fun selectMediaFile(mediaType: ActivityResultContracts.PickVisualMedia.VisualMediaType=ImageAndVideo):Uri? {
        return invoke(PickVisualMediaRequest.Builder().setMediaType(mediaType).build())
    }
    suspend fun selectImage(): Uri? {
        return selectMediaFile(ActivityResultContracts.PickVisualMedia.ImageOnly)
    }
    suspend fun selectVideo(): Uri? {
        return selectMediaFile(ActivityResultContracts.PickVisualMedia.VideoOnly)
    }
    suspend fun selectMedia(): Uri? {
        return selectMediaFile()
    }
}