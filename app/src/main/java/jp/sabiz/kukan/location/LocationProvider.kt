package jp.sabiz.kukan.location

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.location.Location
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import jp.sabiz.kukan.common.Logger
import mad.location.manager.lib.Interfaces.LocationServiceInterface
import mad.location.manager.lib.Interfaces.LocationServiceStatusInterface
import mad.location.manager.lib.Services.KalmanLocationService
import mad.location.manager.lib.Services.ServicesHelper
import mad.location.manager.lib.Services.Settings

class LocationProvider: LocationCallback(), LocationServiceInterface, LocationServiceStatusInterface {
    companion object {
        private val Log = Logger.LOCATION
        private const val REQUEST_CHECK_SETTINGS = 1010
        private const val REQUEST_INTERVAL_MILLIS = 3000L
        private const val REQUEST_DURATION_MILLIS = Long.MAX_VALUE
        private const val REQUEST_MIN_UPDATE_DISTANCE_METERS = 5F
        private const val RESULT_FILTER_ACCURACY_METERS = 100

        private const val ACCELEROMETER_DEVIATION = 0.1
        private const val POSITION_MIN_TIME = 1000
        private const val GEO_HASH_PRECISION = 6
        private const val GEO_MIN_POINT = 3
        private const val SENSOR_FREQUENCY = 30.0
        private const val DEFAULT_VEL_FACTOR = 1.0
        private const val DEFAULT_POS_FACTOR = 1.0

        private var instance: LocationProvider? = null

        fun get() = instance ?: synchronized(this) {
            instance ?: LocationProvider().also { instance = it }
        }
    }

    private val locationRequest: LocationRequest
    get() = LocationRequest.create()
//        LocationRequest.builder()
        .setInterval(REQUEST_INTERVAL_MILLIS)
//        .setMinUpdateIntervalMillis(0)
        .setFastestInterval(0)
//        .setMaxUpdateDelayMillis(REQUEST_INTERVAL_MILLIS)
        .setMaxWaitTime(REQUEST_INTERVAL_MILLIS)
//        .setDurationMillis(REQUEST_DURATION_MILLIS)
        .setExpirationDuration(REQUEST_DURATION_MILLIS)
//        .setMaxUpdates(Int.MAX_VALUE)
        .setNumUpdates(Int.MAX_VALUE)
//        .setMinUpdateDistanceMeters(REQUEST_MIN_UPDATE_DISTANCE_METERS)
        .setSmallestDisplacement(REQUEST_MIN_UPDATE_DISTANCE_METERS)
        .setWaitForAccurateLocation(false)
        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
//        .setGranularity(Granularity.GRANULARITY_FINE)
//        .setMaxUpdateAgeMillis(LocationRequest.Builder.IMPLICIT_MAX_UPDATE_AGE)
//        .build()

    private val settings: Settings
    get() {
        return Settings(
            ACCELEROMETER_DEVIATION,
            REQUEST_MIN_UPDATE_DISTANCE_METERS.toInt(), // Not working in [Settings.LocationProvider.FUSED]
            REQUEST_INTERVAL_MILLIS.toInt(),
            POSITION_MIN_TIME,
            GEO_HASH_PRECISION,
            GEO_MIN_POINT,
            SENSOR_FREQUENCY,
            null,
            true,
            false,
            true,
            DEFAULT_VEL_FACTOR,
            DEFAULT_POS_FACTOR,
            Settings.LocationProvider.FUSED
        )
    }
    private var locationListener: LocationListener? = null

    fun setup(activity: Activity) {
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity)
        if (code != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().showErrorNotification(activity, code)
        }

        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .setAlwaysShow(true)
            .setNeedBle(true)
            .addLocationRequest(locationRequest)
            .build()

        val client = LocationServices.getSettingsClient(activity)
        val task = client.checkLocationSettings(locationSettingsRequest)
        task.addOnFailureListener {
            if (it is ResolvableApiException) {
                try {
                    it.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS)
                } catch (_: IntentSender.SendIntentException) {

                }
            } else if (it is ApiException && it.status.statusCode == CommonStatusCodes.RESOLUTION_REQUIRED) {
                try {
                    it.status.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS)
                } catch (_: IntentSender.SendIntentException) {

                }
            }
        }
        ServicesHelper.addLocationServiceInterface(this)
        ServicesHelper.addLocationServiceStatusInterface(this)
    }

    fun requestRepeat(context: Context, locationListener: LocationListener) {
        if (this.locationListener != null) {
            return
        }
        this.locationListener = locationListener

        ServicesHelper.getLocationService(context) {
            if (it.IsRunning()) {
                return@getLocationService
            }
            it.reset(settings)
            it.start()
        }

    }

    fun stopRepeat(context: Context) {
        locationListener ?: return
        locationListener = null
        ServicesHelper.getLocationService(context) {
            it.stop()
        }
    }

    override fun onLocationResult(locationResult: LocationResult) {
        Log.i("${locationResult.locations}")

        val locations = locationResult.locations.toMutableList()

        locations.forEach {
            if (it.hasAccuracy() && (it.accuracy <= 0 || it.accuracy >= RESULT_FILTER_ACCURACY_METERS)) {
                return@forEach
            }
            this.locationListener?.onLocation(it)
        }
    }


    override fun locationChanged(location: Location?) {
        val loc = location?:return
        Log.d("loc: $loc")
        if (loc.hasAccuracy() && (loc.accuracy <= 0 || loc.accuracy >= RESULT_FILTER_ACCURACY_METERS)) {
            return
        }
        this.locationListener?.onLocation(loc)
    }

    override fun serviceStatusChanged(status: KalmanLocationService.ServiceStatus?) {
        Log.i("service status: ${status?.name}")
    }

    override fun GPSStatusChanged(activeSatellites: Int) {
        Log.i("activeSatellites: $activeSatellites")
    }

    override fun GPSEnabledChanged(enabled: Boolean) {}

    override fun lastLocationAccuracyChanged(accuracy: Float) {
        Log.i("lastLocationAccuracyChanged: $accuracy")
    }
}