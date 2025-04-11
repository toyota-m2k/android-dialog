package io.github.toyota32k.dialog.sample

import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.toyota32k.binder.Binder
import io.github.toyota32k.binder.BoolConvert
import io.github.toyota32k.binder.activityActionBarBinding
import io.github.toyota32k.binder.activityStatusBarBinding
import io.github.toyota32k.binder.checkBinding
import io.github.toyota32k.binder.command.LiteUnitCommand
import io.github.toyota32k.binder.command.bindCommand
import io.github.toyota32k.binder.editTextBinding
import io.github.toyota32k.binder.enableBinding
import io.github.toyota32k.binder.observe
import io.github.toyota32k.binder.spinnerBinding
import io.github.toyota32k.dialog.UtDialog
import io.github.toyota32k.dialog.UtDialog.GravityOption
import io.github.toyota32k.dialog.UtDialog.HeightOption
import io.github.toyota32k.dialog.UtDialog.WidthOption
import io.github.toyota32k.dialog.UtDialogBase
import io.github.toyota32k.dialog.UtDialogConfig
import io.github.toyota32k.dialog.mortal.UtMortalActivity
import io.github.toyota32k.dialog.sample.OptionActivity.OptionActivityViewModel.DarkLightInfo
import io.github.toyota32k.dialog.sample.OptionActivity.OptionActivityViewModel.DialogMarginInfo
import io.github.toyota32k.dialog.sample.OptionActivity.OptionActivityViewModel.GravityOptionInfo
import io.github.toyota32k.dialog.sample.OptionActivity.OptionActivityViewModel.GuardColorInfo
import io.github.toyota32k.dialog.sample.OptionActivity.OptionActivityViewModel.HeightOptionInfo
import io.github.toyota32k.dialog.sample.OptionActivity.OptionActivityViewModel.ThemeInfo
import io.github.toyota32k.dialog.sample.OptionActivity.OptionActivityViewModel.WidthOptionInfo
import io.github.toyota32k.dialog.sample.databinding.ActivityOptionBinding
import io.github.toyota32k.dialog.sample.dialog.FullHeightDialog
import io.github.toyota32k.dialog.sample.dialog.OptionSampleDialog
import io.github.toyota32k.dialog.sample.dialog.ThemeColorDialog
import io.github.toyota32k.dialog.task.UtImmortalTask.Companion.launchTask
import io.github.toyota32k.dialog.task.createViewModel
import io.github.toyota32k.utils.ApplicationViewModelStoreOwner
import io.github.toyota32k.utils.hideActionBar
import io.github.toyota32k.utils.hideStatusBar
import io.github.toyota32k.utils.isActionBarVisible
import io.github.toyota32k.utils.isStatusBarVisible
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class OptionActivity : UtMortalActivity() {
    class OptionActivityViewModel : ViewModel() {
        // Activity Settings
        val themeInfo = MutableStateFlow(ThemeInfo.DYNAMIC_NOACTION_BAR)
        val darkLightInfo = MutableStateFlow(DarkLightInfo.SYSTEM)
        val showStatusBar = MutableStateFlow(true)
        val showActionBar = MutableStateFlow(true)
        val edgeToEdgeEnabled = MutableStateFlow(true)

        // Global Options
        val isDialogMode = MutableStateFlow(UtDialogConfig.showInDialogModeAsDefault)
        val portraitMarginInfo = MutableStateFlow(DialogMarginInfo.DEFAULT_PORTRAIT)
        val landscapeMarginInfo = MutableStateFlow(DialogMarginInfo.DEFAULT_LANDSCAPE)

        // Dialog Options
        val dialogTitle = MutableStateFlow("Sample Dialog")
        val widthOptionInfo = MutableStateFlow(WidthOptionInfo.COMPACT)
        val heightOptionInfo = MutableStateFlow(HeightOptionInfo.COMPACT)
        val gravityOptionInfo = MutableStateFlow(GravityOptionInfo.CENTER)
        val guardColorInfo = MutableStateFlow(GuardColorInfo.DEFAULT)
        val bodyGuardColorInfo = MutableStateFlow(GuardColorInfo.DEFAULT)
        val progressRingOnBodyGuardView = MutableStateFlow(false)
        val scrollable = MutableStateFlow(false)
        val cancellable = MutableStateFlow(true)
        val draggable = MutableStateFlow(false)
        val noHeader = MutableStateFlow(false)
        val noFooter = MutableStateFlow(false)
        val hideStatusBarOnDialog = MutableStateFlow(UtDialogConfig.hideStatusBarOnDialogMode)
        val systemBarOptionOnFragmentMode = MutableStateFlow(UtDialogConfig.systemBarOptionOnFragmentMode)

        val hasActionBar = themeInfo.map { it.hasActionBar }
        val isMaterial3 = themeInfo.map { it.material3 }

        data class ThemeInfo(val label: String, @StyleRes val themeId: Int, val hasActionBar: Boolean, val material3:Boolean=true) {
            override fun toString():String {
                return label
            }
            companion object {
                val MATERIAL2 = ThemeInfo("Material 2", R.style.Theme_DialogSample_Material2, true, false)
                val MATERIAL3 = ThemeInfo("Material 3", R.style.Theme_DialogSample_Material3, true)
                val DYNAMIC = ThemeInfo("Dynamic", R.style.Theme_DialogSample_DynamicColor, true)
                val MATERIAL2_NO_ACTION_BAR = ThemeInfo("Material 2-NoActionBar", R.style.Theme_DialogSample_Material2_NoActionBar, false)
                val MATERIAL3_NO_ACTION_BAR = ThemeInfo("Material 3-NoActionBar", R.style.Theme_DialogSample_Material3_NoActionBar, false)
                val DYNAMIC_NOACTION_BAR = ThemeInfo("Dynamic-NoActionBar", R.style.Theme_DialogSample_DynamicColor_NoActionBar, false)

                val values = listOf(MATERIAL2, MATERIAL3, DYNAMIC, MATERIAL2_NO_ACTION_BAR, MATERIAL3_NO_ACTION_BAR, DYNAMIC_NOACTION_BAR)
            }
        }

        data class DarkLightInfo(val label: String, val mode: Int) {
            override fun toString():String {
                return label
            }
            companion object {
                val SYSTEM = DarkLightInfo("System", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                val LIGHT = DarkLightInfo("Light", AppCompatDelegate.MODE_NIGHT_NO)
                val DARK = DarkLightInfo("Dark", AppCompatDelegate.MODE_NIGHT_YES)

                val values = listOf(SYSTEM, LIGHT, DARK)
            }
        }

        data class DialogMarginInfo(val label: String, val margin: Rect?) {
            override fun toString():String {
                return label
            }
            companion object {
                val DEFAULT_PORTRAIT = DialogMarginInfo("Default", UtDialogConfig.dialogMarginOnPortrait?:Rect(20, 40, 20, 40))
                val DEFAULT_LANDSCAPE = DialogMarginInfo("Default", UtDialogConfig.dialogMarginOnLandscape?:Rect(40, 20, 40, 20))
                val ZERO = DialogMarginInfo("Zero", null)
                val CUSTOM = DialogMarginInfo("Custom (150dp)", Rect(150, 150, 150, 150))
                val landscapeValues = listOf(DEFAULT_LANDSCAPE, ZERO, CUSTOM)
                val portraitValues = listOf(DEFAULT_PORTRAIT, ZERO, CUSTOM)
            }
        }

        data class WidthOptionInfo(val label: String, val option: WidthOption) {
            override fun toString():String {
                return label
            }
            companion object {
                val COMPACT = WidthOptionInfo("COMPACT", WidthOption.COMPACT)
                val FULL = WidthOptionInfo("FULL", WidthOption.FULL)
                val FIXED_300 = WidthOptionInfo("FIXED (300dp)", WidthOption.FULL)
                val LIMIT_400 = WidthOptionInfo("LIMIT (400dp)", WidthOption.LIMIT(400))

                val values = listOf(COMPACT, FULL, FIXED_300, LIMIT_400)
            }
        }

        data class HeightOptionInfo(val label: String, val option: HeightOption) {
            override fun toString():String {
                return label
            }

            companion object {
                val COMPACT = HeightOptionInfo("COMPACT", HeightOption.COMPACT)
                val FULL = HeightOptionInfo("FULL", HeightOption.FULL)
                val AUTO_SCROLL = HeightOptionInfo("AUTO_SCROLL", HeightOption.AUTO_SCROLL)
                val FIXED_300 = HeightOptionInfo("FIXED (300dp)", HeightOption.FULL)
                val LIMIT_400 = HeightOptionInfo("LIMIT (400dp)", HeightOption.LIMIT(400))

                val values = listOf(COMPACT, FULL, AUTO_SCROLL, FIXED_300, LIMIT_400)
            }
        }

        data class GravityOptionInfo(val label: String, val option: GravityOption) {
            override fun toString():String {
                return label
            }
            companion object {
                val CENTER = GravityOptionInfo("CENTER", GravityOption.CENTER)
                val LEFT_TOP = GravityOptionInfo("LEFT_TOP", GravityOption.LEFT_TOP)
                val RIGHT_TOP = GravityOptionInfo("RIGHT_TOP", GravityOption.RIGHT_TOP)
                val CUSTOM = GravityOptionInfo("CUSTOM", GravityOption.CUSTOM)
                val values = listOf(CENTER, LEFT_TOP, RIGHT_TOP)
            }
        }

        data class GuardColorInfo(val label: String, val color: UtDialog.GuardColor) {
            override fun toString():String {
                return label
            }

            companion object {
                val DEFAULT = GuardColorInfo("Default", UtDialog.GuardColor.INVALID)
                val DIM = GuardColorInfo("DIM", UtDialog.GuardColor.DIM)
                val SEE_THROUGH = GuardColorInfo("SEE_THROUGH", UtDialog.GuardColor.SEE_THROUGH)
                val SOLID_GRAY = GuardColorInfo("SOLID_GRAY", UtDialog.GuardColor.SOLID_GRAY)
                val AUTO_DIM = GuardColorInfo("AUTO_DIM", UtDialog.GuardColor.THEME_DIM)
                val AUTO_SEE_THROUGH = GuardColorInfo("THEME_SEE_THROUGH", UtDialog.GuardColor.THEME_SEE_THROUGH)
                val CUSTOM = GuardColorInfo("CUSTOM (yellow)", UtDialog.GuardColor.CUSTOM(Color.argb(0x80, 0xff, 0xff, 0x00)))
                val values = listOf(DEFAULT, DIM, SEE_THROUGH, SOLID_GRAY, AUTO_DIM, AUTO_SEE_THROUGH, CUSTOM)
            }
        }

        val commandShowDialog = LiteUnitCommand {
            launchTask {
//                createViewModel<FullHeightDialog.FullHeightDialogViewModel>()
//                showDialog(FullHeightDialog())
//
                // Global Options
                UtDialogConfig.dialogMarginOnLandscape = landscapeMarginInfo.value.margin
                UtDialogConfig.dialogMarginOnPortrait = portraitMarginInfo.value.margin

                createViewModel<OptionSampleDialog.OptionSampleDialogViewModel> { setup(this@OptionActivityViewModel) }
                showDialog("sample-dialog") {
                    OptionSampleDialog().apply {
                        isDialog = isDialogMode.value
                        systemBarOptionOnFragmentMode = this@OptionActivityViewModel.systemBarOptionOnFragmentMode.value
                        hideStatusBarOnDialogMode = hideStatusBarOnDialog.value
                    }
                }

                // Restore Global Options
                UtDialogConfig.dialogMarginOnLandscape = DialogMarginInfo.DEFAULT_LANDSCAPE.margin
                UtDialogConfig.dialogMarginOnPortrait = DialogMarginInfo.DEFAULT_PORTRAIT.margin

            }
        }
        val commandThemeColors = LiteUnitCommand {
            launchTask {
                showDialog(ThemeColorDialog())
            }
        }
    }

    private val binder = Binder()
    // theme を切り替えるたびに startActivityするので、Activityのライフサイクルではなくアプリのライフサイクルでビューモデルを構築しておく。
    private val viewModel:OptionActivityViewModel = ViewModelProvider(ApplicationViewModelStoreOwner.viewModelStore, ViewModelProvider.NewInstanceFactory())[OptionActivityViewModel::class.java]
    private lateinit var controls: ActivityOptionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val edgeToEdgeEnabled = viewModel.edgeToEdgeEnabled.value
        if(edgeToEdgeEnabled) {
            enableEdgeToEdge()
        }

        val currentTheme = viewModel.themeInfo.value.themeId
        setTheme(currentTheme)
        controls = ActivityOptionBinding.inflate(layoutInflater)
        setContentView(controls.root)

        if(edgeToEdgeEnabled) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            ViewCompat.setOnApplyWindowInsetsListener(controls.root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, maxOf(systemBars.bottom, ime.bottom))
                insets
            }
        }

        if(!viewModel.showStatusBar.value && isActionBarVisible()) {
            hideActionBar()
        }
        if(!viewModel.showActionBar.value && isStatusBarVisible()) {
            hideStatusBar()
        }

        binder.owner(this)
            .bindCommand(viewModel.commandShowDialog, controls.showDialogButton)
            .bindCommand(viewModel.commandThemeColors, controls.themeButton)
            .activityStatusBarBinding(viewModel.showStatusBar)
            .activityActionBarBinding(viewModel.showActionBar)
            .checkBinding(controls.checkDialogMode, viewModel.isDialogMode)
            .checkBinding(controls.checkEdgeToEdge, viewModel.edgeToEdgeEnabled)
            .checkBinding(controls.checkHideStatusBarOnDialog, viewModel.hideStatusBarOnDialog)
            .checkBinding(controls.checkCancellable, viewModel.cancellable)
            .checkBinding(controls.checkDraggable, viewModel.draggable)
            .checkBinding(controls.checkScrollable, viewModel.scrollable)
            .checkBinding(controls.checkStatusBar, viewModel.showStatusBar)
            .checkBinding(controls.checkActionBar, viewModel.showActionBar)
            .checkBinding(controls.checkProgressRingOnBodyGuardView, viewModel.progressRingOnBodyGuardView)
            .checkBinding(controls.noHeader, viewModel.noHeader)
            .checkBinding(controls.noFooter, viewModel.noFooter)

            .editTextBinding(controls.dialogTitleEdit, viewModel.dialogTitle)
            .spinnerBinding(controls.gravityOptionSpinner, viewModel.gravityOptionInfo, GravityOptionInfo.values)
            .spinnerBinding(controls.systemBarModeOnFragment, viewModel.systemBarOptionOnFragmentMode, UtDialogBase.SystemBarOptionOnFragmentMode.entries.toList(), null)
            .spinnerBinding(controls.widthOptionSpinner, viewModel.widthOptionInfo, WidthOptionInfo.values)
            .spinnerBinding(controls.heightOptionSpinner, viewModel.heightOptionInfo, HeightOptionInfo.values)
            .spinnerBinding(controls.guardColorSpinner, viewModel.guardColorInfo, GuardColorInfo.values)
            .spinnerBinding(controls.bodyGuardColorSpinner, viewModel.bodyGuardColorInfo, GuardColorInfo.values)
            .spinnerBinding(controls.themeSpinner, viewModel.themeInfo, ThemeInfo.values)
            .spinnerBinding(controls.darkLightSpinner, viewModel.darkLightInfo, DarkLightInfo.values)
            .spinnerBinding(controls.portraitMarginSpinner, viewModel.portraitMarginInfo, DialogMarginInfo.portraitValues)
            .spinnerBinding(controls.landscapeMarginSpinner, viewModel.landscapeMarginInfo, DialogMarginInfo.landscapeValues)
            .enableBinding(controls.checkHideStatusBarOnDialog, viewModel.isDialogMode)
            .enableBinding(controls.systemBarModeOnFragment, viewModel.isDialogMode, boolConvert = BoolConvert.Inverse)
            .enableBinding(controls.checkActionBar, viewModel.hasActionBar)

            .observe(viewModel.themeInfo) {
                if(currentTheme!=it.themeId) {
                    restartActivity()
                }
            }
            .observe(viewModel.edgeToEdgeEnabled) {
                if(edgeToEdgeEnabled!=it) {
                    restartActivity()
                }
            }
            .observe(viewModel.darkLightInfo) {
                if(AppCompatDelegate.getDefaultNightMode()!=it.mode) {
                    AppCompatDelegate.setDefaultNightMode(it.mode)
                }
            }
    }

    private fun restartActivity() {
        startActivity(Intent(this, this@OptionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}