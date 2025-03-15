package io.github.toyota32k.dialog.sample

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.IdRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.toyota32k.binder.Binder
import io.github.toyota32k.binder.IIDValueResolver
import io.github.toyota32k.dialog.UtDialog
import io.github.toyota32k.dialog.UtDialog.WidthFlag
import io.github.toyota32k.dialog.UtDialog.WidthOption
import io.github.toyota32k.dialog.UtDialogConfig
import io.github.toyota32k.utils.ApplicationViewModelStoreOwner
import kotlinx.coroutines.flow.MutableStateFlow

class OptionActivity : AppCompatActivity() {
    class OptionActivityViewModel : ViewModel() {
        val isDialogMode = MutableStateFlow(true)
        val noActionBarTheme = MutableStateFlow(false)
        val edgeToEdgeEnabled = MutableStateFlow(true)
        val hideStatusBarOnDialog = MutableStateFlow(true)
        val cancellable = MutableStateFlow(true)
        val draggable = MutableStateFlow(true)
        val showStatusBar = MutableStateFlow(false)
        val showActionBar = MutableStateFlow(false)
        val dialogPosition = MutableStateFlow(DialogPosition.Center)
        val materialTheme = MutableStateFlow(MaterialTheme.Legacy)
        val guardColor = MutableStateFlow(GuardColorEx.None)
        var currentTheme = R.style.Theme_DialogSample_Legacy
        val selectedTheme:Int get() = if(noActionBarTheme.value) materialTheme.value.noActionBarThemeId else materialTheme.value.themeId
        var currentEdgeToEdgeEnabled = true

        enum class DialogPosition(@IdRes val id:Int) {
            Full(R.id.radio_fit_screen_width),
            Left(R.id.radio_left),
            Center(R.id.radio_center),
            Right(R.id.radio_right),
            ;
            object IdValueResolver : IIDValueResolver<DialogPosition> {
                override fun id2value(id: Int): DialogPosition {
                    return enumValues<DialogPosition>().find { it.id == id } ?: Right
                }
                override fun value2id(v: DialogPosition): Int {
                    return v.id
                }
            }
        }
        enum class MaterialTheme(@IdRes val id:Int, @StyleRes val themeId:Int, @StyleRes val noActionBarThemeId:Int) {
            Legacy(R.id.radio_material2, R.style.Theme_DialogSample_Legacy, R.style.Theme_DialogSample_Legacy_NoActionBar),
            Material3(R.id.radio_material3, R.style.Theme_DialogSample_Material3, R.style.Theme_DialogSample_Material3_NoActionBar),
            DynamicColor(R.id.radio_dynamic_color, R.style.Theme_DialogSample_DynamicColor, R.style.Theme_DialogSample_DynamicColor_NoActionBar),
            ;
            object IdValueResolver : IIDValueResolver<MaterialTheme> {
                override fun id2value(id: Int): MaterialTheme {
                    return enumValues<MaterialTheme>().find { it.id == id } ?: Legacy
                }
                override fun value2id(v: MaterialTheme): Int {
                    return v.id
                }
            }
        }
        enum class GuardColorEx(@IdRes val id:Int, val color:Int) {
            None(R.id.radio_guard_color_none, UtDialog.GuardColor.INVALID.color),
            Dim(R.id.radio_guard_color_dim, UtDialog.GuardColor.DIM.color),
            SeeThrough(R.id.radio_guard_color_see_through, UtDialog.GuardColor.SEE_THROUGH.color),
            AutoDim(R.id.radio_guard_color_auto, UtDialog.GuardColor.THEME_DIM.color),
            AutoST(R.id.radio_guard_color_auto_s, UtDialog.GuardColor.THEME_SEE_THROUGH.color)
            ;
            object IdValueResolver : IIDValueResolver<GuardColorEx> {
                override fun id2value(id: Int): GuardColorEx {
                    return enumValues<GuardColorEx>().find { it.id == id } ?: None
                }
                override fun value2id(v: GuardColorEx): Int {
                    return v.id
                }
            }

        }

        private fun <T:UtDialog> T.applyDialogParams():T {
            if(materialTheme.value == MaterialTheme.Legacy) {
                UtDialogConfig.dialogFrameId = io.github.toyota32k.dialog.R.layout.dialog_frame_legacy
            } else {
                UtDialogConfig.dialogFrameId = io.github.toyota32k.dialog.R.layout.dialog_frame
            }
//            setLimitWidth(400)
            isDialog = this@OptionActivityViewModel.isDialogMode.value
            edgeToEdgeEnabled = this@OptionActivityViewModel.edgeToEdgeEnabled.value
            this.hideStatusBarOnDialogMode = this@OptionActivityViewModel.hideStatusBarOnDialog.value
            cancellable = this@OptionActivityViewModel.cancellable.value
            draggable = this@OptionActivityViewModel.draggable.value
            guardColor = this@OptionActivityViewModel.guardColor.value.color
            when(dialogPosition.value) {
                DialogPosition.Full -> {
                    gravityOption = UtDialog.GravityOption.CENTER
                    widthOption = WidthOption.FULL
                }
                DialogPosition.Left -> {
                    widthOption = UtDialog.WidthOption.LIMIT(400)
                    gravityOption = UtDialog.GravityOption.LEFT_TOP
                }
                DialogPosition.Center -> {
                    widthOption = UtDialog.WidthOption.LIMIT(400)
                    gravityOption = UtDialog.GravityOption.CENTER
                }
                DialogPosition.Right -> {
                    widthOption = UtDialog.WidthOption.LIMIT(400)
                    gravityOption = UtDialog.GravityOption.RIGHT_TOP
                }
            }
            return this
        }
    }

    private val binder = Binder()
    // theme を切り替えるたびに startActivityするので、Activityのライフサイクルではなくアプリのライフサイクルでビューモデルを構築しておく。
    private val viewModel:OptionActivityViewModel = ViewModelProvider(ApplicationViewModelStoreOwner.viewModelStore, ViewModelProvider.NewInstanceFactory())[OptionActivityViewModel::class.java]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_option)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}