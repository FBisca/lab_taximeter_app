package com.bisca.taximeter.data.model

class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val elapsedTime: Long,
    val speed: Float,
    val accuracy: Float,
    val distanceMoved: Double
) {
  override fun toString(): String {
    return "lat:$latitude, lon:$longitude, et:$elapsedTime, speed:$speed, acc:$accuracy, dist:$distanceMoved"
  }
}

