@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package io.github.toyota32k.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import io.github.toyota32k.binder.*
import io.github.toyota32k.binder.command.LiteUnitCommand
import io.github.toyota32k.binder.command.bindCommand
import kotlinx.coroutines.flow.Flow

/**
 * binder(v2)を使うための小さな仕掛けを追加したダイアログ基底クラス
 */
abstract class UtDialogEx : UtDialog() {
    val binder = Binder()

    override fun onCreateView(orgInflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binder.owner(this)
        return super.onCreateView(orgInflater, container, savedInstanceState)
    }

    // ダイアログのプロパティとモデルをバインドするためのヘルパー拡張関数
    // UtDialog#onCreateView で使用することを想定。

    /**
     * ダイアログタイトルとモデルをバインドする
     */
    fun Binder.dialogTitle(data: LiveData<String>):Binder
            = textBinding(titleView, data)
    fun Binder.dialogTitle(data: Flow<String>):Binder
            = dialogTitle(data.asLiveData())

    /**
     * 左ボタンの表示/非表示をモデルにバインドする
     */
    fun Binder.dialogLeftButtonVisibility(data: LiveData<Boolean>, boolConvert: BoolConvert=BoolConvert.Straight):Binder
            = visibilityBinding(leftButton, data, boolConvert,VisibilityBinding.HiddenMode.HideByGone)
    fun Binder.dialogLeftButtonVisibility(data: Flow<Boolean>, boolConvert: BoolConvert=BoolConvert.Straight):Binder
            = dialogLeftButtonVisibility(data.asLiveData(),boolConvert)

    /**
     * 右ボタンの表示/非表示をモデルにバインドする
     */
    fun Binder.rightButtonVisibility(data: LiveData<Boolean>, boolConvert: BoolConvert=BoolConvert.Straight):Binder
            = visibilityBinding(rightButton, data, boolConvert,VisibilityBinding.HiddenMode.HideByGone)
    fun Binder.rightButtonVisibility(data: Flow<Boolean>, boolConvert: BoolConvert=BoolConvert.Straight):Binder
            = rightButtonVisibility(data.asLiveData(), boolConvert)

    /**
     * 左ボタンの有効/無効とモデルをバインドする。
     */
    fun Binder.dialogLeftButtonEnable(data: LiveData<Boolean>, boolConvert: BoolConvert=BoolConvert.Straight):Binder
            = enableBinding(leftButton, data, boolConvert)
    fun Binder.dialogLeftButtonEnable(data: Flow<Boolean>, boolConvert: BoolConvert=BoolConvert.Straight):Binder
            = dialogLeftButtonEnable(data.asLiveData(), boolConvert)

    /**
     * 右ボタンの有効/無効とモデルをバインドする。
     */
    fun Binder.dialogRightButtonEnable(data: LiveData<Boolean>, boolConvert: BoolConvert=BoolConvert.Straight):Binder
            = enableBinding(rightButton, data, boolConvert)
    fun Binder.dialogRightButtonEnable(data: Flow<Boolean>, boolConvert: BoolConvert=BoolConvert.Straight):Binder
            = dialogRightButtonEnable(data.asLiveData(),boolConvert)

    /**
     * ガードビューの表示/非表示をモデルとバインドする。
     * @param showProgressRing  ガードビューにプログレスリングを表示するかどうか
     */
    fun Binder.dialogGuardViewVisibility(data:LiveData<Boolean>, showProgressRing:Boolean=false, boolConvert: BoolConvert=BoolConvert.Straight):Binder {
        centerProgressRing.visibility = if(showProgressRing) View.VISIBLE else View.GONE
        return visibilityBinding(bodyGuardView, data, boolConvert, VisibilityBinding.HiddenMode.HideByGone)
    }
    fun Binder.dialogGuardViewVisibility(data:Flow<Boolean>, showProgressRing:Boolean=false, boolConvert: BoolConvert=BoolConvert.Straight):Binder
            = dialogGuardViewVisibility(data.asLiveData(), showProgressRing,boolConvert)

    /**
     * タイトルバー上のプログレスリングの表示/非表示をモデルにバインドする。
     */
    fun Binder.dialogProgressRingOnTitleTitleBarVisibility(data:LiveData<Boolean>, boolConvert: BoolConvert=BoolConvert.Straight):Binder
            = visibilityBinding(progressRingOnTitleBar, data, boolConvert, VisibilityBinding.HiddenMode.HideByInvisible)
    fun Binder.dialogProgressRingOnTitleTitleBarVisibility(data:Flow<Boolean>, boolConvert: BoolConvert=BoolConvert.Straight):Binder
            = dialogProgressRingOnTitleTitleBarVisibility(data.asLiveData(), boolConvert)


    /**
     * cancellableプロパティ（ダイアログ外タップによるキャンセルを許可するか）をモデルとバインドする。
     */
    fun Binder.dialogCancellable(data:LiveData<Boolean>, boolConvert: BoolConvert=BoolConvert.Straight):Binder
            = genericBoolBinding(rootView, data, boolConvert) {_,b-> cancellable = b }
    fun Binder.dialogCancellable(data:Flow<Boolean>, boolConvert: BoolConvert=BoolConvert.Straight):Binder
            = dialogCancellable(data.asLiveData(), boolConvert)

}