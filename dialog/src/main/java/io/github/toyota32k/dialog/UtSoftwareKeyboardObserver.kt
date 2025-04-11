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
import io.github.toyota32k.utils.LifecycleDisposer

/**
 * ソフトウェアキーボードの開閉を監視する
 */
interface ISoftwareKeyboardObserver : IDisposable {
    fun observe(listener:(keyboardHeight:Int)->Unit):ISoftwareKeyboardObserver
}

/**
 * ISoftwareKeyboardObserverの実装クラスの共通実装
 */
internal abstract class BaseSoftwareKeyboardObserver(owner: LifecycleOwner) : ISoftwareKeyboardObserver {
    protected val disposer = LifecycleDisposer(owner)
    protected lateinit var callback:(keyboardHeight:Int)->Unit
    override fun observe(listener: (Int) -> Unit):ISoftwareKeyboardObserver {
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
internal class GlobalLayoutSoftwareKeyboardObserver(owner:LifecycleOwner, activity: FragmentActivity) : BaseSoftwareKeyboardObserver(owner) {
    private val decorView:View = activity.window?.decorView ?: throw Exception("decorView not found")
    var handler:OnGlobalLayoutListener = object:OnGlobalLayoutListener {
        private val rect = Rect()
        private var prevShowing = false
        override fun onGlobalLayout() {
            decorView.getWindowVisibleDisplayFrame(rect)
            val keyboardHeight = decorView.height - rect.bottom
            val showing = keyboardHeight > decorView.height * THRESHOLD
            if( showing != prevShowing) {
                prevShowing = showing
                callback(keyboardHeight)
            }
        }
    }
    init {
        // キーボードが表示されたら検出するリスナーを設定
        decorView.viewTreeObserver?.addOnGlobalLayoutListener(handler)
        disposer.register(GenericDisposable {decorView.viewTreeObserver?.removeOnGlobalLayoutListener(handler)} )
    }

    companion object {
        const val THRESHOLD = 0.1f
    }
}

internal class WindowInsetsSoftwareKeyboardObserver(owner: LifecycleOwner, val rootView:View) : BaseSoftwareKeyboardObserver(owner) {
    init {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            callback(ime.bottom)
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