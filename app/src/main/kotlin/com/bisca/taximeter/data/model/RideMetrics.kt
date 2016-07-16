package com.bisca.taximeter.data.model

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
    kilometersPerHour.set(speed * 3.6f)
  }

  fun appendRoute(userLocation: UserLocation) {
    route.add(userLocation)
  }

  fun appendSecond() {
    durationInSeconds.incrementAndGet()
  }
}