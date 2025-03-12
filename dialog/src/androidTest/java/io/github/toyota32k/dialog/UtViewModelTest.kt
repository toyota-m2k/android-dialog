package io.github.toyota32k.dialog

import android.os.Bundle
import android.view.View
import io.github.toyota32k.dialog.task.UtImmortalTask
import io.github.toyota32k.dialog.task.UtDialogViewModel
import io.github.toyota32k.dialog.task.createViewModel
import io.github.toyota32k.dialog.task.getViewModel
import io.github.toyota32k.dialog.task.withViewModel

class UtViewModelTest {
    class MyViewModel : UtDialogViewModel() {
        // ...
        fun initialize() {
            // ...
        }
        fun doSomething() {
            // ...
        }
    }
    class MyDialog : UtDialogEx() {
        val viewModel = getViewModel<MyViewModel>()
        override fun createBodyView(savedInstanceState: Bundle?, inflater: IViewInflater): View {
            // ...
        }
    }
    fun test() {
        UtImmortalTask.launchTask {
            createViewModel<MyViewModel> {
                // 呼び出し元でViewModelへのパラメーター設定などを行うならこのタイミング
                initialize()
            }
            val dlg = showDialog<MyDialog>("test-dialog")
            if (dlg.status.ok) {
                // ダイアログがDoneボタンで閉じた
            }
        }

        UtImmortalTask.launchTask {
            withViewModel<MyViewModel> { viewModel ->
                // 呼び出し元でViewModelへのパラメーター設定などを行うならこのタイミング
                viewModel.initialize()
                val dlg = showDialog<MyDialog>("test-dialog")
                if (dlg.status.ok) {
                    // ダイアログがDoneボタンで閉じた
                    // viewModelからパラメータを取り出して次の処理を行うならこのタイミング
                    viewModel.doSomething()
                }
            }
        }
    }
}