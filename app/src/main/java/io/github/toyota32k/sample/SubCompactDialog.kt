package io.github.toyota32k.sample

import android.os.Bundle
import android.view.View
import io.github.toyota32k.R
import io.github.toyota32k.dialog.IUtDialog
import io.github.toyota32k.dialog.UtDialog
import io.github.toyota32k.dialog.task.UtImmortalTask

class SubCompactDialog: UtDialog() {
    override fun preCreateBodyView() {
        title="サブダイアログ"
        heightOption = HeightOption.COMPACT
        widthOption = WidthOption.LIMIT(400)
        leftButtonType = ButtonType.CANCEL
        rightButtonType = ButtonType.DONE
        draggable = true
        parentVisibilityOption = ParentVisibilityOption.NONE
    }

    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        return inflater.inflate(R.layout.sample_sub_dialog)
    }

    companion object {
        fun open(isDialog:Boolean) {
            UtImmortalTask.launchTask(SubCompactDialog::class.java.name) {
                showDialog<IUtDialog>(taskName) {
                    SubCompactDialog().also { dlg-> dlg.isDialog = isDialog }
                }.status.ok
            }
        }
    }
}