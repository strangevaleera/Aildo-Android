package com.xinqi.utils.log

import android.util.Log

const val TAG_PRE = "=xq="

fun Any.logI(msg: String) {

    Log.i(TAG_PRE + javaClass.simpleName, msg)

}

fun Any.logE(msg: String, throwable: Throwable? = null) {

    Log.e(TAG_PRE + javaClass.simpleName, msg, throwable)

}