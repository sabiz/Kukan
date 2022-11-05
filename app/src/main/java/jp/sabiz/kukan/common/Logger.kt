package jp.sabiz.kukan.common

import android.util.Log

enum class Logger {
    MAIN,
    LOCATION;

    fun d(msg: String) {
        Log.d(this.name, msg)
    }

    fun i(msg: String) {
        Log.i(this.name, msg)
    }

    fun w(msg: String) {
        Log.w(this.name, msg)
    }

    fun e(msg: String) {
        Log.e(this.name, msg)
    }

}