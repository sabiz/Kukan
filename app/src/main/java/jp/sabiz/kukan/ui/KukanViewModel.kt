package jp.sabiz.kukan.ui

import android.location.Location
import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import jp.sabiz.kukan.common.KukanState
import jp.sabiz.kukan.common.Logger
import jp.sabiz.kukan.extension.getElapsedRealtimeMillis
import jp.sabiz.kukan.location.LocationListener
import java.util.concurrent.TimeUnit

class KukanViewModel : LocationListener, ViewModel() {

    companion object {
        private val Log = Logger.MAIN
        private const val MAX_LOCATION_INTERVAL_MILLIS = 10 * 60 * 1000
        private const val MIN_DISTANCE_METER = 5
//        private const val MIN_DISTANCE_METER = 0
    }

    private val state: MutableLiveData<KukanState> = MutableLiveData(KukanState.OFF)
    val kukanState: LiveData<KukanState> = state
    var tripKm = MutableLiveData(0f)
    var averageKPH = MutableLiveData(0.0)
    var time = MutableLiveData("")
    private var lastLocation: Location? = null
    private var averageKPHCount = 0L
    private var startTimeMillis = 0L

    // For DEBUG
    var dbgMessage = MutableLiveData("")

    fun onClickButtonStart() {
        state.value = state.value?.toggle()
        Log.i("onClickButtonStart: ${state.value}")
        state.value?.let {
            if (it == KukanState.ON) {
                startTimeMillis = SystemClock.elapsedRealtime()
                tripKm.value = 0f
                averageKPHCount = 0
                averageKPH.value = 0.0
                time.value = ""
            }
        }
    }

    override fun onLocation(location: Location) {
        dbgMessage.value = "acc:${location.accuracy}"

        if (lastLocation == null) {
            lastLocation = location
            return
        }
        lastLocation?.let {
            val elapsedMillis = location.getElapsedRealtimeMillis() - it.getElapsedRealtimeMillis()
            if(elapsedMillis > MAX_LOCATION_INTERVAL_MILLIS) {
                lastLocation = location
                return
            }

            val distance = it.distanceTo(location)
            if (MIN_DISTANCE_METER > distance) {
                lastLocation = location
                return
            }
            updateTrip(distance)
            updateAverageKPH(distance, elapsedMillis)
            updateTime()

            dbgMessage.value = "TRIP: ${tripKm.value}\n" +
                                "distance: $distance \n" +
                                "elapsedMillis: $elapsedMillis \n" +
                                "averageKPH: ${averageKPH.value} \n" +
                                "time: ${time.value} \n"
            Log.i("${dbgMessage.value}")
            lastLocation = location
        }
    }

    private fun updateTrip(distanceM: Float) {
        tripKm.value?.let {
            tripKm.value = it + (distanceM / 1000F)
        }
    }

    private fun updateAverageKPH(distanceM: Float, elapsedMillis: Long) {
        val elapsedSec =elapsedMillis / 1000.0
        val speedMPS = distanceM / elapsedSec
        val speedKPH = (speedMPS * 60.0 * 60.0 / 1000.0)

        averageKPH.value?.let {
            averageKPH.value = (averageKPHCount * it + speedKPH) / (averageKPHCount + 1)
            averageKPHCount += 1
        }
    }

    private fun updateTime() {
        val current = SystemClock.elapsedRealtime()
        val elapsed = current - startTimeMillis
        val hour = TimeUnit.MILLISECONDS.toHours(elapsed)
        val min = TimeUnit.MILLISECONDS.toMinutes(elapsed - TimeUnit.HOURS.toMillis(hour))
        val sec = TimeUnit.MILLISECONDS.toSeconds(elapsed - TimeUnit.HOURS.toMillis(hour) - TimeUnit.MINUTES.toMillis(min))
        time.value = "%02d:%02d:%02d".format(hour, min, sec)
    }
}