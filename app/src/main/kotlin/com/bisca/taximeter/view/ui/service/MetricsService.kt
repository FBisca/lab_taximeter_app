package com.bisca.taximeter.view.ui.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.SystemClock
import android.support.v4.app.NotificationCompat
import com.bisca.taximeter.data.logger.Logger
import com.bisca.taximeter.data.model.RideMetrics
import com.bisca.taximeter.data.model.RideState
import com.bisca.taximeter.data.model.UserLocation
import com.bisca.taximeter.data.repository.RideRepository
import com.bisca.taximeter.di.component.DaggerMetricsComponent
import com.bisca.taximeter.extensions.calculateDistance
import com.bisca.taximeter.extensions.getComponent
import com.bisca.taximeter.view.ui.activity.MetricsActivity
import com.bisca.taximeter.view.ui.location.LocationManager
import rx.Observable
import rx.Subscription
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.concurrent.scheduleAtFixedRate

class MetricsService : Service() {

  companion object {
    val TAG: String = MetricsService::class.java.simpleName

    fun getIntent(context: Context): Intent {
      return Intent(context, MetricsService::class.java)
    }
  }

  val binder = Binder()

  @Inject
  lateinit var rideRepository: RideRepository

  @Inject
  lateinit var locationManager: LocationManager

  val rideMetrics = RideMetrics()
  val timer = Timer()
  var locationsSubscription: Subscription? = null

  override fun onCreate() {
    super.onCreate()
    initInjection()
  }

  override fun onBind(intent: Intent?) = binder

  override fun onUnbind(intent: Intent?): Boolean {
    return super.onUnbind(intent)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (rideMetrics.getState() == RideState.FOR_HIRE) {

      showRunningNotification()
      startListeningForLocations()
      startListeningForTimeTicks()

      rideMetrics.hired()
    }

    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    removeRunningNotification()
    stopListeningForLocations()
    stopListeningForTimeTicks()
  }

  private fun initInjection() {
    DaggerMetricsComponent.builder()
        .applicationComponent(application.getComponent())
        .build()
        .inject(this)
  }

  private fun startListeningForLocations() {
    locationsSubscription = locationManager.get()
        .subscribe { location ->
          locationReceived(location)
        }
  }

  private fun stopListeningForLocations() {
    locationsSubscription?.unsubscribe()
  }

  private fun startListeningForTimeTicks() {
    timer.scheduleAtFixedRate(1000, 1000, {
      rideMetrics.appendSecond()
      if (rideMetrics.hasBecomeIdle()) {
        rideMetrics.appendIdleSecond()
      }
    })
  }

  private fun stopListeningForTimeTicks() {
    timer.cancel()
  }

  private fun locationReceived(location: Location) {
    Logger.debug(TAG, "Position Received ${location.toString()}")

    val userLocation = convertToUserLocation(location)

    Logger.debug(TAG, "Position Converted ${userLocation.toString()}")

    if (filterLocation(userLocation)) {
      if (checkIfUserIsMoving(userLocation)) {
        userMoved(userLocation)
      } else {
        userStopped()
      }

      rideMetrics.updateSpeed(userLocation.speed)
      rideMetrics.appendRoute(userLocation)
      rideMetrics.appendRecentLocation(userLocation)
    }

    rideMetrics.updateAccuracy(userLocation.accuracy)
  }

  private fun convertToUserLocation(location: Location): UserLocation {
    val lastRecentLocation = rideMetrics.recentLocations.lastOrNull()

    val distance = calculateDistance(location, lastRecentLocation)
    val speed = calculateSpeed(location, distance, lastRecentLocation)

    return UserLocation(
        location.latitude,
        location.longitude,
        SystemClock.elapsedRealtime(),
        speed,
        location.accuracy,
        distance
    )
  }

  private fun calculateSpeed(location: Location, distance: Double, lastUserLocation: UserLocation?): Float {
    if (location.hasSpeed() && location.speed > 1) {
      return location.speed
    } else {
      if (lastUserLocation == null) return 0f

      val elapsedTime = SystemClock.elapsedRealtime() - lastUserLocation.elapsedTime
      val elapsedSeconds = elapsedTime / 1000

      if (elapsedSeconds > 0) {
        return (distance / elapsedSeconds).toFloat()
      } else {
        return 0f
      }
    }
  }

  private fun calculateDistance(location: Location, lastUserLocation: UserLocation?): Double {
    return when (lastUserLocation) {
      null -> 0.0
      else -> location.calculateDistance(lastUserLocation.latitude, lastUserLocation.longitude)
    }
  }

  private fun checkIfUserIsMoving(userLocation: UserLocation): Boolean {
    if (userLocation.speed > 1) {
      return true
    } else {
      return false
    }
  }

  private fun userStopped() {
    if (!rideMetrics.hasBecomeIdle()) {
      Logger.debug(TAG, "User is idle")
      rideMetrics.startedIdle()
    }
  }

  private fun userMoved(userLocation: UserLocation) {
    Logger.debug(TAG, "User is moving ${userLocation.speed}m/s")

    if (rideMetrics.hasBecomeIdle()) {
      rideMetrics.stopIdle()
    }

    rideMetrics.computeDistance(userLocation.distanceMoved.toFloat())
  }


  private fun filterLocation(location: UserLocation): Boolean {
    if (rideMetrics.route.isEmpty()) {
      return true
    }

    if (location.distanceMoved in 5 .. 100) {
      return true
    }

    val elapsed = location.elapsedTime - rideMetrics.route.last().elapsedTime
    if (elapsed > 15000 && location.accuracy < 100) {
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

    fun getState() = rideMetrics.getState()

    fun stop() {
      rideMetrics.stopped()
      stopListeningForLocations()
      stopListeningForTimeTicks()
    }

    fun finish() {
      stopSelf()
    }

    fun getRideMetricsStream(): Observable<RideMetrics> {
      return Observable.interval(0L, 1L, TimeUnit.SECONDS)
          .onBackpressureLatest()
          .map { rideMetrics }
    }

    fun getTaximeterStream(interval: Long, timeUnit: TimeUnit): Observable<Float> {
      return Observable.interval(0L, interval, timeUnit)
          .map {
            rideRepository.calculateTaximeter(
                rideMetrics.meters.get(),
                rideMetrics.idleSeconds.get()
            )
          }

    }
  }


}
