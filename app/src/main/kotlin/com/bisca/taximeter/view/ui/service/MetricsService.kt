package com.bisca.taximeter.view.ui.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices

class MetricsService : Service(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

  companion object {
    fun getIntent(context: Context): Intent {
      return Intent(context, MetricsService::class.java)
    }
  }

  val binder = Binder()

  lateinit var googleApiClient: GoogleApiClient

  override fun onCreate() {
    super.onCreate()
    googleApiClient = initGoogleApiClient()
    googleApiClient.connect()
  }

  override fun onDestroy() {
    super.onDestroy()
    googleApiClient.disconnect()
  }

  override fun onBind(intent: Intent?) = binder

  override fun onConnectionFailed(result: ConnectionResult) {
  }

  override fun onConnected(connectionHint: Bundle?) {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
      val request = LocationRequest.create()
          .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
          .setInterval(5000)
          .setSmallestDisplacement(10f)

      LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, request, this)
    }
  }

  override fun onConnectionSuspended(cause: Int) {

  }

  override fun onLocationChanged(location: Location?) {

  }

  private fun initGoogleApiClient() : GoogleApiClient {
    return GoogleApiClient.Builder(this)
        .addApi(LocationServices.API)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .build()
  }

  inner class Binder : android.os.Binder()
}
