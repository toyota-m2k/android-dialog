package io.github.toyota32k

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.toyota32k.databinding.ActivityDummyBinding
import io.github.toyota32k.utils.UtLog
import io.github.toyota32k.utils.hideActionBar
import io.github.toyota32k.utils.hideStatusBar

class DummyActivity : AppCompatActivity() {
    val logger = UtLog("DummyActivity")
    private lateinit var controls: ActivityDummyBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        controls = ActivityDummyBinding.inflate(layoutInflater)
        setContentView(controls.root)
        ViewCompat.setOnApplyWindowInsetsListener(controls.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            logger.debug("WindowInsets:left=${systemBars.left},top=${systemBars.top},right=${systemBars.right},bottom=${systemBars.bottom}")
            insets
        }
        hideActionBar()
        hideStatusBar()
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON  // スリープしない
                    or WindowManager.LayoutParams.FLAG_SECURE  // キャプチャーを禁止（タスクマネージャで見えないようにする）
        )

    }
}