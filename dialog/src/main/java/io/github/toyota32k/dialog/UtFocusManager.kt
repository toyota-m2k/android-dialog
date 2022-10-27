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
     */
    fun attach(rootView: View) {
        this.rootViewRef = WeakReference(rootView)
        if(autoRegister) {
            // 自動登録の解決
            val baseView = rootView as? ViewGroup
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
        return focusables.mapNotNull { it.fm }.find {
            it.applyInitialFocus()
        } != null
    }

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

    fun removeChild(fm: UtFocusManager): UtFocusManager {
        focusables.removeAll { it.fm === fm }
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

    fun handleTabEvent(keyCode:Int, event:KeyEvent?, currentFocus:()->View?):Boolean {
        return if(keyCode==KeyEvent.KEYCODE_TAB && event!=null && event.action == KeyEvent.ACTION_DOWN) {
            if (event.isShiftPressed) {
                prevOrLoop(currentFocus()?.id ?: 0)
            } else {
                nextOrLoop(currentFocus()?.id ?: 0)
            }
            true
        } else false
    }

    // endregion

}