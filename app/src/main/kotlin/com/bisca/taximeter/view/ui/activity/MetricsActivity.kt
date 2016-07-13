package com.bisca.taximeter.view.ui.activity

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.widget.Button
import android.widget.TextView
import com.bisca.taximeter.R
import com.bisca.taximeter.di.component.DaggerMetricsComponent
import com.bisca.taximeter.extensions.getComponent
import com.bisca.taximeter.view.ui.activity.base.BaseActivity
import com.bisca.taximeter.view.ui.service.MetricsService
import com.bisca.taximeter.extensions.registerConnectionCallbacks
import com.bisca.taximeter.extensions.registerConnectionFailedCallback
import com.google.android.gms.common.api.GoogleApiClient
import javax.inject.Inject

class MetricsActivity : BaseActivity(), ServiceConnection {

  companion object {
    const val REQUEST_FINE_LOCATION = 1
    const val REQUEST_PLAY_SERVICES_SOLUTION = 2
  }

  @Inject
  lateinit var googleApiClient: GoogleApiClient

  val labelStatus by lazy { findViewById(R.id.labelStatus) as TextView }
  val buttonStart by lazy { findViewById(R.id.buttonStart) as Button }

  var googleApiConnectionCallback: GoogleApiClient.ConnectionCallbacks? = null
  var googleApiConnectionFailedCallback: GoogleApiClient.OnConnectionFailedListener? = null
  var metricsBinder: MetricsService.Binder? = null


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_metrics)

    initInjection()
    initActivity()
    initGoogleApi()
  }

  private fun initGoogleApi() {
    googleApiConnectionCallback = googleApiClient.registerConnectionCallbacks(
        { connectionHint ->
          startMetricsService()
        }
    )

    googleApiConnectionFailedCallback = googleApiClient.registerConnectionFailedCallback {

    }
  }

  private fun releaseGoogleApi() {
    googleApiConnectionCallback?.let { googleApiClient.unregisterConnectionCallbacks(it) }
    googleApiConnectionFailedCallback?.let { googleApiClient.unregisterConnectionFailedListener(it) }
  }

  private fun initInjection() {
    DaggerMetricsComponent.builder()
        .applicationComponent(application.getComponent())
        .build()
        .inject(this)
  }

  override fun onDestroy() {
    super.onDestroy()
    unregisterService()
    releaseGoogleApi()
  }

  private fun unregisterService() {
    metricsBinder?.let {
      unbindService(this)
    }
  }

  override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
    metricsBinder = binder as MetricsService.Binder?
  }

  override fun onServiceDisconnected(componentName: ComponentName?) {

  }

  private fun initActivity() {
    bindService(MetricsService.getIntent(this), this, 0)

    buttonStart.setOnClickListener {
      checkMetricsRequirements()
    }
  }

  private fun checkMetricsRequirements() {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_FINE_LOCATION)
    } else if (!googleApiClient.isConnected) {
      googleApiClient.connect()
    } else {
      startMetricsService()
    }
  }

  private fun startMetricsService() {
    val intent = MetricsService.getIntent(this)

    bindService(intent, this, 0)
    startService(intent)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == Activity.RESULT_OK) {
      if (requestCode == REQUEST_FINE_LOCATION || requestCode == REQUEST_PLAY_SERVICES_SOLUTION) {
        startMetricsService()
      }
    }
  }

}
