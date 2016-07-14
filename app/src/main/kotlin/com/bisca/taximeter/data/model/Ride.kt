package com.bisca.taximeter.data.model

import android.os.Parcel
import android.os.Parcelable

class Ride(
    val taximeter: Double,
    val totalMeters: Float,
    val idleSeconds: Long,
    val baseFare: Double,
    val durationInSeconds: Long
) : Parcelable {

  constructor(parcel: Parcel)
  : this(
      parcel.readDouble(),
      parcel.readFloat(),
      parcel.readLong(),
      parcel.readDouble(),
      parcel.readLong()
  )

  override fun writeToParcel(dest: Parcel?, flags: Int) {
    dest?.writeDouble(taximeter)
    dest?.writeFloat(totalMeters)
    dest?.writeLong(idleSeconds)
    dest?.writeDouble(baseFare)
    dest?.writeLong(durationInSeconds)
  }

  override fun describeContents() = 0

  companion object {
    @JvmField
    val CREATOR = object : Parcelable.Creator<Ride> {
      override fun createFromParcel(parcel: Parcel) = Ride(parcel)

      override fun newArray(size: Int): Array<out Ride?> {
        return arrayOfNulls<Ride>(size)
      }
    }
  }
}
