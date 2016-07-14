package com.bisca.taximeter.data.repository

import com.bisca.taximeter.data.model.Ride
import java.util.Date

class RideRepositoryImpl : RideRepository {

  val baseFare = 4.5
  val meterFare = 0.00275
  val secondFare = 0.01333

  override fun calculateRide(startedDate: Date, meters: Float, idleSeconds: Long): Ride {
    val taximeter = baseFare + (meterFare.times(meters)) + (idleSeconds.times(secondFare))

    return Ride(
        taximeter,
        meters,
        idleSeconds,
        baseFare,
        (Date().time - startedDate.time) / 1000
    )
  }

}
