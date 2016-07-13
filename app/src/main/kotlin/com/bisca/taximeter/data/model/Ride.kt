package com.bisca.taximeter.data.model

import android.os.Parcel
import android.os.Parcelable

class Ride(
    val taximeter: Double,
    val totalMeters: Double,
    val standingMinutes: Double,
    val baseFare: Double,
    val durationInSeconds: Long
) : Parcelable {

  constructor(parcel: Parcel)
  : this(
      parcel.readDouble(),
      parcel.readDouble(),
      parcel.readDouble(),
      parcel.readDouble(),
      parcel.readLong()
  )

  override fun writeToParcel(dest: Parcel?, flags: Int) {
    dest?.writeDouble(taximeter)
    dest?.writeDouble(totalMeters)
    dest?.writeDouble(standingMinutes)
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