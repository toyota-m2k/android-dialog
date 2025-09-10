package io.github.toyota32k.dialog

import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.view.ancestors
import androidx.core.view.descendants
import androidx.core.view.isVisible
import io.github.toyota32k.logger.UtLog
import java.lang.ref.WeakReference

/**
 * UtFocusManager のカスタムアクション用デリゲート型
 * params
 *  - view    イベントが発生したビュー
 *  - actionId  OnEditorActionListenerが受け取ったAction ID
 *  - moveFocus   imeOptions == actionNext/Previous なら true それ以外なら false
 */
typealias UtEditorAction = (view:TextView, actionId:Int, moveFocus:Boolean)->Boolean

/**
 * フォーカス管理クラス
 *
 *  see https://github.com/toyota-m2k/android-dialog/blob/main/doc/focus_manager.md
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class UtFocusManager : TextView.OnEditorActionListener {
    enum class UseKey(val ud:Boolean, val lr:Boolean) {
        None(false,false),           // Tabキーのみ
        UpDown(true, false),
        LeftRight(false, true),
        All(true,true),            // Tabキーと上下左右キー
    }

    // region Internals

    private data class Focusable (@param:IdRes val id: Int, val fm: UtFocusManager?) {
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
    private var useKeys: UseKey = defaultUseKey
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


    /**
     * TextView.OnEditorActionListener
     */
    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        val action = v?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: return false
        logger.debug("viewAction:$action calledAction:${actionId.and(EditorInfo.IME_MASK_ACTION)} - $event")
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
     * UtDialog の場合は、enableFocusManagement() により、自動的に呼びだされる。
     * @param rootView  ルートビュー
     */
    fun attach(rootView: View) {
        this.rootViewRef = WeakReference(rootView)
        if(autoRegister) {
            // 自動登録の解決
            focusables.clear()  // 複数回呼ばれると重複が発生するので事前にクリアしておく
            val baseView = rootView as? ViewGroup
            if(baseView != null) {
                focusables.addAll(baseView.descendants.mapNotNull {
                    if (it.isFocusable && it !is ViewGroup) {
                        if (it.id != View.NO_ID) {
                            Focusable(it.id)
                        } else {
                            logger.warn("View ${it.javaClass.simpleName} is focusable but has no id, cannot be managed.")
                            null
                        }
                    } else null
                })
            } else {
                logger.error("cannot resolve views automatically.")
            }
        }
        // EditText のnextFocusDown/nextFocusForward の無効化
        for (f in focusables) {
            if (f.isView) {
                rootView.findViewById<View?>(f.id)?.patchNextFocus()
            }
        }
    }

    // endregion

    // region Setup operation modes.

    /**
     * ダイアログを表示したときの初期フォーカスを指定
     */
    fun setInitialFocus(id: Int): UtFocusManager {
        initialFocus = id
        return this
    }

    /**
     * ビューの自動登録を有効にする。
     * rootView を attach() するときに、rootView に含まれるフォーカス受け取り可能なビューを、一括登録します。
     * 登録順序が期待通りにならない場合は、register系のメソッドを使って明示的に順序を指定してください。
     */
    fun autoRegister(): UtFocusManager {
        autoRegister = true
        return this
    }

    /**
     * TextView の TextView.OnEditorActionListener イベントは、UtFocusManager がフォーカス移動のために消費してしまうが、
     * このイベントを利用者側でフックする仕組みを提供。
     */
    fun setCustomEditorAction(fn:UtEditorAction?=null): UtFocusManager {
        customForwardAction = true
        externalEditorAction = fn
        return this
    }

    fun setUseKeys(useKey:UseKey): UtFocusManager {
        useKeys = useKey
        return this
    }

    /**
     * focusableなビューを渡された順番で、リストの末尾に登録する。
     */
    fun register(@IdRes vararg ids: Int): UtFocusManager {
        focusables.addAll(ids.map { Focusable(it) })
        return this
    }

    /**
     * フォーカスアイテムリストの末尾に子マネージャを追加する
     */
    fun appendChild(fm: UtFocusManager): UtFocusManager {
        focusables.add(Focusable(fm))
        return this
    }

    /**
     * 指定されたアイテムの直後に子マネージャを挿入する。
     * @param prev  挿入位置の１つ手前の子マネージャ。nullなら先頭に挿入。
     */
    fun insertChildAfter(prev:UtFocusManager?, fm:UtFocusManager) {
        val index = if(prev!=null) { focusables.indexOfFirst { it.fm === prev }+1 } else 0
        focusables.add(index,Focusable(fm))
    }
    /**
     * 指定されたアイテムの直後に子マネージャを挿入する。
     * @param prev  挿入位置の１つ手前の子マネージャ。0なら先頭に挿入。
     */
    fun insertChildAfter(@IdRes prev:Int, fm:UtFocusManager) {
        val index = if(prev!=0) { focusables.indexOfFirst { it.id == prev }+1 } else 0
        focusables.add(index,Focusable(fm))
    }

    /**
     * 子マネージャーを削除する。
     */
    fun removeChild(fm: UtFocusManager): UtFocusManager {
        focusables.removeAll { it.fm === fm }
        return this
    }

    /**
     * 全クリア
     */
    fun clear(): UtFocusManager {
        focusables.clear()
        return this
    }

    // endregion

    // Focus management

    /**
     * 初期フォーカスを適用
     * ビューが構築された後に呼び出す。UtDialog の場合は、setInitialFocus() しておけば自動的に呼び出される。
     *
     */
    fun applyInitialFocus(): Boolean {
        if (initialFocus != 0) {
            val view = rootViewRef.get()?.findViewById<View?>(initialFocus)
            if (view != null) {
                view.forceRequestFocus()
                return true
            }
        }
        return focusables.mapNotNull { it.fm }.find {
            it.applyInitialFocus()
        } != null
    }

