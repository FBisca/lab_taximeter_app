package com.bisca.taximeter.view.ui.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import rx.Observable
import rx.subjects.BehaviorSubject

class LocationManager(val context: Context) {

  private var locationSubject: BehaviorSubject<Location> = BehaviorSubject.create()

  private val callbackHandler = CallbacksHandler()

  private val googleApiClient: GoogleApiClient = GoogleApiClient.Builder(context)
      .addApi(LocationServices.API)
      .addConnectionCallbacks(callbackHandler)
      .addOnConnectionFailedListener(callbackHandler)
      .build()

  private val locationRequest: LocationRequest = LocationRequest.create()
      .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
      .setInterval(1000)
      .setMaxWaitTime(5000)
      .setFastestInterval(1000)

  fun resetIfHasErrors(): LocationManager {
    if (locationSubject.hasThrowable()) {
      locationSubject = BehaviorSubject.create()
    }

    return this
  }

  fun get(): Observable<Location> {
    return locationSubject
        .onBackpressureLatest()
        .doOnSubscribe {
          connectGoogleApi()
        }
        .doOnUnsubscribe {
          disconnect()
        }
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
    LocationServices.FusedLocationApi.requestLocationUpdates(
        googleApiClient,
        locationRequest,
        callbackHandler
    )
  }

  private fun stopLocationsUpdates() {
    if (googleApiClient.isConnected) {
      LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, callbackHandler)
    }
  }

  private inner class CallbacksHandler() : GoogleApiClient.OnConnectionFailedListener,
      GoogleApiClient.ConnectionCallbacks, LocationListener {

    override fun onLocationChanged(location: Location?) {
      location?.let { locationSubject.onNext(location) }
    }

    override fun onConnected(connectionHint: Bundle?) {
      checkLocationSettings()
    }

    override fun onConnectionSuspended(cause: Int) {

    }

    override fun onConnectionFailed(result: ConnectionResult) {
      locationSubject.onError(GoogleApiConnectException(result))
    }
  }
}
