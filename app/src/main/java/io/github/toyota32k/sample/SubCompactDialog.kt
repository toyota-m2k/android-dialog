package io.github.toyota32k.sample

import android.os.Bundle
import android.view.View
import io.github.toyota32k.R
import io.github.toyota32k.dialog.IUtDialog
import io.github.toyota32k.dialog.UtDialog
import io.github.toyota32k.dialog.task.UtImmortalSimpleTask

class SubCompactDialog: UtDialog() {
    init {
        title="サブダイアログ"
        setLimitWidth(400)
        draggable = true
        heightOption=HeightFlag.COMPACT
//        guardColor = GuardColor.DIM.color
        setLeftButton(BuiltInButtonType.CANCEL)
        setRightButton(BuiltInButtonType.DONE)
        parentVisibilityOption = ParentVisibilityOption.NONE
        gravityOption = GravityOption.CENTER
    }

    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        return inflater.inflate(R.layout.sample_sub_dialog)
    }

    companion object {
        fun open(isDialog:Boolean) {
            UtImmortalSimpleTask.run(SubCompactDialog::class.java.name) {
                showDialog<IUtDialog>(taskName) {
                    SubCompactDialog().also { dlg-> dlg.isDialog = isDialog }
                }.status.ok
            }
        }
    }
}