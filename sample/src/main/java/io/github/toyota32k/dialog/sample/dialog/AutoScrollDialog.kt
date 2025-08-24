package io.github.toyota32k.dialog.sample.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.fragment.app.FragmentActivity
import io.github.toyota32k.binder.clickBinding
import io.github.toyota32k.binder.command.LiteCommand
import io.github.toyota32k.binder.command.LiteUnitCommand
import io.github.toyota32k.binder.command.bindCommand
import io.github.toyota32k.dialog.UtDialog
import io.github.toyota32k.dialog.UtDialogEx
import io.github.toyota32k.dialog.UtDialogHelper
import io.github.toyota32k.dialog.sample.databinding.DialogAutoScrollBinding
import io.github.toyota32k.dialog.task.UtDialogViewModel
import io.github.toyota32k.dialog.task.getViewModel
import kotlinx.coroutines.delay

class AutoScrollDialog : UtDialogEx() {
    class AutoScrollDialogViewModel : UtDialogViewModel() {
        var count = 3

        val refugeCommand = LiteCommand<FragmentActivity> {
            launchSubTask {
                delay(1000)
//                val cur = UtDialogHelper.currentDialog(it)
//                if (cur!=null) {
//                    val fragmentManager = cur.requireActivity().supportFragmentManager
//                    val fragments = fragmentManager.fragments
//                    assert(fragments.contains(cur)) { "Current dialog is not in the fragment manager." }
//                    cur.requireActivity().supportFragmentManager.beginTransaction().hide(cur).commitNow()
//                    assert(cur.dialogView.parent == cur.rootView) { "Dialog view is not attached to the root view." }
////                    cur.rootView.visibility = View.GONE
//
//                }
                val rf = UtDialogHelper.refugeAll(it)
                if (rf!=null) {
                    delay(2000)
                    rf.restore(it)
//                    delay(1000)
//                    val cur = UtDialogHelper.currentDialog(it)
//                    if (cur is AutoScrollDialog) {
//                        cur.visible = false
//                        delay(2000)
//                        cur.visible = true
//                    }
                }
            }
        }
    }
    private lateinit var controls: DialogAutoScrollBinding
    private val viewModel by lazy { getViewModel<AutoScrollDialogViewModel>() }

    override fun preCreateBodyView() {
        title="Auto Scroll Dialog"
        heightOption=HeightOption.AUTO_SCROLL
        gravityOption = GravityOption.CENTER
        leftButtonType = ButtonType.CANCEL
        rightButtonType = ButtonType.DONE
        cancellable = true
        guardColor = GuardColor.SEE_THROUGH
        enableFocusManagement().autoRegister()
    }

    private fun addItemToView(index:Int) {
        val view = TextView(requireContext()).apply {
            text = "Item - $index"
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        controls.root.addView(view)
    }

    @SuppressLint("SetTextI18n")
    override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
        binder.owner(this)

        controls = DialogAutoScrollBinding.inflate(inflater.layoutInflater)
        for(i in 0 until viewModel.count) {
            addItemToView(i+1)
        }

        return controls.apply {
            binder
                .clickBinding(addItemButton) {
                    viewModel.count++
                    addItemToView(viewModel.count)
                }
                .clickBinding(delItemButton) {
                    val view = controls.root.children.lastOrNull { it is TextView } ?: return@clickBinding
                    controls.root.removeView(view)
                    viewModel.count--
                }
                .bindCommand(viewModel.refugeCommand, Pair(controls.refugeButton,requireActivity()))
        }.root
    }
}