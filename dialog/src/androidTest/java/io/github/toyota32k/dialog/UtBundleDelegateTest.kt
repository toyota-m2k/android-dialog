package io.github.toyota32k.dialog

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class UtBundleDelegateTest {
//    @Test
//    fun useAppContext() {
//        // Context of the app under test.
//        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
//        assertEquals("io.github.toyota32k.dialog", appContext.packageName)
//    }

    @Test
    fun putAndGet() {
        val bundle = Bundle()
        val subBundle = Bundle().apply { put("String", "fuga") }
        val byte:Byte = 8
        val boolArray = booleanArrayOf(true,false,true,false)
        val stringArray:Array<String> = arrayOf("hoge", "fuga", "moge")

        val hoge = "hoge"
        val char:Char = 'a'
        val charArray = charArrayOf('a', 'b', 'c')
        val charSequence = StringBuffer().append("abcdefg").subSequence(1,2) // "bc"
        val double:Double = 12.3
        val doubleArray = doubleArrayOf(1.1, 2.2, 3.3)
        val float:Float = 45.6f
        val floatArray = floatArrayOf(4.4f, 5.5f, 6.6f)
        val short: Short = 100
        val shortArray = shortArrayOf(100, 101, 102)
        val int:Int = 200
        val intArray = intArrayOf(200, 201,202)
        val long:Long = 300L
        val longArray = longArrayOf(300,301,302)

        bundle.put("Boolean", true)
        bundle.put("BoolArray", boolArray)
        bundle.put("Bundle", subBundle)
        bundle.put("Byte", byte)
        bundle.put("ByteArray", hoge.toByteArray())
        bundle.put("String", hoge)
        bundle.put("StringArray", stringArray)
        bundle.put("StringArrayList", ArrayList<String>(stringArray.toList()))
        bundle.put("Char", char)
        bundle.put("CharArray", charArray)
        bundle.put("CharSequence", charSequence)
        bundle.put("Double",double)
        bundle.put("DoubleArray", doubleArray)
        bundle.put("Float", float)
        bundle.put("FloatArray", floatArray)
        bundle.put("Short", short)
        bundle.put("ShortArray", shortArray)
        bundle.put("Int", int)
        bundle.put("IntArray", intArray)
        bundle.put("Long", long)
        bundle.put("LongArray", longArray)

        assertEquals(true, bundle.get("Boolean", Boolean::class))
        assertArrayEquals(boolArray, bundle.get("BoolArray", boolArray::class) as BooleanArray)

        val sb:Bundle = bundle.get("Bundle", Bundle::class) as Bundle
        assertEquals("fuga", sb.get("String", String::class))

        assertEquals(byte, bundle.get("Byte",Byte::class))
        assertArrayEquals(hoge.toByteArray(), bundle.get("ByteArray", ByteArray::class) as ByteArray)

        assertEquals(hoge, bundle.get("String", String::class))
        assertEquals(stringArray, bundle.get("StringArray", Array<String>::class))

        assertArrayEquals(stringArray, (bundle.get("StringArrayList", ArrayList::class) as ArrayList<*>).toArray())
        assertEquals(char, bundle.get("Char", Char::class))
        assertArrayEquals(charArray, bundle.get("CharArray", charArray::class) as CharArray)

        assertEquals(charSequence, bundle.get("CharSequence", CharSequence::class))

        assertEquals(double, bundle.get("Double", Double::class))
        assertArrayEquals(doubleArray.map { (it*100).toLong() }.toLongArray(), (bundle.get("DoubleArray", DoubleArray::class) as DoubleArray).map { (it*100).toLong() }.toLongArray())
        assertEquals(float, bundle.get("Float", Float::class))
        assertArrayEquals(floatArray.map { (it*100).toLong() }.toLongArray(), (bundle.get("FloatArray", FloatArray::class) as FloatArray).map { (it*100).toLong() }.toLongArray())
        assertEquals(short, bundle.get("Short", Short::class))
        assertArrayEquals(shortArray, bundle.get("ShortArray", shortArray::class) as ShortArray)

        assertEquals(int, bundle.get("Int", Int::class))
        assertArrayEquals(intArray, bundle.get("IntArray", intArray::class) as IntArray)

        assertEquals(long, bundle.get("Long", Long::class))
        assertArrayEquals(longArray, bundle.get("LongArray", longArray::class) as LongArray)
    }

    class BundledMember {
        val internalBundle = Bundle()
        val bundle = UtBundleDelegate { internalBundle }

        // Int
        var intNullable by bundle.intNullable
        var intZero by bundle.intZero
        var intMinusOne by bundle.intMinusOne
        var intNonnull by bundle.intNonnull(1234)

        // Long
        var longNullable by bundle.longNullable
        var longZero by bundle.longZero
        var longMinusOne by bundle.longMinusOne
        var longNonnull by bundle.longNonnull(5678L)

        // Float
        var floatNullable by bundle.floatNullable
        var floatZero by bundle.floatZero
        var floatNonnull by bundle.floatNonnull(1.2f)

        // Boolean
        var booleanNullable by bundle.booleanNullable
        var booleanFalse by bundle.booleanFalse
        var booleanTrue by bundle.booleanTrue

        // String
        var string by bundle.string
        var stringNullable by bundle.stringNullable
        var stringNonnull by bundle.stringNonnull("hoge")

        // IntArray
        var intArray by bundle.intArray
        var intArrayNullable by bundle.intArrayNullable
        var intArrayNonnull by bundle.intArrayNonnull { intArrayOf(1,2,3) }

        // BooleanArray
        var booleanArray by bundle.booleanArray
        var booleanArrayNullable by bundle.booleanArrayNullable
        var booleanArrayNonnull by bundle.booleanArrayNonnull { booleanArrayOf(true,false,true) }

        // Array<String>
        var stringArray by bundle.stringArray
        var stringArrayNullable by bundle.stringArrayNullable
        var stringArrayNonnull by bundle.stringArrayNonnull {arrayOf("hoge", "fuga", "moge") }

        // enum
        enum class SomeEnum {
            Alpha,Beta,Gamma,Delta
        }
        var some by bundle.enum(SomeEnum.Alpha)
    }
    @Test
    fun propertyTest() {
        val x = BundledMember()

        // Int
        assertNull(x.intNullable)
        x.intNullable = 100
        assertEquals(100, x.intNullable)
        x.intNullable = null
        assertNull(x.intNullable)

        assertEquals(0, x.intZero)
        // x.intZero = null これはコンパイルエラー
        x.intZero = -200
        assertEquals(-200, x.intZero)

        assertEquals(-1, x.intMinusOne)
        x.intMinusOne = 0
        assertEquals(0, x.intMinusOne)

        assertEquals(x.intNonnull, 1234)
        x.intNonnull = 2345
        assertEquals(x.intNonnull, 2345)

        // Long
        assertNull(x.longNullable)
        x.longNullable = 100L
        assertEquals(x.longNullable, 100L)
        x.longNullable = null
        assertNull(x.longNullable)

        assertEquals(0, x.longZero)
        // x.longZero = null
        x.longZero = 1000L
        assertEquals(1000L, x.longZero)

        assertEquals(-1, x.longMinusOne)
        x.longMinusOne = 0
        assertEquals(0, x.longMinusOne)

        assertEquals(5678L, x.longNonnull)
        x.longNonnull = -100L
        assertEquals(-100L, x.longNonnull)

        // Float
        assertNull(x.floatNullable)
        x.floatNullable = 1.0f
        assertEquals(1.0f, x.floatNullable)
        x.floatNullable = null
        assertNull(x.floatNullable)

        assertEquals(0f, x.floatZero)
        x.floatZero = 1.0f
        assertEquals(1.0f, x.floatZero)
        assertEquals(1.2f, x.floatNonnull)
        x.floatNonnull = 0f
        assertEquals(0f, x.floatNonnull)

        // Boolean
        assertNull(x.booleanNullable)
        x.booleanNullable = true
        assertTrue(x.booleanNullable!!)

        assertTrue(x.booleanTrue)
        x.booleanTrue = false
        assertFalse(x.booleanTrue)

        assertFalse(x.booleanFalse)
        x.booleanFalse = true
        assertTrue(x.booleanFalse)

        // String
        assertEquals("", x.string)
        //x.string = null
        x.string = "hoge"
        assertEquals("hoge", x.string)

        assertNull(x.stringNullable)
        x.stringNullable = "fuga"
        assertEquals("fuga", x.stringNullable)
        x.stringNullable = null
        assertNull(x.stringNullable)

        assertEquals("hoge", x.stringNonnull)
        x.stringNonnull = "moge"
        assertEquals("moge", x.stringNonnull)

        // IntArray
        val refIntArray = intArrayOf(1,2,3)
        val refIntArray2 = intArrayOf(4,5,1,2,3)
        assertEquals(0, x.intArray.size)
        assertNull(x.intArrayNullable)
        assertArrayEquals(refIntArray, x.intArrayNonnull)
        x.intArray = refIntArray
        assertArrayEquals(refIntArray, x.intArray)
        x.intArrayNullable = refIntArray2
        assertArrayEquals(refIntArray2, x.intArrayNullable)
        x.intArrayNullable = null
        assertNull(x.intArrayNullable)
        x.intArrayNonnull = refIntArray2
        assertArrayEquals(refIntArray2, x.intArrayNonnull)

        // BooleanArray
        val refBooleanArray = booleanArrayOf(true, false, true)
        val refBooleanArray2 = booleanArrayOf(false, true, false, false, true)
        assertEquals(0, x.booleanArray.size)
        assertNull(x.booleanArrayNullable)
        assertArrayEquals(refBooleanArray, x.booleanArrayNonnull)
        x.booleanArray = refBooleanArray
        assertArrayEquals(refBooleanArray, x.booleanArray)
        x.booleanArrayNullable = refBooleanArray2
        assertArrayEquals(refBooleanArray2, x.booleanArrayNullable)
        x.booleanArrayNullable = null
        assertNull(x.booleanArrayNullable)
        x.booleanArrayNonnull = refBooleanArray2
        assertArrayEquals(refBooleanArray2, x.booleanArrayNonnull)

        // Array<String>
        val refStringArray = arrayOf("hoge", "fuga", "moge")
        val refStringArray2 = arrayOf("1", "2", "3", "4")
        assertEquals(0, x.stringArray.size)
        assertNull(x.stringArrayNullable)
        assertArrayEquals(refStringArray, x.stringArrayNonnull)
        x.stringArray = refStringArray
        assertArrayEquals(refStringArray, x.stringArray)
        x.stringArrayNullable = refStringArray2
        assertArrayEquals(refStringArray2, x.stringArrayNullable)
        x.stringArrayNullable = null
        assertNull(x.stringArrayNullable)
        x.stringArrayNonnull = refStringArray2
        assertArrayEquals(refStringArray2, x.stringArrayNonnull)

        // enum
        assertEquals(BundledMember.SomeEnum.Alpha, x.some)
        x.some = BundledMember.SomeEnum.Delta
        assertEquals(BundledMember.SomeEnum.Delta, x.some)
    }

}