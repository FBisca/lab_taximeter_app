package com.bisca.taximeter.view.ui.activity

import android.Manifest
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
import com.bisca.taximeter.view.ui.activity.base.BaseActivity
import com.bisca.taximeter.view.ui.service.MetricsService

class MetricsActivity : BaseActivity(), ServiceConnection {

  companion object {
    const val REQUEST_FINE_LOCATION = 1
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
    metricsBinder?.let { unbindService(this) }
  }

  override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
    metricsBinder = binder as MetricsService.Binder?
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
    if (checkPermission()) {
      startService(MetricsService.getIntent(this))
    }
  }

  private fun checkPermission(): Boolean {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_FINE_LOCATION)
      return false
    }

    return true
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_FINE_LOCATION && resultCode == RESULT_OK) {
      startMetricsService()
    }
  }


}
