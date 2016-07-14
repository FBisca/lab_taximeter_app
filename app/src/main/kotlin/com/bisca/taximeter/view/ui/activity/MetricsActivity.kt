package com.bisca.taximeter.view.ui.activity

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.TextView
import com.bisca.taximeter.R
import com.bisca.taximeter.di.component.DaggerMetricsComponent
import com.bisca.taximeter.extensions.getComponent
import com.bisca.taximeter.view.ui.LocationManager
import com.bisca.taximeter.view.ui.activity.base.BaseActivity
import com.bisca.taximeter.view.ui.service.MetricsService
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MetricsActivity : BaseActivity(), ServiceConnection {

  companion object {
    const val REQUEST_LOCATION_SOLUTION = 1
    val TAXIMETER_INTERVAL = 10L to TimeUnit.SECONDS
  }

  @Inject
  lateinit var locationManager: LocationManager

  val labelStatus by lazy { findViewById(R.id.labelStatus) as TextView }
  val buttonStart by lazy { findViewById(R.id.buttonStart) as Button }

  var metricsBinder: MetricsService.Binder? = null
  var locationManagerSubscription: Subscription? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_metrics)

    initInjection()
    initActivity()

    bindService(MetricsService.getIntent(this), this, 0)
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
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_LOCATION_SOLUTION) {
      startMetricsService()
    }
  }

  override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
    metricsBinder = binder as MetricsService.Binder?
    metricsBinder?.let {
      if (it.isRunningRide()) {
        connectToTaximeterStream(it)
      }
    }
  }

  private fun connectToTaximeterStream(binder: MetricsService.Binder) {
    val (interval, timeUnit) = TAXIMETER_INTERVAL
    binder.getTaximeterStream(interval, timeUnit)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { ride ->
      labelStatus.text = "Taximeter: ${ride.taximeter}"
    }
  }

  override fun onServiceDisconnected(componentName: ComponentName?) {

  }

  private fun initActivity() {
    buttonStart.setOnClickListener {
     connectToLocations()
    }
  }

  private fun connectToLocations() {
    locationManagerSubscription = locationManager.stream().first().subscribe(
        { location ->
          startMetricsService()
        },
        { error ->
          if (error is LocationManager.LocationManagerException && error.hasSolution()) {
            error.startActivityForSolution(this, REQUEST_LOCATION_SOLUTION)
          }
        }
    )
  }

  private fun startMetricsService() {
    val intent = MetricsService.getIntent(this)

    startService(intent)

    bindService(intent, this, 0)
  }

  private fun unregisterService() {
    locationManagerSubscription?.unsubscribe()
    metricsBinder?.let {
      unbindService(this)
    }
  }

}
