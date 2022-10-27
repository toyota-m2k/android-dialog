package io.github.toyota32k.dialog

import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.view.descendants
import androidx.core.view.isVisible
import io.github.toyota32k.utils.UtLogger
import java.lang.ref.WeakReference

/**
 * UtFocusManager のカスタムアクション用デリゲート型
 * @param   view    イベントが発生したビュー
 * @param   actionId  OnEditorActionListenerが受け取ったAction ID
 * @param   moveFocus   imeOptions == actionNext/Previous なら true それ以外なら false
 */
typealias UtEditorAction = (view:TextView, actionId:Int, moveFocus:Boolean)->Boolean

/**
 * フォーカス管理クラス
 *
 * 作成の動機
 *  EditTextでエンターキーを押したときの動作（変換確定、フォーカス移動、EditorAction）、タブによるフォーカス移動を、人様に説明できる程度にすっきりさせたい。
 *
 * 解決したい課題（＝EditTextとフォーカス移動に関する不可解な動作）
 *  (A) ダイアログを開いて、(HWキーボードの）Tabでフォーカスを移動すると、ダイアログ外（Activity上の）のコントロールにフォーカスが移動できてしまう。
 *          --> モーダルダイアログのつもりなので、ダイアログ外のボタンが押せてしまうと何が起こるか考えるだけでも恐ろしいことです。
 *  (B) 日本語入力時、HWキーボードのEnterで確定すると、（imeOptions == actionDone でも）次のコントロール (nextFocusDown) にフォーカスが移動してしまう。
 *          --> 途中までの入力を確定して、続きを入力、という操作の妨げとなる
 * 課題(A)の対策
 *  フォーカス移動順序、移動範囲を自前で管理する。
 *      - register()  フォーカス対象を登録（登録された順序でタブ移動する）
 *      - autoRegister()   focusable なコントロールを自動列挙（簡単なダイアログ用）
 *  Tabキー押下時（DialogやActivityの OnKeyDown）に、自力でフォーカスを移動する。
 *      - nextOrLoop()  次のコントロールへフォーカスを移動。最後のコントロールにフォーカスがあれば先頭のコントロールにフォーカスを移動。
 *      - prevOrLoop()  前のコントロールへフォーカスを移動。先頭のコントロールにフォーカスがあれば最後のコントロールにフォーカスを移動。
 *
 * 課題(B)の対策
 *  (1) nextFocusDown に自分自身を設定することで課題(A)を回避できた（他の回避方法は見つからなかった）が、確定後、次の Enter でもフォーカスが移動しなくなった。
 *      日本語以外のIMEなら、nextFocusForward を指定しておくと Enterでフォーカス移動 するが、日本語IMEだと、これが効かない。
 *      HWキーボードとSWキーボード、IMEの種類などによっても挙動が定まらない。
 *  (2) nextFocusForwardを無効化して、EditText の OnEditorAction で、自力でフォーカス移動できるようにした。
 *      - setCustomEditorAction()   EditText の OnEditorActionの制御を有効にする。UtEditorAction引数を渡すことで OnEditorActionの挙動をカスタマイズ可能。
 *      ※EditText の OnEditorActionListener は１つしか設定できないので注意。
 *
 * 使い方
 * UtDialog での利用
 *  - UtDialog派生クラスのコンストラクタ、または、preCreateBodyView() で、rootFocusManager を初期化する
 *      ```
 *      enableFocusManagement()                 // rootFocusManagerを有効化する。（デフォルトは無効）
 *          .autoRegister()                     // この例ではフォーカス対象を自動登録。個別に登録する場合は、register()に、R.id.xxxx を渡す。
 *          .setCustomEditorAction()            // Enterキーによる自力フォーカス移動を有効化
 *          .setInitialFocus(R.id.input_1)      // 初期状態でフォーカスをセットするコントロールを指定（任意）
 *      ```
 * 一般的なActivityやFragmentでの利用
 *  - ActivityやFragmentのメンバーとして、UtFocusManagerインスタンスを作って初期化する。
 *  - Activity#onCreateまたは、Fragment#onCreateView で、UtFocusManager#attach() を呼び出して、ルートビュー(IdRes --> View解決に利用）をアタッチする。
 *  - setInitialFocus() を利用する場合は、Activity#onResume、Fragment#onViewCreated から、
 *  - Activityの onKeyDown() （UtMortalActivityの場合は、handleKeyDown()）をオーバーライドして、KeyEvent.KEYCODE_TAB のときに、nextOrLoop() / prevOrLoop() を呼ぶ。
 *
 */
class UtFocusManager : TextView.OnEditorActionListener {

    // region Internals

    private data class Focusable constructor(@IdRes val id: Int, val fm: UtFocusManager?) {
        constructor(@IdRes id: Int) : this(id, null)
        constructor(fm: UtFocusManager) : this(0, fm)

        val isView: Boolean = fm == null
        fun hasView(@IdRes id: Int): Boolean {
            return if (isView) this.id == id else fm?.hasView(id) == true
        }
    }

    private lateinit var rootViewRef: WeakReference<View>
    private val rootView: View get() = rootViewRef.get()!!
    private val focusables = mutableListOf<Focusable>()
    private var initialFocus: Int = 0
    private var autoRegister = false
    private var customForwardAction = false

    enum class KeyAction {
        DONE,
        NEXT,
    }
    private var externalEditorAction: UtEditorAction? = null

