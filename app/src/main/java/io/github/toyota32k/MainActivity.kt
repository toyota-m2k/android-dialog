package io.github.toyota32k

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import io.github.toyota32k.dialog.IUtDialogHost
import io.github.toyota32k.dialog.IUtDialogResultReceptor
import io.github.toyota32k.dialog.UtDialogHostManager
import io.github.toyota32k.dialog.UtStandardString
import io.github.toyota32k.sample.HogeDialog
import io.github.toyota32k.utils.UtLog

class MainActivity : AppCompatActivity(), IUtDialogHost {
    val logger = UtLog("SAMPLE")
    val dialogHostManager = UtDialogHostManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UtStandardString.setContext(this)
        dialogHostManager["hoge"] = {
            logger.info("hoge:${it.status}")
        }
        setContentView(R.layout.activity_main)
        val btn = findViewById<Button>(R.id.message_button)
        btn.setOnClickListener {
            //UtMultiSelectionBox.select(this,"hoge", "タイトル", arrayOf("hoge", "fuga", "piyo"), booleanArrayOf(true,false,true), cancelLabel = getString(R.string.cancel))
            HogeDialog().show(this, "hoge")
        }
    }

    override fun queryDialogResultReceptor(tag: String): IUtDialogResultReceptor? {
        return dialogHostManager.queryDialogResultReceptor(tag)
    }
}