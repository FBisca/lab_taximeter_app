package com.bisca.taximeter.view.ui.service

import android.app.PendingIntent
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
import com.bisca.taximeter.view.ui.activity.MetricsActivity
import rx.Observable
import rx.Subscription
import rx.subjects.PublishSubject
import java.util.*
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

  val lastPoint = Location(INITIAL_POINT)
  val rideMetrics = RideMetrics()

  val speedSubject: PublishSubject<Float> = PublishSubject.create<Float>()

  val handler = Handler()

  var currentState = State.FOR_HIRE

  var initialDate: Long = 0L

  var locationsSubscription: Subscription? = null

  override fun onCreate() {
    super.onCreate()
    initInjection()
  }

  override fun onBind(intent: Intent?) = binder

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (currentState == State.FOR_HIRE) {
      initialDate = System.currentTimeMillis()

      showRunningNotification()
      startListeningForLocations()

      currentState = State.HIRED
    }

    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    handler.removeCallbacksAndMessages(null)
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
    if (filterLocation(location)) {
      if (location.hasSpeed() && location.speed > 0f) {
        userMoved(location)
      } else {
        userStopped()
      }
    }

    lastPoint.set(location)
  }

  private fun startIdleChecker(expectedSecondsToNextPosition: Int) {
    Log.d(TAG, "Expected Position in $expectedSecondsToNextPosition seconds")

    handler.removeCallbacksAndMessages(null)
    handler.postDelayed({
      if (!rideMetrics.hasBecomeIdle()) {
        Log.d(TAG, "Idle Checker")
        userStopped()
      }
    }, expectedSecondsToNextPosition * 1000L)
  }

  private fun userStopped() {
    Log.d(TAG, "User is idle")

    rideMetrics.startedIdle(SystemClock.elapsedRealtime())
    speedSubject.onNext(0f)
  }

  private fun userMoved(location: Location) {
    Log.d(TAG, "User is moving ${location.speed}m/s")

    if (rideMetrics.hasBecomeIdle()) {
      computeIdleTime()
      stopIdlingTime()
    }

    computeDistance(location)

    val timeToNextPosition = (locationManager.locationRequest.smallestDisplacement / location.speed) * 4

    startIdleChecker(Math.ceil(timeToNextPosition.toDouble()).toInt())

    val kilometerPerHour = location.speed * 3.6
    speedSubject.onNext(kilometerPerHour.toFloat())
  }

  private fun computeDistance(location: Location) {
    if (lastPoint.provider.equals(INITIAL_POINT)) {
      return
    }

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
    val newIdleSeconds = Math.round(newIdleTime.div(1000.0)) // To Seconds

    val computedIdleSeconds = currentIdleSeconds.plus(newIdleSeconds)
    rideMetrics.idleSeconds.set(computedIdleSeconds)

    Log.d(TAG, "Computing Idle, adding $newIdleSeconds seconds")
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
    val intent = Intent(this, MetricsActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT)

    val notification = NotificationCompat.Builder(this)
        .setContentIntent(pendingIntent)
        .setContentTitle("Taximeter Running")
        .build()

    startForeground(1, notification)
  }

  private fun removeRunningNotification() {
    stopForeground(true)
  }

  inner class Binder : android.os.Binder() {

    fun isRunningRide() = currentState != State.FOR_HIRE

    fun getState() = currentState

    fun nextState(): State {
      return when (currentState) {
        State.HIRED -> {
          stopListeningForLocations()
          currentState = State.STOPPED
          State.STOPPED
        }

        State.STOPPED -> {
          stopSelf()
          State.FOR_HIRE
        }

        else -> currentState

      }
    }

    fun getTaximeterStream(interval: Long, timeUnit: TimeUnit): Observable<Ride> {
      return Observable.interval(0L, interval, timeUnit)
          .onBackpressureLatest()
          .map {
            if (currentState == State.HIRED && rideMetrics.hasBecomeIdle()) {
              computeIdleTime()
            }

            rideRepository.calculateRide(Date(initialDate), rideMetrics.meters.get(), rideMetrics.idleSeconds.get())
          }
    }

    fun getSpeedStream(): Observable<Float> {
      return speedSubject.onBackpressureLatest()
    }

    fun getTimeStream(): Observable<Long> {
      return Observable.interval(0, 60L, TimeUnit.SECONDS)
        .onBackpressureLatest()
        .map {
          System.currentTimeMillis() - initialDate
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

  enum class State {
    FOR_HIRE, HIRED, STOPPED
  }
}
