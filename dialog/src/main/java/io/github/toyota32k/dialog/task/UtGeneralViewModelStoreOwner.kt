package io.github.toyota32k.dialog.task

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * ライフサイクルをマニュアルで制御するViewModelStoreのオーナー
 * （ViewModelProviderの引数に渡すやつ）
 */
@Suppress("unused")
class UtGeneralViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()

    // 不要になったら呼び出す。
    // このストアに属するすべてのViewModelが破棄される
    fun release() {
        viewModelStore.clear()
    }
}