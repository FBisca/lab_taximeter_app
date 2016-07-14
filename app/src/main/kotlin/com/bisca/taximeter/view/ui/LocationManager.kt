package com.bisca.taximeter.view.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResult
import com.google.android.gms.location.LocationSettingsStatusCodes
import rx.Observable
import rx.subjects.BehaviorSubject

class LocationManager(
    val context: Context
) : GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, LocationListener {

  var locationSubject: BehaviorSubject<Location> = BehaviorSubject.create()

  val googleApiClient: GoogleApiClient = GoogleApiClient.Builder(context)
      .addApi(LocationServices.API)
      .addConnectionCallbacks(this)
      .addOnConnectionFailedListener(this)
      .build()

  val locationRequest: LocationRequest = LocationRequest.create()
      .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
      .setInterval(5000)
      .setSmallestDisplacement(10f)

  fun stream(): Observable<Location> {

    if (locationSubject.hasThrowable()) {
      locationSubject = BehaviorSubject.create()
    }

    return locationSubject
        .onBackpressureLatest()
        .doOnSubscribe {
          connectGoogleApi()
        }
        .doOnUnsubscribe {
          disconnect()
        }
  }

  override fun onLocationChanged(location: Location?) {
    location?.let { locationSubject.onNext(location) }
  }

  private fun connectGoogleApi() {
    if (!googleApiClient.isConnected) {
      googleApiClient.connect()
    }
  }

  private fun disconnect() {
    if (!locationSubject.hasObservers()) {
      stopLocationsUpdates()
      disconnectGoogleApi()
    }
  }

  private fun disconnectGoogleApi() {
    if (googleApiClient.isConnected || googleApiClient.isConnecting) {
      googleApiClient.disconnect()
    }
  }

  private fun checkLocationSettings() {
    stopLocationsUpdates()

    val permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    if (permission != PackageManager.PERMISSION_GRANTED) {
      locationSubject.onError(LocationPermissionException())
      return
    }

    val settingsBuilder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
    val pendingIntent = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, settingsBuilder.build())

    pendingIntent.setResultCallback { result ->
      val status = result.status
      when (status.statusCode) {
        LocationSettingsStatusCodes.SUCCESS -> {
          startLocationsUpdates()
        }

        LocationSettingsStatusCodes.RESOLUTION_REQUIRED,
        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
          locationSubject.onError(LocationSettingsException(result))
        }
      }
    }
  }

  private fun startLocationsUpdates() {
    LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this)
  }

  private fun stopLocationsUpdates() {
    if (googleApiClient.isConnected) {
      LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this)
    }
  }

  override fun onConnected(connectionHint: Bundle?) {
    checkLocationSettings()
  }

  override fun onConnectionSuspended(cause: Int) {

  }

  override fun onConnectionFailed(result: ConnectionResult) {
    locationSubject.onError(GoogleApiConnectException(result))
  }

  abstract class LocationManagerException : Throwable() {
    abstract fun hasSolution(): Boolean
    abstract fun startActivityForSolution(activity: Activity, requestCode: Int)
  }

  private class LocationSettingsException(
      val locationSettingsResult: LocationSettingsResult
  ) : LocationManagerException() {

    override fun hasSolution(): Boolean {
      return locationSettingsResult.status.hasResolution()
    }

    override fun startActivityForSolution(activity: Activity, requestCode: Int) {
      locationSettingsResult.status.startResolutionForResult(activity, requestCode)
    }
  }

  private class GoogleApiConnectException(
      val connectionResult: ConnectionResult? = null
  ) : LocationManagerException() {

    override fun hasSolution(): Boolean {
      return connectionResult?.hasResolution() ?: false
    }

    override fun startActivityForSolution(activity: Activity, requestCode: Int) {
      connectionResult?.startResolutionForResult(activity, requestCode)
    }
  }

  private class LocationPermissionException : LocationManagerException() {
    override fun hasSolution() = true

    override fun startActivityForSolution(activity: Activity, requestCode: Int) {
      ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestCode)
    }
  }
}
