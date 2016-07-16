package com.bisca.taximeter.data.repository

class RideRepositoryImpl : RideRepository {

  val baseFare = 4.5f
  val kilometerFare = 2.75f
  val hourFare = 33.0f

  override fun calculateTaximeter(meters: Float, idleSeconds: Long): Float {
    val meterFare = toMeterFare(kilometerFare)
    val secondFare = toSecondFare(hourFare)
    
    return baseFare + (meterFare.times(meters)) + (idleSeconds.times(secondFare))
  }

  private fun toSecondFare(hourFare: Float): Float {
    return ((hourFare / 60.0f) / 60.0f)
  }

  private fun toMeterFare(kilometerFare: Float): Float {
    return (kilometerFare / 1000)
  }

}
