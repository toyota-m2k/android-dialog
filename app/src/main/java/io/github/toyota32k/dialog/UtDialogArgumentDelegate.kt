package io.github.toyota32k.dialog

import android.os.Bundle
import androidx.fragment.app.Fragment
import kotlin.reflect.KProperty

/**
 * プロパティをFragment#argumentsに委譲するためのデリゲートクラス（Nullableなプリミティブ型用）
 */
class UtDialogArgumentDelegate {
    inline operator fun <reified T> getValue(thisRef: Fragment, property: KProperty<*>): T {
        return thisRef.arguments?.get(property.name) as T
    }

    private fun safeArguments(thisRef: Fragment): Bundle {
        return thisRef.arguments ?: Bundle().apply { thisRef.arguments = this }
    }

    operator fun setValue(thisRef: Fragment, property: KProperty<*>, v:String?) {
        safeArguments(thisRef).putString(property.name, v)
    }
    operator fun setValue(thisRef: Fragment, property: KProperty<*>, v:Int?) {
        safeArguments(thisRef).putInt(property.name, v?:0)
    }
    operator fun setValue(thisRef: Fragment, property: KProperty<*>, v:Boolean?) {
        safeArguments(thisRef).putBoolean(property.name, v?:false)
    }
    operator fun setValue(thisRef: Fragment, property: KProperty<*>, v:Array<String>?) {
        safeArguments(thisRef).putStringArray(property.name, v)
    }
    operator fun setValue(thisRef: Fragment, property: KProperty<*>, v:IntArray?) {
        safeArguments(thisRef).putIntArray(property.name, v)
    }
    operator fun setValue(thisRef: Fragment, property: KProperty<*>, v:BooleanArray?) {
        safeArguments(thisRef).putBooleanArray(property.name, v)
    }
}

/**
 * プロパティをFragment#argumentsに委譲するためのデリゲートクラス（NotNullなBoolean型用）
 */
class UtDialogArgumentDelegateBool(private val defaultValue:Boolean=false) {
    operator fun getValue(thisRef: Fragment, property: KProperty<*>): Boolean {
        return thisRef.arguments?.getBoolean(property.name, defaultValue) ?: defaultValue
    }
    operator fun setValue(thisRef: Fragment, property: KProperty<*>, v:Boolean) {
        (thisRef.arguments ?: Bundle().apply { thisRef.arguments = this }).putBoolean(property.name, v)
    }
}

/**
 * プロパティをFragment#argumentsに委譲するためのデリゲートクラス（NotNullなInt型用）
 */
class UtDialogArgumentDelegateInt(private val defaultValue:Int=0) {
    operator fun getValue(thisRef: Fragment, property: KProperty<*>): Int {
        return thisRef.arguments?.getInt(property.name, defaultValue) ?: defaultValue
    }
    operator fun setValue(thisRef: Fragment, property: KProperty<*>, v:Int) {
        (thisRef.arguments ?: Bundle().apply { thisRef.arguments = this }).putInt(property.name, v)
    }
}

/**
 * プロパティをFragment#argumentsに委譲するための汎用デリゲートクラス（文字列化してBundleに入れておいてconvでenumなどに変換する）
 */
class UtDialogArgumentGenericDelegate<T>(val conv:(String?)->T) {
    operator fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        return conv(thisRef.arguments?.getString(property.name))
    }
    operator fun setValue(thisRef: Fragment, property: KProperty<*>, v:T) {
        (thisRef.arguments ?: Bundle().apply { thisRef.arguments = this }).putString(property.name, v.toString())
    }
}
