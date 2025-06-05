package io.github.toyota32k.dialog

import android.content.Context
import androidx.annotation.StringRes
import io.github.toyota32k.dialog.task.UtImmortalTaskManager
import io.github.toyota32k.utils.android.getStringOrDefault
import io.github.toyota32k.utils.android.getStringOrNull
import java.lang.ref.WeakReference

interface IUtStringTable {
    @StringRes
    fun resId(type: UtStandardString): Int

    @StringRes
    operator fun get(str:UtStandardString):Int {
        return resId(str)
    }
}

enum class UtStandardString(@StringRes private val resId:Int, val defaultText:String) {
    OK(R.string.ut_dialog_ok, "OK"),
    CANCEL(R.string.ut_dialog_cancel, "Cancel"),
    CLOSE(R.string.ut_dialog_close, "Close"),
    DONE(R.string.ut_dialog_done, "Done"),
    YES(R.string.ut_dialog_yes, "Yes"),
    NO(R.string.ut_dialog_no, "No"),
    BACK(R.string.ut_dialog_back, "Back"),
    NONE(0, "");

    val text : String
        get() = getText(this)

    val id : Int @StringRes
        get() = getId(this)

    companion object {
        private var contextRef:WeakReference<Context>? = null
        private var table:IUtStringTable? = null
        @JvmStatic
        @JvmOverloads
        fun setContext(context:Context, table:IUtStringTable?=null) {
            this.contextRef = WeakReference(context)
            this.table = table
        }
        @StringRes
        private fun getId(type:UtStandardString) : Int {
            return table?.get(type) ?: type.resId
        }
        private val context get() = contextRef?.get() ?: UtImmortalTaskManager.application
        private fun getText(type:UtStandardString) : String {
            return context.getStringOrDefault(getId(type), type.defaultText)
        }
        fun getText(@StringRes id:Int):String? {
            return context.getStringOrNull(id)
        }
    }
}
