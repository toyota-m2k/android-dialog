package io.github.toyota32k.dialog

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference

data class UtDialogOwner(val lifecycleOwner: LifecycleOwner) {
    constructor(owner:UtDialogOwner):this(owner.lifecycleOwner)
    init {
        if(!(lifecycleOwner is FragmentActivity || lifecycleOwner is Fragment)) {
            throw IllegalArgumentException("DialogOwner must be FragmentActivity or Fragment")
        }
    }
}

fun FragmentActivity.toDialogOwner() = UtDialogOwner(this)
fun Fragment.toDialogOwner() = UtDialogOwner(this)

fun IUtDialog.show(owner:UtDialogOwner, tag:String) {
    when(owner.lifecycleOwner) {
        is FragmentActivity -> show(owner.lifecycleOwner, tag)
        is Fragment         -> show(owner.lifecycleOwner, tag)
    }
}
fun UtDialogHostManager.ReceptorImpl.showDialog(owner:UtDialogOwner, creator:(UtDialogHostManager.ReceptorImpl)->IUtDialog) {
    when(owner.lifecycleOwner) {
        is FragmentActivity -> showDialog(owner.lifecycleOwner, creator)
        is Fragment         -> showDialog(owner.lifecycleOwner, creator)
    }
}
fun UtDialogHostManager.ReceptorImpl.showDialog(owner:UtDialogOwner, clientData:Any?, creator:(UtDialogHostManager.ReceptorImpl)->IUtDialog) {
    when(owner.lifecycleOwner) {
        is FragmentActivity -> showDialog(owner.lifecycleOwner, clientData, creator)
        is Fragment         -> showDialog(owner.lifecycleOwner, clientData, creator)
    }
}


open class UtDialogWeakOwner(owner:LifecycleOwner) : LifecycleEventObserver {
    constructor(owner:UtDialogOwner):this(owner.lifecycleOwner)
    var weakOwner :WeakReference<LifecycleOwner>?
    init {
        if(!(owner is FragmentActivity || owner is Fragment)) {
            throw IllegalArgumentException("DialogOwner must be FragmentActivity or Fragment")
        }
        weakOwner = WeakReference(owner)
        owner.lifecycle.addObserver(this)
    }

    val lifecycleOwner:LifecycleOwner?
        get() = weakOwner?.get()

    val asDialogOwner:UtDialogOwner?
        get() = lifecycleOwner?.let { UtDialogOwner(it) }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
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