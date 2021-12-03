package io.github.toyota32k.dialog

import android.content.Context
import androidx.annotation.StringRes
import java.lang.ref.WeakReference

enum class UtStandardString(@StringRes val id:Int) {
    OK(R.string.ok),
    CANCEL(R.string.cancel),
    CLOSE(R.string.close),
    DONE(R.string.done),
    YES(R.string.yes),
    NO(R.string.no),
    BACK(R.string.back);

    val text : String
        get() = context?.get()?.getString(id) ?: ""

    companion object {
        private var context:WeakReference<Context>? = null
        @JvmStatic
        fun setContext(context:Context) {
            this.context = WeakReference(context)
        }
    }
}