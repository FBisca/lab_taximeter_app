package com.bisca.taximeter.view.ui.activity

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.bisca.taximeter.R
import com.bisca.taximeter.data.model.RideState
import com.bisca.taximeter.di.component.DaggerMetricsComponent
import com.bisca.taximeter.extensions.getComponent
import com.bisca.taximeter.view.ui.activity.base.BaseActivity
import com.bisca.taximeter.view.ui.location.LocationManager
import com.bisca.taximeter.view.ui.location.LocationManagerException
import com.bisca.taximeter.view.ui.service.MetricsService
import com.bisca.taximeter.view.ui.widget.SignalView
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import java.text.NumberFormat
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MetricsActivity : BaseActivity(), ServiceConnection {

  companion object {
    const val REQUEST_LOCATION_SOLUTION = 1
    val TAXIMETER_INTERVAL = 5L to TimeUnit.SECONDS
  }

  @Inject
  lateinit var locationManager: LocationManager

  val signalView by lazy { findViewById(R.id.signalView) as SignalView }
  val textStatus by lazy { findViewById(R.id.textStatus) as TextView }
  val textTime by lazy { findViewById(R.id.textTime) as TextView }
  val textSpeed by lazy { findViewById(R.id.textSpeed) as TextView }
  val textValue by lazy { findViewById(R.id.textValue) as TextView }
  val buttonAction by lazy { findViewById(R.id.buttonAction) as ImageView }


  var metricsBinder: MetricsService.Binder? = null
  var serviceBound = false
  var compositeSubscription: CompositeSubscription = CompositeSubscription()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.setFlags(
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
    )
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
    serviceBound = true
    metricsBinder = binder as MetricsService.Binder?
    metricsBinder?.let {

      compositeSubscription.add(
          it.getRideMetricsStream()
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe { rideMetrics ->
                displaySpeed(rideMetrics.kilometersPerHour.get())
                displayTime(rideMetrics.durationInSeconds.get())
                displaySignal(rideMetrics.accuracy.get())
              }
      )

      val (interval, timeUnit) = TAXIMETER_INTERVAL
      compositeSubscription.add(
          it.getTaximeterStream(interval, timeUnit)
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe { taximeter ->
                displayTaximeter(taximeter)
              }
      )
    }
  }

  override fun onServiceDisconnected(componentName: ComponentName?) {
    metricsBinder = null
    serviceBound = false
  }

  private fun displaySignal(accuracy: Float) {
    val percentage = if (accuracy > 200 || accuracy <= 0) {
      20f
    } else {
      (100 * (200 - accuracy)) / 200
    }
    signalView.setSignalPercentage(percentage.toInt())
  }

  private fun displaySpeed(kilometersPerHour: Float) {
    textSpeed.text = String.format("%d", kilometersPerHour.toInt())
  }

  private fun displayState(state: RideState) {
    when (state) {
      RideState.FOR_HIRE -> {
        textStatus.text = "DISPONIVEL"
        textStatus.setTextColor(ContextCompat.getColor(this, R.color.greenLed))

        textSpeed.text = "0"
        textTime.text = "00:00"
        textValue.text = "0.00"
      }

      RideState.HIRED -> {
        textStatus.text = "OCUPADO"
        textStatus.setTextColor(ContextCompat.getColor(this, R.color.redLed))
      }

      RideState.STOPPED -> {
        textStatus.text = "PARADO"
        textStatus.setTextColor(ContextCompat.getColor(this, R.color.redLed))
      }
    }
  }

  private fun displayTime(seconds: Long) {
    val format = NumberFormat.getInstance()
    format.minimumIntegerDigits = 2

    val minutes = (seconds / 60) % 60
    val hours = (seconds / 60) / 60
    if (hours == 0L) {
      val currentSeconds = seconds % 60
      textTime.text = String.format("%s:%s", format.format(minutes), format.format(currentSeconds))
    } else {
      textTime.text = String.format("%s:%s", format.format(hours), format.format(minutes))
    }
  }

  private fun displayTaximeter(taximeter: Float) {
    textValue.text = String.format("%.2f", taximeter)
  }

  private fun initActivity() {
    val drawable = DrawableCompat.wrap(buttonAction.drawable).mutate()
    DrawableCompat.setTint(drawable, ContextCompat.getColor(this, R.color.greenLed))

    buttonAction.setOnClickListener {
      buttonActionClicked()
    }
  }

  private fun buttonActionClicked() {
    metricsBinder.let {
      if (it == null) {
        connectToLocations()
      } else {
        val state = it.getState()

        if (state == RideState.HIRED) {
          it.stop()
          displayState(RideState.STOPPED)
        }

        if (state == RideState.STOPPED) {
          it.finish()
          displayState(RideState.FOR_HIRE)
          unsubscribeAll()
        }
      }
    }
  }

  private fun connectToLocations() {
    compositeSubscription.add(
        locationManager.resetIfHasErrors().get()
            .first()
            .subscribe(
                { location ->
                  startMetricsService()
                },
                { error ->
                  if (error is LocationManagerException && error.hasSolution()) {
                    error.startActivityForSolution(this, REQUEST_LOCATION_SOLUTION)
                  }
                }
            )
    )
  }

  private fun startMetricsService() {
    val intent = MetricsService.getIntent(this)

    displayState(RideState.HIRED)

    startService(intent)
    bindService(intent, this, 0)
  }

  private fun unregisterService() {
    unsubscribeAll()
    metricsBinder = null
    if (serviceBound) {
      unbindService(this)
    }
  }

  private fun unsubscribeAll() {
    compositeSubscription.clear()
  }

}
