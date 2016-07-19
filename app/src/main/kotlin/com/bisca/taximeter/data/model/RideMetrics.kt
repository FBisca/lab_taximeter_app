package com.bisca.taximeter.data.model

import android.os.SystemClock
import com.bisca.taximeter.data.logger.Logger
import com.bisca.taximeter.view.ui.service.MetricsService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class RideMetrics() {
  var rideState = AtomicReference<RideState>(RideState.FOR_HIRE)
  val meters = AtomicReference<Float>(0F)
  val kilometersPerHour = AtomicReference<Float>(0F)
  val accuracy = AtomicReference<Float>(0F)
  val idleSeconds = AtomicLong(0)
  val durationInSeconds = AtomicLong(0)
  val recentLocations = mutableListOf<UserLocation>()
  val route = mutableListOf<UserLocation>()

  private val becomeIdle = AtomicBoolean()

  fun getState(): RideState = rideState.get()

  fun hired() {
    rideState.set(RideState.HIRED)
  }

  fun stopped() {
    rideState.set(RideState.STOPPED)
  }

  fun startedIdle() {
    becomeIdle.set(true)
  }

  fun stopIdle() {
    becomeIdle.set(false)
  }

  fun hasBecomeIdle() = becomeIdle.get()

  fun computeDistance(distanceMoved: Float) {
    val currentMeters = meters.get()

    val computedDistance = distanceMoved.plus(currentMeters)

    meters.set(computedDistance)

    Logger.debug(MetricsService.TAG, "Computed Distance $computedDistance")
  }

  fun appendIdleSecond() {
    val currentIdleSeconds = idleSeconds.incrementAndGet()

    Logger.debug(MetricsService.TAG, "Computing Idle, total $currentIdleSeconds idle seconds")
  }

  fun updateAccuracy(accuracy: Float) {
    this.accuracy.set(accuracy)
  }

  fun updateSpeed(speed: Float) {
    if (speed == 0f) {
      kilometersPerHour.set(speed * 3.6f)
    } else {
      val currentTime = SystemClock.elapsedRealtime()
      val locations = recentLocations.filter {
        it.speed != 0f && (currentTime - it.elapsedTime) < 5000
      }

      if (locations.isEmpty()) {
        kilometersPerHour.set(speed * 3.6f)
      } else {

        val averageSpeed = locations.sumByDouble { it.speed.toDouble() }.div(locations.size)
        kilometersPerHour.set(averageSpeed.toFloat() * 3.6f)
      }
    }

  }

  fun appendRoute(userLocation: UserLocation) {
    if (route.isEmpty() || userLocation.distanceMoved > 10) {
      route.add(userLocation)
    }
  }

  fun appendSecond() {
    durationInSeconds.incrementAndGet()
  }

  fun appendRecentLocation(userLocation: UserLocation) {
    if (recentLocations.size >= 5) {
      recentLocations.removeAt(0)
    }
    recentLocations.add(userLocation)
  }
}
