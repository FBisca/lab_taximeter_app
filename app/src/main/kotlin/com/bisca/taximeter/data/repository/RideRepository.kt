package com.bisca.taximeter.data.repository

import com.bisca.taximeter.data.model.Ride
import java.util.Date

interface RideRepository {
  fun calculateRide(startedDate: Date, meters: Float, idleSeconds: Long): Ride
}
