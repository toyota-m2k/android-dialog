@file:Suppress("unused")

package io.github.toyota32k.dialog

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import io.github.toyota32k.utils.asArrayOfType
import java.io.Serializable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KProperty

fun Bundle.put(key:String, value:Any?):Bundle {
    when(value) {
        null -> remove(key)
        is Boolean -> putBoolean(key, value)
        is BooleanArray -> putBooleanArray(key, value)
        is Bundle -> putBundle(key, value)
        is Byte -> putByte(key, value)
        is ByteArray-> putByteArray(key, value)
        is String -> putString(key, value)
        is Array<*> -> @Suppress("UNCHECKED_CAST") putStringArray(key, value as Array<String?>)     // Array<String>のみサポート
        is ArrayList<*> -> @Suppress("UNCHECKED_CAST") putStringArrayList(key, value as ArrayList<String?>) // 。。。
        is Char -> putChar(key, value)
        is CharArray -> putCharArray(key, value)
        is CharSequence-> putCharSequence(key, value)
        is Double-> putDouble(key, value)
        is DoubleArray->putDoubleArray(key, value)
        is Float->putFloat(key, value)
        is FloatArray->putFloatArray(key, value)
        is Short->putShort(key, value)
        is ShortArray->putShortArray(key, value)
        is Int->putInt(key, value)
        is IntArray->putIntArray(key, value)
        is Long->putLong(key, value)
        is LongArray->putLongArray(key, value)
        is Parcelable ->putParcelable(key, value)
        is Serializable->putSerializable(key, value)
        else-> throw IllegalArgumentException("${key}:unsupported value type (${value.javaClass.simpleName})")
    }
    return this
}

// Bundle#get(key) の Deprecatedを回避するために、ちょっと無理やりなタイプセーフ get を実装。。。
// GenericDelegate で使うため、Any型で返さざるを得ず、どこがセーフやねん、となるのだが、GenericDelegate内でキャストするところがセーフになるのではなかろうか。
// UtBundleDelegateTest で確認したが、UtDialog の委譲プロパティで GenericDelegate を使う限りにおいては、正しく動作している。
// それ以外での利用については責任を持てない。
internal fun Bundle.get(key:String, clazz: KClassifier?):Any? {
    return if(!containsKey(key)) {
        null    // 未定義なら nullを返す
    } else {
        when (clazz) {
            Boolean::class -> getBoolean(key)
            BooleanArray::class -> getBooleanArray(key)
            Bundle::class -> getBundle(key)
            Byte::class -> getByte(key)
            ByteArray::class -> getByteArray(key)
            String::class -> getString(key)
            Array<String>::class -> getStringArray(key)
            ArrayList::class -> getStringArrayList(key)
            Char::class -> getChar(key)
            CharArray::class -> getCharArray(key)
            CharSequence::class -> getCharSequence(key)
            Double::class -> getDouble(key)
            DoubleArray::class -> getDoubleArray(key)
            Float::class -> getFloat(key)
            FloatArray::class -> getFloatArray(key)
            Short::class -> getShort(key)
            ShortArray::class -> getShortArray(key)
            Int::class -> getInt(key)
            IntArray::class -> getIntArray(key)
            Long::class -> getLong(key)
            LongArray::class -> getLongArray(key)
            Parcelable::class -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getParcelable(key, (clazz as KClass<*>).java)
                } else {
                    @Suppress("DEPRECATION")
                    getParcelable(key)
                }
            }
            Serializable::class -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    @Suppress("UNCHECKED_CAST")
                    getSerializable(key, (clazz as KClass<Serializable>).java)
                } else {
                    @Suppress("DEPRECATION")
                    getSerializable(key)
                }
            }
            else -> throw IllegalArgumentException("${key}:unsupported value type (${clazz})")
        }
    }
}

@Suppress("RemoveExplicitTypeArguments", "unused")
class UtBundleDelegate(private val namespace:String?, val source:()->Bundle) {
    constructor(source:()->Bundle):this(null,source)
//    constructor(fragment:Fragment, namespace:String?=null): this(namespace, { fragment.arguments!! })

    val bundle:Bundle
        get() = source()

    fun key(name:String) : String {
        return if(namespace.isNullOrEmpty()) name else "$namespace.name"
    }

    open inner class GenericDelegate<R>(private val clazz: KClassifier?, val conv:(Any?)->R, private val rev:((R)->Any?)?) : ReadWriteProperty<Any,R> {
        constructor(conv:(Any?)->R):this(clazz=null, conv, rev=null)

        override fun getValue(thisRef: Any, property: KProperty<*>): R {
//            @Suppress("DEPRECATION")    // bundle.get()はdeprecateじゃと言われても、これ以外にやりようがないやろ。型毎にデリゲートクラスを作るのはいややし。
//            return conv(bundle.get(key(property.name)))

            return conv(bundle.get(key(property.name), clazz ?: property.returnType.classifier))
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: R) {
            val v = rev?.invoke(value) ?: value
            bundle.put(key(property.name), v)
        }
    }

