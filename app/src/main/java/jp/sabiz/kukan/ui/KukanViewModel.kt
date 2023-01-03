package jp.sabiz.kukan.ui

import android.animation.ValueAnimator
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.os.SystemClock
import android.view.animation.LinearInterpolator
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.sabiz.kukan.common.KukanState
import jp.sabiz.kukan.common.Logger
import jp.sabiz.kukan.common.LongTouchEventDetector
import jp.sabiz.kukan.data.KukanDatabase
import jp.sabiz.kukan.data.entities.Drive
import jp.sabiz.kukan.extension.getElapsedRealtimeMillis
import jp.sabiz.kukan.location.LocationListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class KukanViewModel : LocationListener, ViewModel(),
    LongTouchEventDetector.OnLongTouchEventListener {

    companion object {
        private val Log = Logger.MAIN
        private const val MAX_LOCATION_INTERVAL_MILLIS = 10 * 60 * 1000
        private const val MIN_DISTANCE_METER = 5
        private const val ON_OFF_LONG_TOUCH_TIME_MILLIS = 2000L
    }

    private val state: MutableLiveData<KukanState> = MutableLiveData(KukanState.OFF)
    val kukanState: LiveData<KukanState> = state
    var tripKm = MutableLiveData(0f)
    var averageKPH = MutableLiveData(0.0)
    var time = MutableLiveData("")
    var progressOnOff = MutableLiveData(0)
    val longTouchEventDetector = LongTouchEventDetector(ON_OFF_LONG_TOUCH_TIME_MILLIS, this)
    private var lastLocation: Location? = null
    private var averageKPHCount = 0L
    private var startTimeMillis = 0L
    private var startTimeEpochMillis = 0L
    private val elapsedTimeMillis: Long
    get() = SystemClock.elapsedRealtime() - startTimeMillis
    private val locationList = mutableListOf<Location>()
    private val updateCoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val progressOnOffAnimator: ValueAnimator by lazy {
        val  animator = ValueAnimator.ofInt(0, 100)
        animator.duration = ON_OFF_LONG_TOUCH_TIME_MILLIS
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener {
            progressOnOff.value = it.animatedValue as Int
        }
        return@lazy animator
    }

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

    override fun onLocation(location: Location) {
        dbgMessage.value = "acc:${location.accuracy}"
        locationList.add(location)
    }

    override fun onLongTouchDown() {
        Log.d("TOUCH DOWN")
        state.value?: return
        if (state.value == KukanState.OFF) {
            progressOnOff.value = 0
            progressOnOffAnimator.start()
        } else if (state.value == KukanState.ON) {
            progressOnOff.value = 100
            progressOnOffAnimator.reverse()
        }
    }

    override fun onLongTouchCanceled() {
        Log.d("TOUCH CANCELED")
        progressOnOffAnimator.reverse()
    }

    override fun onLongTouchUp() {
        Log.d("TOUCH UP")
        state.value?: return
        state.value = state.value?.toggle()
        Log.i("long touch: ${state.value}")
        if (state.value == KukanState.ON) {
            startTimeEpochMillis = System.currentTimeMillis()
            startTimeMillis = SystemClock.elapsedRealtime()
            viewModelScope.launch(updateCoroutineDispatcher){
                updateLoop()
            }
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
                viewModelScope.launch(Dispatchers.Main) {
                    tripKm.value = 0f
                    averageKPHCount = 0
                    averageKPH.value = 0.0
                    time.value = ""
                    locationList.clear()
                }
            }
        }
    }

    private suspend fun updateLoop() {
        while (true) {
            if ((state.value ?: KukanState.OFF) == KukanState.ON) {
                val before = SystemClock.elapsedRealtime()
                update()
                val after = SystemClock.elapsedRealtime()
                Log.d("Wait: ${1000 - (after - before)}")
                Thread.sleep(1000 - (after - before))
            }
        }
    }

    private suspend fun update() {
        if (lastLocation == null && locationList.size > 0) {
            lastLocation = locationList.removeFirst()
            return
        }
        val last = lastLocation ?: return
        val new = locationList.removeFirstOrNull()?:
                    withContext(viewModelScope.coroutineContext + Dispatchers.Main) {updateTime()}.let { return }
        val elapsedMillis = new.getElapsedRealtimeMillis() - last.getElapsedRealtimeMillis()
        lastLocation = new
        if(elapsedMillis > MAX_LOCATION_INTERVAL_MILLIS) {
            return
        }
        val distance = last.distanceTo(new)
        withContext(viewModelScope.coroutineContext + Dispatchers.Main) {
            if (MIN_DISTANCE_METER <= distance) {
                updateTrip(distance)
            }
            updateAverageKPH()
            updateTime()
        }
    }

    private fun updateTrip(distanceM: Float) {
        tripKm.value?.let {
            tripKm.value = it + (distanceM / 1000F)
        }
    }

    private fun updateAverageKPH() {
        val trip = tripKm.value ?: return
        val elapsed = elapsedTimeMillis / 1000.0 / 60.0 / 60.0
        averageKPH.value = trip / elapsed
    }

    private fun updateTime() {
        val elapsed = elapsedTimeMillis
        val hour = TimeUnit.MILLISECONDS.toHours(elapsed)
        val min = TimeUnit.MILLISECONDS.toMinutes(elapsed - TimeUnit.HOURS.toMillis(hour))
        val sec = TimeUnit.MILLISECONDS.toSeconds(elapsed - TimeUnit.HOURS.toMillis(hour) - TimeUnit.MINUTES.toMillis(min))
        time.value = "%02d:%02d:%02d".format(hour, min, sec)
    }
}