package io.github.toyota32k.utils

import java.io.Closeable
import java.lang.Exception

@Suppress("unused")
class UtLog @JvmOverloads constructor(val tag:String, val parent:UtLog?=null, private val outputClassName:Boolean=true, private val outputMethodName:Boolean=true) {
    @Suppress("unused")
    companion object {
        fun hierarchicTag(tag:String, parent:UtLog?):String {
            return if(parent!=null) {
                "${hierarchicTag(parent.tag, parent.parent)}.${tag}"
            } else {
                tag
            }
        }
        fun className():String {
            return Thread.currentThread().stackTrace[2].className
        }
        fun methodName():String {
            return Thread.currentThread().stackTrace[2].methodName
        }
        fun classAndMethodName():Pair<String,String> {
            val e = Thread.currentThread().stackTrace[2]
            return Pair(e.className, e.methodName)
        }

        fun assert(chk:Boolean, msg:String) {
            if(!chk) {
                UtLogger.stackTrace(Exception("assertion failed."), msg)
            }
        }

        val libLogger:UtLog by lazy { UtLog("libUtils") }
    }

    private val logger = UtLoggerInstance(hierarchicTag(tag,parent))

    private fun compose(message:String?):String {
        return if(outputClassName||outputMethodName) {
            val stack = Thread.currentThread().stackTrace
            var n:Int = 4
            var e = stack[n]
            while(e.methodName.endsWith("\$default") && n<stack.size) {
                n++
                e = stack[n]
            }
            if(!outputClassName) {
                if(message!=null) "${e.methodName}: ${message}" else e.methodName
            } else if(!outputMethodName) {
                if(message!=null) "${e.className}:${message}" else e.className
            } else {
                if(message!=null) "${e.className}.${e.methodName}:${message}" else "${e.className}.${e.methodName}"
            }
        } else {
            message ?: ""
        }
    }

    @JvmOverloads
    fun debug(msg: String?=null) {
        logger.debug(compose(msg))
    }

    @JvmOverloads
    fun warn(msg: String?=null) {
        logger.warn(compose(msg))
    }

    @JvmOverloads
    fun error(msg: String?=null) {
        logger.error(compose(msg))
    }

    @JvmOverloads
    fun info(msg: String?=null) {
        logger.info(compose(msg))
    }

    @JvmOverloads
    fun verbose(msg: String?=null) {
        logger.verbose(compose(msg))
    }

    @JvmOverloads
    fun stackTrace(e:Throwable, msg:String?=null) {
        logger.stackTrace(e, compose(msg))
    }

    @JvmOverloads
    fun assert(chk:Boolean, msg:String?=null) {
        if(!chk) {
            stackTrace(Exception("assertion failed."), msg)
        }
    }

    @JvmOverloads
    fun scopeWatch(msg:String?=null) : Closeable {
        val composed = compose(msg)
        logger.debug("$composed - enter")
        return ScopeWatcher { logger.debug("$composed - leave") }
    }

    private class ScopeWatcher(val leaving:()->Unit) : Closeable {
        override fun close() {
            leaving()
        }
    }
}