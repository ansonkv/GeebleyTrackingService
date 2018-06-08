package net.fan360.geebleytrackingservice

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub

import net.fan360.geebleytrackingservice.constants.AppConstants
import net.fan360.geebleytrackingservice.service.LocationUpdaterService

class MainActivity : AppCompatActivity() {

    private var mAlreadyStartedService = false
    private var editText: EditText? = null
    private var textView: TextView? = null
    private var button: Button? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        editText = findViewById(R.id.et_user_name)
        button = findViewById(R.id.btn_submit)
        textView = findViewById(R.id.tv_tracking)
        button!!.setOnClickListener {
            userName = editText!!.text.toString() { it <= ' ' }
            checkInternetConnectivity(null)
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        stopService(Intent(this, LocationUpdaterService::class))
        mAlreadyStartedService = false
        super.onDestroy()
    }

    private fun startService() {
        if (!mAlreadyStartedService) {
            val intent = Intent(this, LocationUpdaterService::class)
            startService(intent)
            textView!!.visibility = View.VISIBLE
            mAlreadyStartedService = true
        }
    }

    private fun checkInternetConnectivity(dialog: DialogInterface?): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo

        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected) {
            promptInternetConnect()
            return false
        }
        dialog?.dismiss()
        if (checkPermissions()) {

            startService()
        } else {
            requestPermissions()
        }

        return true
    }

    private fun promptInternetConnect() {
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("No Internet")
        builder.setMessage("Ni ointernet")

        val positiveText = "Refresh"
        builder.setPositiveButton(positiveText
        ) { dialog, which ->
            if (checkInternetConnectivity(dialog)) {
                if (checkPermissions()) {
                    startService()
                } else if (!checkPermissions()) {
                    requestPermissions()
                }
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun checkPermissions(): Boolean {
        val permissionState1 = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)

        val permissionState2 = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)

        return permissionState1 == PackageManager.PERMISSION_GRANTED && permissionState2 == PackageManager.PERMISSION_GRANTED

    }

    /**
     * Start permissions requests.
     */
    private fun requestPermissions() {

        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)

        val shouldProvideRationale2 = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)

        if (shouldProvideRationale || shouldProvideRationale2) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")
            showSnackBar(R.string.permission_rationale,
                    android.R.string.ok, View.OnClickListener {
                // Request permission
                ActivityCompat.requestPermissions(this@MainActivity,
                        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        REQUEST_PERMISSIONS_REQUEST_CODE)
            })
        } else {
            Log.i(TAG, "Requesting permission")
            ActivityCompat.requestPermissions(this@MainActivity,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }

    private fun showSnackBar(mainTextStringId: Int, actionStringId: Int,
                             listener: View.OnClickListener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show()
    }

    companion object {
        private val TAG = "MainActivity"
        private val REQUEST_PERMISSIONS_REQUEST_CODE = 34
        lateinit var userName: String
    }
}
