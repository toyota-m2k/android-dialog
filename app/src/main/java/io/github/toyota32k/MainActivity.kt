package io.github.toyota32k

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import io.github.toyota32k.dialog.*
import io.github.toyota32k.sample.HogeDialog
import io.github.toyota32k.utils.UtLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

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
        findViewById<Button>(R.id.dialog_button).setOnClickListener {
            //UtMultiSelectionBox.select(this,"hoge", "タイトル", arrayOf("hoge", "fuga", "piyo"), booleanArrayOf(true,false,true), cancelLabel = getString(R.string.cancel))
            HogeDialog().show(this, "hoge")
        }
        findViewById<Button>(R.id.message_button).setOnClickListener {
            //UtMultiSelectionBox.select(this,"hoge", "タイトル", arrayOf("hoge", "fuga", "piyo"), booleanArrayOf(true,false,true), cancelLabel = getString(R.string.cancel))
            UtMessageBox.createForOkCancel("UtMessageBox", "テストです").show(this, "utmessage")
        }
        findViewById<Button>(R.id.rx_dialog_button).setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val r = RxDialog().showAsSuspendable(supportFragmentManager)
                logger.info("$r")
            }
        }
        findViewById<Button>(R.id.flow_test_button).setOnClickListener {
            flowTest()
        }
    }

    override fun queryDialogResultReceptor(tag: String): IUtDialogResultReceptor? {
        return dialogHostManager.queryDialogResultReceptor(tag)
    }

    private fun flowTest() {
        val flow = MutableStateFlow<Int>(0)
        GlobalScope.launch {
            val v = flow.filter { it>=5 }.first()
            logger.info("flow:$v")
        }

        GlobalScope.launch {
            delay(500)
            for (n in 1..10) {
                logger.info("emit:$n")
                flow.value = n
                delay(100)
            }
        }
    }
}