//    private fun hideSoftwareKeyboard() {
//        try {
//            (rootView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(rootView.windowToken,0)
//        } catch (e:Throwable) {
//            logger.error(e)
//        }
//    }

    // 祖先（親、親の親、...）に１つでも非表示のビューがあれば false を返す
    private fun View.isAncestorsVisible():Boolean {
        return this.ancestors.find { (it as? ViewGroup)?.isVisible==false } == null
    }

    private fun View.forceRequestFocus() {
        if(!requestFocus() && !isFocusableInTouchMode) {
            isFocusableInTouchMode = true
            if(!requestFocus()) {
                logger.warn("cannot focus to $this")
            }
            post { isFocusableInTouchMode  = false }
        }
    }

    private fun setFocusTo(view:View?):Boolean {
        return if (view!=null && view.isEnabled && view.isFocusable && view.isVisible && view.isAncestorsVisible()) {
            logger.debug("$view")
            view.forceRequestFocus()
            true
        } else false
    }

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
                val view:View? = rootView.findViewById(f.id)
                if(setFocusTo(view)) {
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
                val view:View? = rootView.findViewById(f.id)
                if(setFocusTo(view)) {
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

    fun handleTabEvent(event:KeyEvent, currentFocus:()->View?):Boolean {
        if(event.action != KeyEvent.ACTION_DOWN) return false // DOWNのみ処理
        val keyCode = event.keyCode
        return when(keyCode) {
            KeyEvent.KEYCODE_TAB-> {
                if (event.isShiftPressed) {
                    prevOrLoop(currentFocus()?.id ?: 0)
                } else {
                    nextOrLoop(currentFocus()?.id ?: 0)
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (useKeys.ud) {
                    prevOrLoop(currentFocus()?.id ?: 0)
                    true
                } else false
            }
            KeyEvent.KEYCODE_DPAD_LEFT-> {
                if (useKeys.lr) {
                    prevOrLoop(currentFocus()?.id ?: 0)
                    true
                } else false
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (useKeys.ud) {
                    nextOrLoop(currentFocus()?.id ?: 0)
                    true
                } else false
            }
            KeyEvent.KEYCODE_DPAD_RIGHT-> {
                if (useKeys.lr) {
                    nextOrLoop(currentFocus()?.id ?: 0)
                    true
                } else false
            }
            else->false
        }
    }

    // endregion

    companion object {
        val logger: UtLog get() = UtDialogBase.logger
        var defaultUseKey: UseKey = UseKey.None
    }
}