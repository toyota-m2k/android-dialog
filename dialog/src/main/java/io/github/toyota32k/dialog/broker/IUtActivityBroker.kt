package io.github.toyota32k.dialog.broker

import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import io.github.toyota32k.dialog.UtDialogOwner

interface IUtActivityBroker {
    fun register(owner: Fragment)
    fun register(owner: FragmentActivity)
}