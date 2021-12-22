package io.github.toyota32k.dialog.task

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.toyota32k.dialog.UtBundleDelegate

abstract class UtTaskAwareFragment: Fragment() {
    val bundle = UtBundleDelegate { ensureArguments() }
    var immortalTaskName: String? by bundle.stringNullable
    private val ownerTask:IUtImmortalTask? get() = immortalTaskName?.let { UtImmortalTaskManager.taskOf(it)?.task }
    protected fun <T> getViewModel(clazz:Class<T>):T? where T:ViewModel {
        return ownerTask?.let { ViewModelProvider(it.immortalTaskContext, ViewModelProvider.NewInstanceFactory())[clazz] }
    }
}

fun Fragment.ensureArguments(): Bundle {
    return this.arguments ?: Bundle().apply { arguments = this }
}

