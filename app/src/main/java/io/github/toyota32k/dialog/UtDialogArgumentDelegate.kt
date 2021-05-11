package io.github.toyota32k.dialog

import android.os.Bundle
import io.github.toyota32k.utils.asArrayOfType
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class UtBundleDelegate<T>(val namespace:String?, val source:()->Bundle) {
    constructor(source:()->Bundle):this(null,source)
//    constructor(fragment:Fragment, namespace:String?=null): this(namespace, { fragment.arguments!! })

    val bundle:Bundle
        get() = source()

    fun key(name:String) : String {
        return if(namespace.isNullOrEmpty()) name else "$namespace.name"
    }

    open inner class GenericDelegate<R>(val conv:(Any?)->R, val rev:((R)->Any?)?) : ReadWriteProperty<T,R> {
        constructor(conv:(Any?)->R):this(conv,null)

        override fun getValue(thisRef: T, property: KProperty<*>): R {
            return conv(bundle.get(key(property.name)))
        }

        override fun setValue(thisRef: T, property: KProperty<*>, value: R) {
            val v = rev?.invoke(value) ?: value
            if(v==null) {
                bundle.remove(key(property.name))
            } else {
                when (v) {
                    is Int -> bundle.putInt(key(property.name), v)
                    is Boolean -> bundle.putBoolean(key(property.name), v)
                    is IntArray -> bundle.putIntArray(key(property.name), v)
                    is BooleanArray -> bundle.putBooleanArray(key(property.name), v)
                    is String -> bundle.putString(key(property.name), v)
                    is Array<*> -> bundle.putStringArray(key(property.name), v.asArrayOfType())
                    // 他のタイプは必要なら追加する
                    else -> throw IllegalArgumentException("unknown type ${v::class.java.name}")
                }
            }
        }
    }

    // Int
    val intNullable:ReadWriteProperty<T,Int?> by lazy { GenericDelegate<Int?>{it as? Int} }
    val intZero:ReadWriteProperty<T,Int> by lazy { GenericDelegate<Int>{(it as? Int)?:0} }
    val intMinusOne:ReadWriteProperty<T,Int> by lazy { GenericDelegate<Int>{(it as? Int)?:-1} }
    fun intNonnull(def:Int) : ReadWriteProperty<T,Int> { return GenericDelegate<Int>{(it as? Int)?:def} }

    // Boolean
    val booleanNullable:ReadWriteProperty<T,Boolean?> by lazy { GenericDelegate<Boolean?>{it as? Boolean} }
    val booleanFalse:ReadWriteProperty<T,Boolean> by lazy { GenericDelegate<Boolean>{(it as? Boolean)?:false} }
    val booleanTrue: ReadWriteProperty<T,Boolean> by lazy { GenericDelegate<Boolean>{(it as? Boolean)?:true} }

    // String
    val string:ReadWriteProperty<T,String> by lazy { GenericDelegate<String>{it as? String ?: ""} }
    val stringNullable:ReadWriteProperty<T,String?> by lazy { GenericDelegate<String?>{it as? String} }
    fun stringNonnull(def:String):ReadWriteProperty<T,String> { return GenericDelegate<String>{(it as? String)?:def} }

    // IntArray
    val intArray:ReadWriteProperty<T,IntArray> by lazy { GenericDelegate<IntArray>{it as? IntArray ?: intArrayOf()} }
    val intArrayNullable:ReadWriteProperty<T,IntArray?> by lazy { GenericDelegate<IntArray?>{it as? IntArray} }
    fun intArrayNonnull(def:()->IntArray):ReadWriteProperty<T,IntArray> { return GenericDelegate<IntArray>{it as? IntArray ?: def()}}

    // BooleanArray
    val booleanArray:ReadWriteProperty<T,BooleanArray> by lazy { GenericDelegate<BooleanArray>{it as? BooleanArray ?: booleanArrayOf()}}
    val booleanArrayNullable:ReadWriteProperty<T,BooleanArray?> by lazy { GenericDelegate<BooleanArray?>{it as? BooleanArray}}
    fun booleanArrayNonnull(def:()->BooleanArray) { GenericDelegate<BooleanArray>{it as? BooleanArray ?: def()}}

    // Array<String>
    val stringArray:ReadWriteProperty<T, Array<String>> by lazy { GenericDelegate{(it as? Array<*>)?.asArrayOfType() ?: arrayOf()}}
    val stringArrayNullable:ReadWriteProperty<T, Array<String>?> by lazy { GenericDelegate{(it as? Array<*>)?.asArrayOfType() }}
    fun stringArrayNonnull(def:()->Array<String>) { GenericDelegate<Array<String>>{ (it as? Array<*>)?.asArrayOfType() ?: def()}}

//    inline fun <reified V> straight(def:V):ReadWriteProperty<T,V> {
//        return GenericDelegate({it as? V ?: def})
//    }

    // enum
    inline fun <reified E:Enum<E>> enum(def:E) : ReadWriteProperty<T,E> {
        return GenericDelegate(
            {(it as? String)?.let { name-> enumValueOf<E>(name) } ?: def},
            { it.toString()})
    }

    fun inherit(namespace: String?) : UtBundleDelegate<T> {
        return UtBundleDelegate("${this.namespace}.$namespace", source)
    }
    fun <T2> export(namespace: String?) : UtBundleDelegate<T2> {
        return UtBundleDelegate("${this.namespace}.$namespace", source)
    }
}

