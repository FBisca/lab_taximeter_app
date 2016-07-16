package com.bisca.taximeter.data.repository

interface RideRepository {
  fun calculateTaximeter(meters: Float, idleSeconds: Long): Float
}
