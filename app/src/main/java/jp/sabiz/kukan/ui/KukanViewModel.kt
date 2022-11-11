package jp.sabiz.kukan.ui

import android.icu.text.SimpleDateFormat
import android.location.Location
import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.sabiz.kukan.common.KukanState
import jp.sabiz.kukan.common.Logger
import jp.sabiz.kukan.data.KukanDatabase
import jp.sabiz.kukan.data.entities.Drive
import jp.sabiz.kukan.extension.getElapsedRealtimeMillis
import jp.sabiz.kukan.location.LocationListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class KukanViewModel : LocationListener, ViewModel() {

    companion object {
        private val Log = Logger.MAIN
        private const val MAX_LOCATION_INTERVAL_MILLIS = 10 * 60 * 1000
        private const val MIN_DISTANCE_METER = 5
        private const val MIN_SPEED_KPH_OF_CALCULATE_AVERAGE = 5
    }

    private val state: MutableLiveData<KukanState> = MutableLiveData(KukanState.OFF)
    val kukanState: LiveData<KukanState> = state
    var tripKm = MutableLiveData(0f)
    var averageKPH = MutableLiveData(0.0)
    var time = MutableLiveData("")
    private var lastLocation: Location? = null
    private var averageKPHCount = 0L
    private var startTimeMillis = 0L
    private var startTimeEpochMillis = 0L

    // For DEBUG
    var dbgMessage = MutableLiveData("")
    var kukanData = MutableLiveData("")

    fun loadDb() {
        viewModelScope.launch(Dispatchers.IO) {
            val db = KukanDatabase.get()
            val drivers = db.driveDao().getAll()
            var resultText = ""
            val format = SimpleDateFormat("yyyy/MM/dd hh:mm", Locale.JAPAN)

            drivers.forEach {
                resultText += "${format.format(Date(it.time))}, ${it.totalTime}, ${"%05.1f".format(it.trip)}, ${"%05.1f".format(it.avgKph)}\n"
            }

            viewModelScope.launch {
                kukanData.value = resultText
            }
        }
    }

    fun onLongClickButtonStart(): Boolean {
        state.value = state.value?.toggle()
        Log.i("onClickButtonStart: ${state.value}")
        state.value?.let {
            if (it == KukanState.ON) {
                startTimeEpochMillis = System.currentTimeMillis()
                startTimeMillis = SystemClock.elapsedRealtime()
                tripKm.value = 0f
                averageKPHCount = 0
                averageKPH.value = 0.0
                time.value = ""
            } else {
                viewModelScope.launch(Dispatchers.IO) {
                    val drive = Drive(
                        startTimeEpochMillis,
                        time.value?: "",
                        tripKm.value?.toDouble()?: 0.0,
                        averageKPH.value?: 0.0
                    )
                    val db = KukanDatabase.get()
                    db.driveDao().insert(drive)
                }
            }
        }
        return true
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
            updateAverageKPH(location.speed)
            updateTime()

            dbgMessage.value = "acc:${location.accuracy} \n" +
                                "distance: $distance \n" +
                                "elapsedMillis: $elapsedMillis"
            Log.i("${dbgMessage.value}")
            lastLocation = location
        }
    }

    private fun updateTrip(distanceM: Float) {
        tripKm.value?.let {
            tripKm.value = it + (distanceM / 1000F)
        }
    }

    private fun updateAverageKPH(speedMPS: Float) {
        val speedKPH = (speedMPS * 60.0 * 60.0 / 1000.0)
        if (speedKPH < MIN_SPEED_KPH_OF_CALCULATE_AVERAGE) {
            return
        }
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