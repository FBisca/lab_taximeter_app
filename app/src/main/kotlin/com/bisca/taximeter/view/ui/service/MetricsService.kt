package com.bisca.taximeter.view.ui.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Handler
import android.os.SystemClock
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.bisca.taximeter.data.model.Ride
import com.bisca.taximeter.data.repository.RideRepository
import com.bisca.taximeter.di.component.DaggerMetricsComponent
import com.bisca.taximeter.extensions.getComponent
import com.bisca.taximeter.view.ui.LocationManager
import rx.Observable
import rx.Subscription
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

class MetricsService : Service() {

  companion object {
    val TAG: String = MetricsService::class.java.simpleName

    const val INITIAL_POINT = "none"

    fun getIntent(context: Context): Intent {
      return Intent(context, MetricsService::class.java)
    }
  }

  val binder = Binder()

  @Inject
  lateinit var rideRepository: RideRepository

  @Inject
  lateinit var locationManager: LocationManager

  var runningRide: Boolean = false
  val rideMetrics = RideMetrics()
  val lastPoint = Location(INITIAL_POINT)
  val handler = Handler()

  var locationsSubscription: Subscription? = null

  override fun onCreate() {
    super.onCreate()
    initInjection()
  }

  override fun onBind(intent: Intent?) = binder

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (!runningRide) {
      showRunningNotification()
      startListeningForLocations()

      runningRide = true
    }

    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    removeRunningNotification()
    stopListeningForLocations()
  }

  private fun initInjection() {
    DaggerMetricsComponent.builder()
        .applicationComponent(application.getComponent())
        .build()
        .inject(this)
  }

  private fun startListeningForLocations() {
    locationsSubscription = locationManager.stream()
        .retry()
        .subscribe { location ->
          locationReceived(location)
        }
  }

  private fun stopListeningForLocations() {
    locationsSubscription?.unsubscribe()
  }

  private fun locationReceived(location: Location) {
    Log.d(TAG, location.toString())

    if (filterLocation(location)) {
      if (location.hasSpeed() && location.speed > 0f) {
        userMoved(location)
      } else {
        userStopped()
      }
    }

    lastPoint.set(location)
    startHandler()
  }

  private fun startHandler() {
    handler.removeCallbacksAndMessages(null)
    handler.postDelayed({
      Log.d(TAG, "Handler Fired")
      if (!rideMetrics.hasBecomeIdle()) {
        userStopped()
      }
    }, 5000)
  }

  private fun userStopped() {
    Log.d(TAG, "User started idle")

    rideMetrics.startedIdle(SystemClock.elapsedRealtime())
  }

  private fun userMoved(location: Location) {
    Log.d(TAG, "User started moving")

    if (rideMetrics.hasBecomeIdle()) {
      computeIdleTime()
      stopIdlingTime()
    }

    computeDistance(location)
  }

  private fun computeDistance(location: Location) {
    val currentMeters = rideMetrics.meters.get()

    val newDistance = lastPoint.distanceTo(location)
    val computedDistance = newDistance.plus(currentMeters)

    rideMetrics.meters.set(computedDistance)

    Log.d(TAG, "Computed Distance $computedDistance")
  }

  private fun computeIdleTime() {
    val currentIdleSeconds = rideMetrics.idleSeconds.get()

    val currentTime = SystemClock.elapsedRealtime()
    val newIdleTime = currentTime.minus(rideMetrics.idleStartedTime.getAndSet(currentTime))
    val newIdleSeconds = newIdleTime.div(1000) // To Seconds

    val computedIdleSeconds = currentIdleSeconds.plus(newIdleSeconds)
    rideMetrics.idleSeconds.set(currentIdleSeconds)

    Log.d(TAG, "Computed Idle Seconds $computedIdleSeconds")
  }

  private fun stopIdlingTime() {
    rideMetrics.stopIdle()
  }

  private fun filterLocation(location: Location): Boolean {
    if (lastPoint.provider.equals(INITIAL_POINT)) {
      return true
    }

    if (location.distanceTo(lastPoint) < 20) {
      return true
    }

    if (location.hasAccuracy() && location.accuracy < 100) {
      return true
    }

    return false
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

    fun isRunningRide() = runningRide

    fun getTaximeterStream(interval: Long, timeUnit: TimeUnit): Observable<Ride> {
      return Observable.interval(0L, interval, timeUnit)
          .onBackpressureLatest()
          .map {
            if (rideMetrics.hasBecomeIdle()) {
              computeIdleTime()
            }

            rideRepository.calculateRide(Date(), rideMetrics.meters.get(), rideMetrics.idleSeconds.get())
          }
    }
  }

  class RideMetrics() {
    val idleStartedTime = AtomicLong(0)
    val meters = AtomicReference<Float>(0F)
    val idleSeconds = AtomicLong(0)
    private val becomeIdle = AtomicBoolean()

    fun startedIdle(time: Long) {
      becomeIdle.set(true)
      idleStartedTime.set(time)
    }

    fun stopIdle() {
      becomeIdle.set(false)
      idleStartedTime.set(0)
    }

    fun hasBecomeIdle() = becomeIdle.get()

  }
}
