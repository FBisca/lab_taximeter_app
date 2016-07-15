package com.bisca.taximeter.data.repository

import com.bisca.taximeter.data.model.Ride
import java.util.Date

class RideRepositoryImpl : RideRepository {

  val baseFare = 4.5
  val kilometerFare = 2.75
  val hourFare = 33.0

  override fun calculateRide(startedDate: Date, meters: Float, idleSeconds: Long): Ride {
    val meterFare = toMeterFare(kilometerFare)
    val secondFare = toSecondFare(hourFare)
    
    val taximeter = baseFare + (meterFare.times(meters)) + (idleSeconds.times(secondFare))

    return Ride(
        taximeter,
        meters,
        idleSeconds,
        baseFare,
        (Date().time - startedDate.time) / 1000
    )
  }

  private fun toSecondFare(hourFare: Double): Double {
    return ((hourFare / 60.0) / 60.0)
  }

  private fun toMeterFare(kilometerFare: Double): Double {
    return (kilometerFare / 1000)
  }

}
