package io.github.toyota32k.dialog.broker

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

interface IUtActivityBroker {
    fun register(owner: Fragment)
    fun register(owner: FragmentActivity)
}