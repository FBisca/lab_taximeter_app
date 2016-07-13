package com.bisca.taximeter.view.ui.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import javax.inject.Inject

class MetricsService : Service(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

  companion object {
    fun getIntent(context: Context): Intent {
      return Intent(context, MetricsService::class.java)
    }
  }

  val binder = Binder()
  val registeredCallbacks = mutableListOf<Callback>()
  var state = State.NONE

  @Inject
  lateinit var googleApiClient: GoogleApiClient

  override fun onCreate() {
    super.onCreate()
    googleApiClient = initGoogleApiClient()
  }

  override fun onBind(intent: Intent?) = binder

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (!googleApiClient.isConnected && !googleApiClient.isConnecting) {
      googleApiClient.connect()
    } else {
      requestLocations()
    }

    showRunningNotification()
    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    googleApiClient.disconnect()

    removeRunningNotification()
  }

  override fun onConnected(connectionHint: Bundle?) {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      registeredCallbacks.forEach {
        it.onLocationPermissionNeeded()
      }
    } else {
      requestLocations()
    }
  }

  override fun onConnectionFailed(result: ConnectionResult) {
    registeredCallbacks.forEach {
      it.onPlayServicesConnectionFailed(result)
    }
  }

  override fun onConnectionSuspended(cause: Int) {
    registeredCallbacks.forEach {
      it.onPlayServicesConnectionSuspended(cause)
    }
  }

  override fun onLocationChanged(location: Location?) {
    location?.let {
      if (location.speed > 0) {
        state = State.MOVING

      } else {
        state = State.STOPPED
      }
    }
  }

  private fun initGoogleApiClient(): GoogleApiClient {
    return GoogleApiClient.Builder(this)
        .addApi(LocationServices.API)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .build()
  }

  private fun requestLocations() {
    LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this)

    val locationRequest = LocationRequest.create()
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        .setInterval(5000)
        .setSmallestDisplacement(10f)
    LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this)
  }

  private fun showRunningNotification() {
    val notification = NotificationCompat.Builder(this)
        .setAutoCancel(false)
        .setContentTitle("Taximeter Running")
        .build()

    startForeground(1, notification)
  }

  private fun removeRunningNotification() {
    stopForeground(true)
  }

  inner class Binder : android.os.Binder() {
    fun registerForCallbacks(callback: Callback) {
      if (!registeredCallbacks.contains(callback)) {
        registeredCallbacks.add(callback)
      }
    }

    fun unregisterForCallbacks(callback: Callback) {
      registeredCallbacks.remove(callback)
    }
  }

  interface Callback {
    fun onLocationPermissionNeeded()
    fun onPlayServicesConnectionSuspended(cause: Int)
    fun onPlayServicesConnectionFailed(result: ConnectionResult)
  }

  enum class State {
    NONE, STOPPED, MOVING
  }
}