///**
// * プロパティをFragment#argumentsに委譲するためのデリゲートクラス（Nullableなプリミティブ型用）
// */
//class UtDialogArgumentDelegate {
//    inline operator fun <reified T> getValue(thisRef: Fragment, property: KProperty<*>): T {
//        return thisRef.arguments?.get(property.name) as T
//    }
//
//    private fun safeArguments(thisRef: Fragment): Bundle {
//        return thisRef.arguments ?: Bundle().apply { thisRef.arguments = this }
//    }
//
//    operator fun setValue(thisRef: Fragment, property: KProperty<*>, v:String?) {
//        safeArguments(thisRef).putString(property.name, v)
//    }
//    operator fun setValue(thisRef: Fragment, property: KProperty<*>, v:Int?) {
//        safeArguments(thisRef).putInt(property.name, v?:0)
//    }
//    operator fun setValue(thisRef: Fragment, property: KProperty<*>, v:Boolean?) {
//        safeArguments(thisRef).putBoolean(property.name, v?:false)
//    }
//    operator fun setValue(thisRef: Fragment, property: KProperty<*>, v:Array<String>?) {
//        safeArguments(thisRef).putStringArray(property.name, v)
//    }
//    operator fun setValue(thisRef: Fragment, property: KProperty<*>, v:IntArray?) {
//        safeArguments(thisRef).putIntArray(property.name, v)
//    }
//    operator fun setValue(thisRef: Fragment, property: KProperty<*>, v:BooleanArray?) {
//        safeArguments(thisRef).putBooleanArray(property.name, v)
//    }
//}
//
///**
// * プロパティをFragment#argumentsに委譲するためのデリゲートクラス（NotNullなBoolean型用）
// */
//class UtDialogArgumentDelegateBool(private val defaultValue:Boolean=false) {
//    operator fun getValue(thisRef: Fragment, property: KProperty<*>): Boolean {
//        return thisRef.arguments?.getBoolean(property.name, defaultValue) ?: defaultValue
//    }
//    operator fun setValue(thisRef: Fragment, property: KProperty<*>, v:Boolean) {
//        (thisRef.arguments ?: Bundle().apply { thisRef.arguments = this }).putBoolean(property.name, v)
//    }
//}
//
///**
// * プロパティをFragment#argumentsに委譲するためのデリゲートクラス（NotNullなInt型用）
// */
//class UtDialogArgumentDelegateInt(private val defaultValue:Int=0) {
//    operator fun getValue(thisRef: Fragment, property: KProperty<*>): Int {
//        return thisRef.arguments?.getInt(property.name, defaultValue) ?: defaultValue
//    }
//    operator fun setValue(thisRef: Fragment, property: KProperty<*>, v:Int) {
//        (thisRef.arguments ?: Bundle().apply { thisRef.arguments = this }).putInt(property.name, v)
//    }
//}
//
///**
// * プロパティをFragment#argumentsに委譲するための汎用デリゲートクラス（文字列化してBundleに入れておいてconvでenumなどに変換する）
// */
//class UtDialogArgumentGenericDelegate<T>(val conv:(String?)->T) {
//    operator fun getValue(thisRef: Fragment, property: KProperty<*>): T {
//        return conv(thisRef.arguments?.getString(property.name))
//    }
//    operator fun setValue(thisRef: Fragment, property: KProperty<*>, v:T) {
//        (thisRef.arguments ?: Bundle().apply { thisRef.arguments = this }).putString(property.name, v.toString())
//    }
//}
