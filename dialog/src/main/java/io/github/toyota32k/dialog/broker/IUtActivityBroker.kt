package io.github.toyota32k.dialog.broker

import androidx.activity.result.ActivityResultCaller

interface IUtActivityBroker {
    fun register(owner: ActivityResultCaller)
}