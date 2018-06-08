package net.fan360.geebleytrackingservice.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.v4.content.ContextCompat
import android.util.Log

import com.google.common.collect.ImmutableMap
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.PNCallback
import com.pubnub.api.models.consumer.PNPublishResult
import com.pubnub.api.models.consumer.PNStatus

import net.fan360.geebleytrackingservice.MainActivity
import net.fan360.geebleytrackingservice.constants.AppConstants

class LocationUpdaterService : Service() {

    lateinit var mLocationManager: LocationManager
    lateinit var mLocationListener: LocationUpdaterListener
    var previousBestLocation: Location? = null
    private var mPubnub_DataStream: PubNub? = null

    internal var mHandler = Handler()
    internal var mHandlerTask: Runnable = object : Runnable {
        override fun run() {
            if ((!isRunning!!)!!) {
                startListening()
            }
            mHandler.postDelayed(this, TWO_MINUTES.toLong())
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        mLocationListener = LocationUpdaterListener()
        initPubNub()
        super.onCreate()
    }

    private fun initPubNub() {
        val config = PNConfiguration()

        config.publishKey = AppConstants.PUBNUB_PUBLISH_KEY
        config.subscribeKey = AppConstants.PUBNUB_SUBSCRIBE_KEY
        config.uuid = MainActivity.userName
        config.isSecure = false
        this.mPubnub_DataStream = PubNub(config)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        mHandlerTask.run()
        return Service.START_STICKY
    }

    override fun onDestroy() {
        stopListening()
        mHandler.removeCallbacks(mHandlerTask)
        super.onDestroy()
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (mLocationManager.allProviders.contains(LocationManager.NETWORK_PROVIDER))
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000, 0f, mLocationListener)


            if (mLocationManager.allProviders.contains(LocationManager.GPS_PROVIDER))
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0f, mLocationListener)
        }
        isRunning = true
    }

    private fun stopListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.removeUpdates(mLocationListener)
        }
        isRunning = false
    }

    inner class LocationUpdaterListener : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (isBetterLocation(location, previousBestLocation)) {
                previousBestLocation = location
                try {
                    // Script to post location data to server..
                    Log.e("LocationUpdaterService", "latitude>>>>>>" + location.latitude + "")
                    Log.e("LocationUpdaterService", "Longitude>>>>>>" + location.longitude + "")

                    val message = ImmutableMap.of("username", MainActivity.userName, "latitude", location.latitude.toString() + "", "longitude", location.longitude.toString() + "")

                    mPubnub_DataStream!!.publish().channel(AppConstants.CHANNEL_NAME).message(message).async(
                            object : PNCallback<PNPublishResult>() {
                                override fun onResponse(result: PNPublishResult, status: PNStatus) {
                                    try {
                                        if (!status.isError) {
                                            Log.e(TAG, "publish(" + result.toString() + ")")
                                        } else {
                                            Log.e(TAG, "publishErr(" + status.toString() + "))")
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }

                                }
                            }
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    stopListening()
                }
            }
        }

        override fun onProviderDisabled(provider: String) {
            stopListening()
        }

        override fun onProviderEnabled(provider: String) {}

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    }

    protected fun isBetterLocation(location: Location, currentBestLocation: Location?): Boolean {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true
        }

        // Check whether the new location fix is newer or older
        val timeDelta = location.time - currentBestLocation.time
        val isSignificantlyNewer = timeDelta > TWO_MINUTES
        val isSignificantlyOlder = timeDelta < -TWO_MINUTES
        val isNewer = timeDelta > 0

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false
        }

        // Check whether the new location fix is more or less accurate
        val accuracyDelta = (location.accuracy - currentBestLocation.accuracy).toInt()
        val isLessAccurate = accuracyDelta > 0
        val isMoreAccurate = accuracyDelta < 0
        val isSignificantlyLessAccurate = accuracyDelta > 200

        // Check if the old and new location are from the same provider
        val isFromSameProvider = isSameProvider(location.provider, currentBestLocation.provider)

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true
        } else if (isNewer && !isLessAccurate) {
            return true
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true
        }
        return false
    }

    /**
     * Checks whether two providers are the same
     */
    private fun isSameProvider(provider1: String?, provider2: String?): Boolean {
        return if (provider1 == null) {
            provider2 == null
        } else provider1 == provider2
    }

    companion object {
        private val TAG = "LocationUpdaterService"
        val TWO_MINUTES = 3000 // 120 seconds
        var isRunning: Boolean? = false
    }
}