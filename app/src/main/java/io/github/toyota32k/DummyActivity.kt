package io.github.toyota32k

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import io.github.toyota32k.MainActivity.MainViewModel
import io.github.toyota32k.databinding.ActivityDummyBinding
import io.github.toyota32k.utils.ApplicationViewModelStoreOwner
import io.github.toyota32k.utils.UtLog
import io.github.toyota32k.utils.hideActionBar
import io.github.toyota32k.utils.hideStatusBar

class DummyActivity : AppCompatActivity() {
    val logger = UtLog("DummyActivity")
    private val viewModel: MainViewModel = ViewModelProvider(ApplicationViewModelStoreOwner.viewModelStore, ViewModelProvider.NewInstanceFactory())[MainViewModel::class.java]

    private lateinit var controls: ActivityDummyBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setTheme(viewModel.selectedTheme)
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
    }
}