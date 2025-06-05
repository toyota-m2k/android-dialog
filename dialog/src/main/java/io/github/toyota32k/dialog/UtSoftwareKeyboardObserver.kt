package io.github.toyota32k.dialog

import android.graphics.Rect

import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import io.github.toyota32k.utils.GenericDisposable
import io.github.toyota32k.utils.IDisposable
import io.github.toyota32k.utils.android.activity
import io.github.toyota32k.utils.lifecycle.LifecycleDisposer

/**
 * ソフトウェアキーボードの開閉を監視する
 */
interface ISoftwareKeyboardObserver : IDisposable {
    fun observe(listener:(keyboardHeight:Int,screenHeight:Int)->Unit):ISoftwareKeyboardObserver
}

/**
 * ISoftwareKeyboardObserverの実装クラスの共通実装
 */
internal abstract class BaseSoftwareKeyboardObserver(owner: LifecycleOwner, protected val decorView:View) : ISoftwareKeyboardObserver {
    protected val disposer = LifecycleDisposer(owner)
    protected lateinit var callback:(keyboardHeight:Int, screenHeight:Int)->Unit
    override fun observe(listener: (keyboardHeight:Int,screenHeight:Int) -> Unit):ISoftwareKeyboardObserver {
        callback = listener
        return this
    }
    override fun dispose() {
        disposer.dispose()
    }
}

/**
 *
 */
internal class GlobalLayoutSoftwareKeyboardObserver(owner:LifecycleOwner, activity: FragmentActivity) : BaseSoftwareKeyboardObserver(owner, activity.window.decorView) {
    var handler:OnGlobalLayoutListener = object:OnGlobalLayoutListener {
        private val rect = Rect()
        private var prevShowing = false
        override fun onGlobalLayout() {
            decorView.getWindowVisibleDisplayFrame(rect)
            val keyboardHeight = decorView.height - rect.bottom
            val showing = keyboardHeight > decorView.height * THRESHOLD
            if( showing != prevShowing) {
                prevShowing = showing
                callback(if(showing) keyboardHeight else 0, decorView.height)
            }
        }
    }
    init {
        // キーボードが表示されたら検出するリスナーを設定
        decorView.viewTreeObserver?.addOnGlobalLayoutListener(handler)
        disposer.register(GenericDisposable {decorView.viewTreeObserver?.removeOnGlobalLayoutListener(handler)} )
    }

    companion object {
        const val THRESHOLD = 0.15f
    }
}

internal class WindowInsetsSoftwareKeyboardObserver(owner: LifecycleOwner, val rootView:View) : BaseSoftwareKeyboardObserver(owner, rootView.activity()?.window?.decorView ?: rootView) {
    init {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            callback(ime.bottom, decorView.height)
            insets
        }
        disposer.register(GenericDisposable { ViewCompat.setOnApplyWindowInsetsListener(rootView, null) } )
    }
}

class UtSoftwareKeyboardObserver(impl:ISoftwareKeyboardObserver) : ISoftwareKeyboardObserver by impl {
    companion object {
        fun byGlobalLayout(owner:LifecycleOwner, activity: FragmentActivity):UtSoftwareKeyboardObserver {
            val impl = GlobalLayoutSoftwareKeyboardObserver(owner, activity)
            return UtSoftwareKeyboardObserver(impl)
        }
        fun byWindowInsets(owner:LifecycleOwner, rootView: View): UtSoftwareKeyboardObserver {
            val impl = WindowInsetsSoftwareKeyboardObserver(owner, rootView)
            return UtSoftwareKeyboardObserver(impl)
        }
    }
}