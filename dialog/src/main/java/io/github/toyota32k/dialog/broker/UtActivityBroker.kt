package io.github.toyota32k.dialog.broker

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import io.github.toyota32k.dialog.task.IUtImmortalTaskContext
import io.github.toyota32k.dialog.task.UtImmortalTaskContext
import io.github.toyota32k.utils.UtLog
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * UtActivityConnectorは必要以上に複雑化してしまったので、
 * ImmortalTaskから呼び出すことに限定して、超絶簡略化したバージョンを用意してみる。
 */

abstract class UtActivityBroker<I,O>
    : ActivityResultCallback<O>, IUtActivityBroker {
    companion object {
        val logger = UtLog("UtActivityBroker")
        var continuation:Continuation<*>? = null
    }

    abstract val contract: ActivityResultContract<I,O>
    private lateinit var launcher: ActivityResultLauncher<I>
    private var taskContext: IUtImmortalTaskContext? = null

    override fun register(owner: Fragment) {
        logger.debug()
        launcher = owner.registerForActivityResult(contract, this)
    }
    override fun register(owner: FragmentActivity) {
        logger.debug()
        launcher = owner.registerForActivityResult(contract, this)
    }

    override fun onActivityResult(result: O) {
        @Suppress("UNCHECKED_CAST")
        (continuation as? Continuation<O>)?.resume(result)
        continuation = null
    }

    suspend fun invoke(input:I): O {
        if(continuation!=null) {
            throw IllegalStateException("broker is busy.")
        }
        return suspendCoroutine {
            continuation = it
            launcher.launch(input)
        }
    }
}