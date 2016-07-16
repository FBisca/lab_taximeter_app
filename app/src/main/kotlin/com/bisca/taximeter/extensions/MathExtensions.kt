package com.bisca.taximeter.extensions

import android.location.Location

fun Location.calculateDistance(otherLatitude: Double, otherLongitude: Double): Double {
  val earthRadius = 6371e3 // meters

  val φ1 = this.latitude.toRadians()
  val φ2 = otherLatitude.toRadians()
  val Δφ = (otherLatitude - this.latitude).toRadians()

  val Δλ = (otherLongitude - this.longitude).toRadians()

  val chord = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
      Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ / 2) * Math.sin(Δλ / 2)

  val angularDistance = 2 * Math.atan2(Math.sqrt(chord), Math.sqrt(1 - chord))

  return earthRadius * angularDistance
}

fun Double.toRadians() = Math.toRadians(this)