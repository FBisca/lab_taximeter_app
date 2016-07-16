package com.bisca.taximeter.view.ui.location

import android.Manifest
import android.app.Activity
import android.support.v4.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.location.LocationSettingsResult

abstract class LocationManagerException : Throwable() {
  abstract fun hasSolution(): Boolean
  abstract fun startActivityForSolution(activity: Activity, requestCode: Int)
}

class LocationSettingsException(
    val locationSettingsResult: LocationSettingsResult
) : LocationManagerException() {

  override fun hasSolution(): Boolean {
    return locationSettingsResult.status.hasResolution()
  }

  override fun startActivityForSolution(activity: Activity, requestCode: Int) {
    locationSettingsResult.status.startResolutionForResult(activity, requestCode)
  }
}

class GoogleApiConnectException(
    val connectionResult: ConnectionResult? = null
) : LocationManagerException() {

  override fun hasSolution(): Boolean {
    return connectionResult?.hasResolution() ?: false
  }

  override fun startActivityForSolution(activity: Activity, requestCode: Int) {
    connectionResult?.startResolutionForResult(activity, requestCode)
  }
}

class LocationPermissionException : LocationManagerException() {
  override fun hasSolution() = true

  override fun startActivityForSolution(activity: Activity, requestCode: Int) {
    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestCode)
  }
}
