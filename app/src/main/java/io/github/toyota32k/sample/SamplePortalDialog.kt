package io.github.toyota32k.sample

import android.os.Bundle
import android.view.View
import android.widget.Button
import io.github.toyota32k.R
import io.github.toyota32k.dialog.IUtDialogHost
import io.github.toyota32k.dialog.IUtDialogResultReceptor
import io.github.toyota32k.dialog.UtDialog
import io.github.toyota32k.dialog.UtDialogHostManager
import io.github.toyota32k.utils.UtLog

class SamplePortalDialog : UtDialog(),
    IUtDialogHost {
    companion object {
        val logger = UtLog("SAMPLE")
    }
    private val dialogHostManager = UtDialogHostManager()

    private val compactDialogReceptor = dialogHostManager.register<CompactDialog>("compactDialogReceptor") {
        if(it.dialog.status.ok) {
            logger.info("name=${it.dialog.name}")
        }
    }
    private val autoScrollDialogReceptor = dialogHostManager.register<AutoScrollDialog>("autoScrollDialogReceptor") {
        logger.info("count=${it.dialog.count}")
    }
    private val fillDialogReceptor = dialogHostManager.register<FillDialog>("fillDialogReceptor") {
        logger.info()
    }
    private val customDialogReceptor = dialogHostManager.register<CustomDialog>("customDialogReceptor") {
        logger.info()
    }
    private val focusDialogReceptor = dialogHostManager.register<FocusDialog>("customDialogReceptor") {
        logger.info()
    }

    override fun queryDialogResultReceptor(tag: String): IUtDialogResultReceptor? {
        return dialogHostManager.queryDialogResultReceptor(tag)
    }

    init {
        title = "Dialog Catalogue"
        setLeftButton(BuiltInButtonType.CLOSE_LEFT)
    }

    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        return inflater.inflate(R.layout.sample_portal_dialog).apply {
            findViewById<Button>(R.id.compact).setOnClickListener {
                compactDialogReceptor.showDialog(this@SamplePortalDialog) { CompactDialog() }
            }
            findViewById<Button>(R.id.auto_scroll).setOnClickListener {
                autoScrollDialogReceptor.showDialog(this@SamplePortalDialog) { AutoScrollDialog() }
            }
            findViewById<Button>(R.id.fill).setOnClickListener {
                fillDialogReceptor.showDialog(this@SamplePortalDialog) { FillDialog() }
            }
            findViewById<Button>(R.id.custom).setOnClickListener {
                customDialogReceptor.showDialog(this@SamplePortalDialog) { CustomDialog() }
            }
            findViewById<Button>(R.id.focus_management).setOnClickListener {
                focusDialogReceptor.showDialog(this@SamplePortalDialog) { FocusDialog() }
            }
        }
    }

}