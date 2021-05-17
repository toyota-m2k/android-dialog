package io.github.toyota32k.dialog

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.lang.IllegalStateException
import java.lang.ref.WeakReference

/**
 * �_�C�A���O�̐e�ƂȂ�AFragment��Activity ��API�I�ɋ�ʂ��Ȃ��ň�����悤�ɂ��邽�߂̃N���X
 * ����́AFragment/Activity�����Q�Ƃŕێ�����̂ŁA�����̈����n���p�Ƃ��Ă̂ݎg�p���A
 * �����o�[�Ƃ��ĕێ�����ꍇ�� UtDialogWeakOwner ���g�p���邱�ƁB
 */
data class UtDialogOwner(val lifecycleOwner: LifecycleOwner) {
    constructor(owner:UtDialogOwner):this(owner.lifecycleOwner)
    init {
        if(!(lifecycleOwner is FragmentActivity || lifecycleOwner is Fragment)) {
            throw IllegalArgumentException("DialogOwner must be FragmentActivity or Fragment")
        }
    }
    fun asContext() : Context {
        return when(lifecycleOwner) {
            is FragmentActivity-> lifecycleOwner
            is Fragment->lifecycleOwner.requireContext()
            is Context->lifecycleOwner
            else -> {
                throw IllegalStateException("invalid lifecycleOwner")
            }
        }

    }
    fun asActivity():FragmentActivity? {
        return when(lifecycleOwner) {
            is FragmentActivity->lifecycleOwner
            is Fragment->lifecycleOwner.requireActivity()
            else -> null
        }
    }
}

/**
 * Activity --> UtDialogOwner
 */
fun FragmentActivity.toDialogOwner() = UtDialogOwner(this)

/**
 * Fragment --> UtDialogOwner
 */
fun Fragment.toDialogOwner() = UtDialogOwner(this)

/**
 * UtDialogOwner��e�ɂ��ă_�C�A���O���J�����߂̊g���֐�
 */
fun IUtDialog.show(owner:UtDialogOwner, tag:String) {
    when(owner.lifecycleOwner) {
        is FragmentActivity -> show(owner.lifecycleOwner, tag)
        is Fragment         -> show(owner.lifecycleOwner, tag)
    }
}

/**
 * UtDialogOwner��e�ɂ���UtDialogHostManager.ReceptorImpl.showDialog���ĂԂ��߂̊g���֐�
 */
//fun UtDialogHostManager.ReceptorImpl.showDialog(owner:UtDialogOwner, creator:(UtDialogHostManager.ReceptorImpl)->IUtDialog) {
//    when(owner.lifecycleOwner) {
//        is FragmentActivity -> showDialog(owner.lifecycleOwner, creator)
//        is Fragment         -> showDialog(owner.lifecycleOwner, creator)
//    }
//}
fun UtDialogHostManager.NamedReceptor.showDialog(owner:UtDialogOwner, clientData:Any?=null, creator:(UtDialogHostManager.NamedReceptor)->IUtDialog) {
    when(owner.lifecycleOwner) {
        is FragmentActivity -> showDialog(owner.lifecycleOwner, clientData, creator)
        is Fragment         -> showDialog(owner.lifecycleOwner, clientData, creator)
    }
}

open class UtDialogWeakOwner(owner: LifecycleOwner) : LifecycleEventObserver {
    constructor(owner:UtDialogOwner) : this(owner.lifecycleOwner)
    private var weakOwner :WeakReference<LifecycleOwner>? = null

    var lifecycleOwner:LifecycleOwner?
        get() = weakOwner?.get()
        private set(owner) {
            if(!(owner is FragmentActivity || owner is Fragment)) {
                throw IllegalArgumentException("DialogOwner must be FragmentActivity or Fragment")
            }
            weakOwner = WeakReference(owner)
            owner.lifecycle.addObserver(this)
        }

    init {
        this.lifecycleOwner = owner
    }


    val asDialogOwner:UtDialogOwner?
        get() = lifecycleOwner?.let { UtDialogOwner(it) }

    final override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (!source.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            dispose()
        }
    }

    open fun dispose() {
        lifecycleOwner?.lifecycle?.removeObserver(this)
        weakOwner = null
    }

    val hasOwner:Boolean
        get() = lifecycleOwner!=null
}