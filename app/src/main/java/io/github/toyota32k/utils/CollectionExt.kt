package io.github.toyota32k.utils

fun <K,V> MutableMap<K,V>.setAndGet(key:K,value:V):V {
    set(key, value)
    return value
}