    private fun View.patchNextFocus(): View {
        if (this is EditText) {
            nextFocusDownId = id
            if (customForwardAction) {
                // ソフトウェアキーボードの actionNext によるフォーカス移動も禁止する
                nextFocusForwardId = id
                setOnEditorActionListener(this@UtFocusManager)
            }
        }
        return this
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        val action = v?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: return false
        UtLogger.debug("viewAction:$action calledAction:${actionId.and(EditorInfo.IME_MASK_ACTION)} - $event")
        if(event?.action == KeyEvent.ACTION_UP) return false

        when (action) {
            EditorInfo.IME_ACTION_NONE -> return true
            EditorInfo.IME_ACTION_NEXT -> {
                if (externalEditorAction?.invoke(v, actionId, true) != true) {
                    nextOrLoop(v.id)
                }
            }
            EditorInfo.IME_ACTION_PREVIOUS -> {
                if (externalEditorAction?.invoke(v, actionId, true) != true) {
                    prevOrLoop(v.id)
                }
            }
            else -> {
                externalEditorAction?.invoke(v, actionId, false)
            }
        }
        return true
    }

    // endregion

    // Interaction with the container view

    /**
     * IdRes --> View の解決に使用するルートビューをアタッチする。(Activity#onCreate/Fragment#onCreateView などから呼び出す。
     * @param rootView  ルートビュー
     * @param autoRegisterBaseView  autoRegister==true のとき、IdResからビューを列挙する起点となるビュー。nullなら rootViewを起点とする。
     */
    fun attach(rootView: View, autoRegisterBaseView:ViewGroup?=null) {
        this.rootViewRef = WeakReference(rootView)
        if(autoRegister) {
            // 自動登録の解決
            val baseView = autoRegisterBaseView ?: rootView as? ViewGroup
            if(baseView != null) {
                focusables.addAll(baseView.descendants.mapNotNull {
                    if (it.isFocusable && it !is ViewGroup) {
                        Focusable(it.id)
                    } else null
                })
            } else {
                UtLogger.error("cannot resolve views automatically.")
            }
        }
        // EditText のnextFocusDown/nextFocusForward の無効化
        for (f in focusables) {
            if (f.isView) {
                rootView.findViewById<View>(f.id).patchNextFocus()
            }
        }
    }

    // endregion

    // region Setup operation modes.

    fun setInitialFocus(id: Int): UtFocusManager {
        initialFocus = id
        return this
    }

    fun setCustomEditorAction(fn:UtEditorAction?=null): UtFocusManager {
        customForwardAction = true
        externalEditorAction = fn
        return this
    }

    fun applyInitialFocus(): Boolean {
        if (initialFocus != 0) {
            val view = rootViewRef.get()?.findViewById<View>(initialFocus)
            if (view != null) {
                view.requestFocus()
                initialFocus = 0
                return true
            }
        }
        focusables.mapNotNull { it.fm }.forEach {
            if (it.applyInitialFocus()) {
                return true
            }
        }
        return false
    }

    fun register(@IdRes vararg ids: Int): UtFocusManager {
        focusables.addAll(ids.map { Focusable(it) })
        return this
    }

    fun register(fm: UtFocusManager): UtFocusManager {
        focusables.add(Focusable(fm))
        return this
    }

    fun autoRegister(): UtFocusManager {
        autoRegister = true
        return this
    }

    fun clear(): UtFocusManager {
        focusables.clear()
        return this
    }

    // endregion

    // Focus management

    fun next(id: Int): Boolean {
        val current = focusables.find { it.hasView(id) } ?: return false
        if (!current.isView && current.fm?.next(id) == true) {
            return true
        }
        return firstAfter(focusables.indexOf(current) + 1)
    }

    fun prev(id: Int): Boolean {
        val current = focusables.find { it.hasView(id) } ?: return false
        if (!current.isView && current.fm?.prev(id) == true) {
            return true
        }

        return lastBefore(focusables.indexOf(current) - 1)
    }

    private fun lastBefore(index: Int): Boolean {
        for (i in index downTo 0) {
            val f = focusables[i]
            if (f.isView) {
                val view = rootView.findViewById<View>(f.id)
                if (view.isEnabled && view.isFocusable && view.isVisible) {
                    view.requestFocus()
                    return true
                }
            } else {
                if (f.fm?.tail() == true) {
                    return true
                }
            }
        }
        return false
    }

    private fun firstAfter(index: Int): Boolean {
        for (i in index until focusables.size) {
            val f = focusables[i]
            if (f.isView) {
                val view = rootView.findViewById<View>(f.id)
                if (view.isEnabled && view.isFocusable && view.isVisible) {
                    view.requestFocus()
                    return true
                }
            } else {
                if (f.fm?.head() == true) {
                    return true
                }
            }
        }
        return false
    }

    fun head(): Boolean {
        return firstAfter(0)
    }

    fun tail(): Boolean {
        return lastBefore(focusables.size - 1)
    }

    fun hasView(id: Int): Boolean {
        return focusables.find { it.hasView(id) } != null
    }

    fun nextOrLoop(@IdRes id: Int) {
        if (id == 0 || !next(id)) {
            head()
        }
    }

    fun prevOrLoop(@IdRes id: Int) {
        if (id == 0 || !prev(id)) {
            tail()
        }
    }

    // endregion

}