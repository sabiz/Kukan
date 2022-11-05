package jp.sabiz.kukan.location

import android.location.Location

interface LocationListener {
    fun onLocation(location: Location)
}