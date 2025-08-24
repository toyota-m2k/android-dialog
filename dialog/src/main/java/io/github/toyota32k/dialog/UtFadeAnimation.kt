package io.github.toyota32k.dialog

import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * アニメーション完了監視付きフェードイン/フェードアウト
 * フェードイン/フェードアウトアニメションを伴ってVISIBLE<->INVISIBLEの切り替えを行う。
 * @param show  true:フェードイン / false:フェードアウト
 * @param duration in milliseconds
 */
class UtFadeAnimation(val show:Boolean, duration:Long) : Animation.AnimationListener{
    private var view:View? = null
    private var completed:(()->Unit)? = null

    override fun onAnimationStart(animation: Animation?) {
        val view = this.view ?: return
        // fade-in/out に関わらず、VISIBLE + alpha=1 からスタートする。
        // fade-in の場合は、alpha=0 から始まるべきかと思ったが、alpha=0 からスタートするとアニメーションしない。
        view.alpha = 1f
        view.visibility = View.VISIBLE
    }

    override fun onAnimationEnd(animation: Animation?) {
        val view = this.view ?: return
        this.view = null
        if(show) {
            view.alpha = 1f
        } else {
            view.visibility = View.INVISIBLE
            view.alpha = 0f
        }
        completed?.invoke()
        completed = null
    }

    override fun onAnimationRepeat(animation: Animation?) {}

    private val animation = (if(show) AlphaAnimation(0f,1f) else AlphaAnimation(1f,0f)).also { anim->
        anim.fillAfter = true
        anim.duration = duration
        anim.setAnimationListener(this@UtFadeAnimation)
    }

    /**
     * fade in/out を開始
     * @param view  VISIBLE/INVISIBLEを切り替えるビュー
     * @param completed 完了ハンドラ
     */
    @MainThread
    fun start(view:View, completed:(()->Unit)? = null) {
        this.completed = completed
        this.view = view
        view.startAnimation(animation)

        // ownerのActivityが死んだ後に呼ばれる場合など、startAnimation()を呼んでもアニメーションが開始しないことがある
        // ダイアログ自体はActivityとともに消えてしまうので、見かけ上は問題ないが、タスクが待ち合わせている場合に、completed()が呼ばれないと、終了できなくなってしまうので、
        // durationの倍の時間だけ待っても、アニメーションが終了しない場合はアニメーションをキャンセルして、アニメーションが終わったことにする。
        CoroutineScope(Dispatchers.Main).launch {
            delay(animation.duration*2)
            if(this@UtFadeAnimation.view == view) {
                UtDialogBase.logger.warn("animation was completed by force.")
                view.clearAnimation()
                onAnimationEnd(null)
            }
        }
    }

}