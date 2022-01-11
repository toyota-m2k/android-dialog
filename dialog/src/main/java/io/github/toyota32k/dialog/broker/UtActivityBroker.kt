package io.github.toyota32k.dialog.broker

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import java.lang.IllegalStateException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * UtActivityConnectorは必要以上に複雑化してしまったので、
 * ImmortalTaskから呼び出すことに限定して、超絶簡略化したバージョンを用意してみる。
 */

abstract class UtActivityBroker<I,O>
    : ActivityResultCallback<O>, IUtActivityBroker {
    abstract val contract: ActivityResultContract<I,O>
    private var continuation:Continuation<O>? = null
    private lateinit var launcher: ActivityResultLauncher<I>

    override fun register(owner: Fragment) {
        launcher = owner.registerForActivityResult(contract, this)
    }
    override fun register(owner: FragmentActivity) {
        launcher = owner.registerForActivityResult(contract, this)
    }

    override fun onActivityResult(result: O) {
        continuation?.let {
            continuation = null
            it.resume(result)
        }
    }

    suspend fun invoke(input:I): O {
        if(continuation!=null) {
            throw IllegalStateException("broker is busy")
        }
        return suspendCoroutine {
            continuation = it
            launcher.launch(input)
        }
    }
}