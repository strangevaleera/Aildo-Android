package com.xinqi.utils.log

import android.content.Context
import android.util.Log
import android.widget.Toast

const val TAG_PRE = "=xq="

fun Any.logI(msg: String) {

    Log.i(TAG_PRE + javaClass.simpleName, msg)

}

fun Any.logE(msg: String, throwable: Throwable? = null) {

    Log.e(TAG_PRE + javaClass.simpleName, msg, throwable)

}

fun Any.showResult(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    logI(msg)
}