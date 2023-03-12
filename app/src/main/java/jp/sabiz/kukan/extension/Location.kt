package jp.sabiz.kukan.extension

import android.location.Location

fun Location.getElapsedRealtimeMillisCompat(): Long {
    return (this.elapsedRealtimeNanos / 1e6).toLong()
}