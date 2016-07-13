package com.bisca.taximeter.view.ui.activity

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.widget.Button
import android.widget.TextView
import com.bisca.taximeter.R
import com.bisca.taximeter.view.ui.activity.base.BaseActivity
import com.bisca.taximeter.view.ui.service.MetricsService
import com.google.android.gms.common.ConnectionResult

class MetricsActivity : BaseActivity(), ServiceConnection, MetricsService.Callback {

  companion object {
    const val REQUEST_FINE_LOCATION = 1
    const val REQUEST_PLAY_SERVICES_SOLUTION = 2
  }

  val labelStatus by lazy { findViewById(R.id.labelStatus) as TextView }
  val buttonStart by lazy { findViewById(R.id.buttonStart) as Button }

  var metricsBinder: MetricsService.Binder? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_metrics)
    initActivity()
  }

  override fun onDestroy() {
    super.onDestroy()
    unregisterService()
  }

  private fun unregisterService() {
    metricsBinder?.let {
      it.unregisterForCallbacks(this)
      unbindService(this)
    }
  }

  override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
    metricsBinder = binder as MetricsService.Binder?
    metricsBinder?.registerForCallbacks(this)
  }

  override fun onServiceDisconnected(componentName: ComponentName?) {

  }

  private fun initActivity() {
    bindService(MetricsService.getIntent(this), this, 0)

    buttonStart.setOnClickListener {
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

  override fun onLocationPermissionNeeded() {
    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_FINE_LOCATION)
  }

  override fun onPlayServicesConnectionSuspended(cause: Int) {

  }

  override fun onPlayServicesConnectionFailed(result: ConnectionResult) {
    if (result.hasResolution()) {
      result.startResolutionForResult(this, REQUEST_PLAY_SERVICES_SOLUTION)
    }
  }

}