    // Int
    val intNullable:ReadWriteProperty<Any,Int?> by lazy { GenericDelegate<Int?>{it as? Int} }
    val intZero:ReadWriteProperty<Any,Int> by lazy { GenericDelegate<Int>{(it as? Int)?:0} }
    val intMinusOne:ReadWriteProperty<Any,Int> by lazy { GenericDelegate<Int>{(it as? Int)?:-1} }
    fun intNonnull(def:Int) : ReadWriteProperty<Any,Int> { return GenericDelegate<Int>{(it as? Int)?:def} }

    // Long
    val longNullable:ReadWriteProperty<Any,Long?> by lazy { GenericDelegate<Long?>{it as? Long} }
    val longZero:ReadWriteProperty<Any,Long> by lazy { GenericDelegate<Long>{(it as? Long)?:0L} }
    val longMinusOne:ReadWriteProperty<Any,Long> by lazy { GenericDelegate<Long>{(it as? Long)?:-1L} }
    fun longNonnull(def:Long) : ReadWriteProperty<Any,Long> { return GenericDelegate<Long>{(it as? Long)?:def} }

    // Float
    val floatNullable:ReadWriteProperty<Any,Float?> by lazy { GenericDelegate<Float?>{it as? Float} }
    val floatZero:ReadWriteProperty<Any,Float> by lazy { GenericDelegate<Float>{ (it as? Float) ?: 0f }}
    fun floatNonnull(def:Float) : ReadWriteProperty<Any,Float> { return GenericDelegate<Float>{ (it as? Float) ?: def }}

    // Boolean
    val booleanNullable:ReadWriteProperty<Any,Boolean?> by lazy { GenericDelegate<Boolean?>{it as? Boolean} }
    val booleanFalse:ReadWriteProperty<Any,Boolean> by lazy { GenericDelegate<Boolean>{(it as? Boolean)?:false} }
    val booleanTrue: ReadWriteProperty<Any,Boolean> by lazy { GenericDelegate<Boolean>{(it as? Boolean)?:true} }
    fun booleanWithDefault(def:Boolean) : ReadWriteProperty<Any,Boolean> { return GenericDelegate<Boolean>{(it as? Boolean)?:def} }
    fun booleanNonnull(def:Boolean) : ReadWriteProperty<Any,Boolean> = booleanWithDefault(def)

    // String
    val string:ReadWriteProperty<Any,String> by lazy { GenericDelegate<String>{it as? String ?: ""} }
    val stringNullable:ReadWriteProperty<Any,String?> by lazy { GenericDelegate<String?>{it as? String} }
    fun stringNonnull(def:String):ReadWriteProperty<Any,String> { return GenericDelegate<String>{(it as? String)?:def} }

    // IntArray
    val intArray:ReadWriteProperty<Any,IntArray> by lazy { GenericDelegate<IntArray>{it as? IntArray ?: intArrayOf()} }
    val intArrayNullable:ReadWriteProperty<Any,IntArray?> by lazy { GenericDelegate<IntArray?>{it as? IntArray} }
    fun intArrayNonnull(def:()->IntArray):ReadWriteProperty<Any,IntArray> { return GenericDelegate<IntArray>{it as? IntArray ?: def()}}

    // BooleanArray
    val booleanArray:ReadWriteProperty<Any,BooleanArray> by lazy { GenericDelegate<BooleanArray>{it as? BooleanArray ?: booleanArrayOf()}}
    val booleanArrayNullable:ReadWriteProperty<Any,BooleanArray?> by lazy { GenericDelegate<BooleanArray?>{it as? BooleanArray}}
    fun booleanArrayNonnull(def: () -> BooleanArray): ReadWriteProperty<Any, BooleanArray> { return GenericDelegate<BooleanArray> { it as? BooleanArray ?: def() } }

    // Array<String>
    val stringArray:ReadWriteProperty<Any, Array<String>> by lazy { GenericDelegate{(it as? Array<*>)?.asArrayOfType() ?: arrayOf()}}
    val stringArrayNullable:ReadWriteProperty<Any, Array<String>?> by lazy { GenericDelegate{(it as? Array<*>)?.asArrayOfType() }}
    fun stringArrayNonnull(def:()->Array<String>):ReadWriteProperty<Any,Array<String>> { return GenericDelegate<Array<String>>{ (it as? Array<*>)?.asArrayOfType() ?: def()} }

//    inline fun <reified V> straight(def:V):ReadWriteProperty<Any,V> {
//        return GenericDelegate({it as? V ?: def})
//    }

    // enum
    inline fun <reified E:Enum<E>> enum(def:E) : ReadWriteProperty<Any,E> {
        return GenericDelegate( String::class,
            {(it as? String)?.let { name-> enumValueOf<E>(name) } ?: def},
            { it.toString()})
    }

    fun inherit(namespace: String?) : UtBundleDelegate {
        return UtBundleDelegate("${this.namespace}.$namespace", source)
    }
//    fun <T2> export(namespace: String?) : UtBundleDelegate {
//        return UtBundleDelegate("${this.namespace}.$namespace", source)
//    }
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
