package com.bisca.taximeter.view.ui.activity

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.widget.ImageView
import android.widget.TextView
import com.bisca.taximeter.R
import com.bisca.taximeter.di.component.DaggerMetricsComponent
import com.bisca.taximeter.extensions.defaultLocale
import com.bisca.taximeter.extensions.getComponent
import com.bisca.taximeter.view.ui.LocationManager
import com.bisca.taximeter.view.ui.activity.base.BaseActivity
import com.bisca.taximeter.view.ui.service.MetricsService
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MetricsActivity : BaseActivity(), ServiceConnection {

  companion object {
    const val REQUEST_LOCATION_SOLUTION = 1
    val TAXIMETER_INTERVAL = 10L to TimeUnit.SECONDS
  }

  @Inject
  lateinit var locationManager: LocationManager

  val textStatus by lazy { findViewById(R.id.textStatus) as TextView }
  val textTime by lazy { findViewById(R.id.textTime) as TextView }
  val textSpeed by lazy { findViewById(R.id.textSpeed) as TextView }
  val textValue by lazy { findViewById(R.id.textValue) as TextView }
  val buttonAction by lazy { findViewById(R.id.buttonAction) as ImageView }


  var metricsBinder: MetricsService.Binder? = null
  var compositeSubscription: CompositeSubscription = CompositeSubscription()

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
      displayState(it.getState())
      if (it.isRunningRide()) {
        connectToTaximeterStream(it)
        connectToSpeedStream(it)
        connectToTimeStream(it)
      }
    }
  }

  private fun displayState(state: MetricsService.State) {
    when (state) {
      MetricsService.State.FOR_HIRE -> {
        unregisterService()

        textStatus.text = "DISPONIVEL"
        textStatus.setTextColor(ContextCompat.getColor(this, R.color.greenLed))

        textSpeed.text = "0"
        textTime.text = "00:00"
        textValue.text = "0.00"
      }
      MetricsService.State.HIRED -> {
        textStatus.text = "OCUPADO"
        textStatus.setTextColor(ContextCompat.getColor(this, R.color.redLed))
      }
      MetricsService.State.STOPPED -> {
        textStatus.text = "PARADO"
        textStatus.setTextColor(ContextCompat.getColor(this, R.color.redLed))
      }
    }
  }

  private fun connectToSpeedStream(binder: MetricsService.Binder) {
    compositeSubscription.add(
        binder.getSpeedStream()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { kilometersPerHour ->
              textSpeed.text = String.format("%d", kilometersPerHour.toInt())
            }
    )
  }

  private fun connectToTimeStream(binder: MetricsService.Binder) {
    val format = NumberFormat.getInstance()
    format.minimumIntegerDigits = 2

    compositeSubscription.add(
        binder.getTimeStream()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { millisecondsPassed ->
              val seconds = millisecondsPassed.div(1000)
              val minutes = seconds.div(60)
              val hours = minutes.div(60)

              textTime.text = String.format("%s:%s", format.format(hours), format.format(minutes))
            }
    )
  }


  private fun connectToTaximeterStream(binder: MetricsService.Binder) {
    val (interval, timeUnit) = TAXIMETER_INTERVAL
    compositeSubscription.add(
        binder.getTaximeterStream(interval, timeUnit)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { ride ->
              textValue.text = String.format("%.2f", ride.taximeter)
            }
    )
  }

  override fun onServiceDisconnected(componentName: ComponentName?) {

  }

  private fun initActivity() {
    val drawable = DrawableCompat.wrap(buttonAction.drawable).mutate()
    DrawableCompat.setTint(drawable, ContextCompat.getColor(this, R.color.greenLed))

    buttonAction.setOnClickListener {
      metricsBinder.let {
        if (it != null && it.isRunningRide()) {
          displayState(it.nextState())
        } else {
          connectToLocations()
        }
      }
    }
  }

  private fun connectToLocations() {
    compositeSubscription.add(
        locationManager.stream().first().subscribe(
            { location ->
              startMetricsService()
            },
            { error ->
              if (error is LocationManager.LocationManagerException && error.hasSolution()) {
                error.startActivityForSolution(this, REQUEST_LOCATION_SOLUTION)
              }
            }
        )
    )
  }

  private fun startMetricsService() {
    val intent = MetricsService.getIntent(this)

    startService(intent)

    bindService(intent, this, 0)
  }

  private fun unregisterService() {
    compositeSubscription.clear()
    metricsBinder?.let {
      unbindService(this)
      metricsBinder = null
    }
  }

}
