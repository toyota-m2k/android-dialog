package io.github.toyota32k.dialog.broker

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import io.github.toyota32k.dialog.task.UtImmortalTaskManager

interface IUtPermissionBrokerProvider {
    val permissionBroker: UtPermissionBroker
}

class UtPermissionBroker() : UtActivityBroker<String, Boolean>() {
    companion object {
        fun isPermitted(context: Context, permission: String):Boolean {
            return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    @Suppress("MemberVisibilityCanBePrivate")
    fun isPermitted(permission: String):Boolean {
        return isPermitted(UtImmortalTaskManager.application, permission)
    }
    override val contract: ActivityResultContract<String, Boolean>
        get() = ActivityResultContracts.RequestPermission()

    @Suppress("unused")
    suspend fun requestPermission(permission:String):Boolean {
        if(isPermitted(permission)) {
            return true // すでに許可されている
        }
        return invoke(permission)
    }
}

@Suppress("unused")
class UtMultiPermissionsBroker() : UtActivityBroker<Array<String>, Map<String,Boolean>>() {
    override val contract: ActivityResultContract<Array<String>, Map<String,Boolean>>
        get() = ActivityResultContracts.RequestMultiplePermissions()

    inner class Request {
        private val list = mutableListOf<String>()
        private var requiredFlags = mutableMapOf<String,Boolean>()
        fun add(permission:String, required:Boolean=true):Request {
            if(!UtPermissionBroker.isPermitted(UtImmortalTaskManager.application,permission)) {
                list.add(permission)
                requiredFlags[permission] = required
            }
            return this
        }
        fun addIf(condition:Boolean, permission:String, required: Boolean=true):Request {
            if(condition) {
                add(permission, required)
            }
            return this
        }
        fun add(permissionFn:()->String?, required: Boolean=true):Request {
            val permission = permissionFn() ?: return this
            add(permission, required)
            return this
        }
        suspend fun execute() : Boolean {
            if(list.isEmpty()) {
                return true
            }
            val result = invoke(list.toTypedArray())
            if(result.containsValue(false)) {
                for(key in result.keys) {
                    if(result[key]!=true && requiredFlags[key]!=false) {
                        // required = true で、resultが true でないものが含まれていたら失敗
                        return false
                    }
                }
            }
            return true
        }
    }
}