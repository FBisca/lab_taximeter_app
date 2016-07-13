package com.bisca.taximeter.extensions

import android.app.Application
import android.os.Bundle
import com.bisca.taximeter.di.component.ApplicationComponent
import com.bisca.taximeter.view.App
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient

fun Application.getComponent(): ApplicationComponent {
  return (this as App).getComponent()
}

fun GoogleApiClient.registerConnectionCallbacks(
    onConnected: ((Bundle?) -> Unit)?,
    onConnectionSuspended: ((Int) -> Unit)? = null
): GoogleApiClient.ConnectionCallbacks {

  val callback = object : GoogleApiClient.ConnectionCallbacks {
    override fun onConnected(connectionHint: Bundle?) {
      onConnected?.invoke(connectionHint)
    }

    override fun onConnectionSuspended(cause: Int) {
      onConnectionSuspended?.invoke(cause)
    }
  }

  registerConnectionCallbacks(callback)

  return callback
}

fun GoogleApiClient.registerConnectionFailedCallback(
    connectionFailed: (ConnectionResult) -> Unit
): GoogleApiClient.OnConnectionFailedListener {

  val callback = GoogleApiClient.OnConnectionFailedListener { result ->
    connectionFailed.invoke(result)
  }

  registerConnectionFailedListener(callback)

  return callback
}
