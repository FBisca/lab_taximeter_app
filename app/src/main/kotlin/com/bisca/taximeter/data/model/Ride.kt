package com.bisca.taximeter.data.model

import android.os.Parcel
import android.os.Parcelable

class Ride(
    var state: RideState
) : Parcelable {

  var taximeter: Float = 0f
  var totalMeters: Float = 0f
  var idleSeconds: Long = 0L
  var baseFare: Float = 0f
  var durationInSeconds: Long = 0L

  constructor(parcel: Parcel)
  : this(parcel.readSerializable() as RideState) {
    taximeter = parcel.readFloat()
    totalMeters = parcel.readFloat()
    idleSeconds = parcel.readLong()
    baseFare = parcel.readFloat()
    durationInSeconds = parcel.readLong()
  }

  override fun writeToParcel(dest: Parcel?, flags: Int) {
    dest?.writeSerializable(state)
    dest?.writeFloat(taximeter)
    dest?.writeFloat(totalMeters)
    dest?.writeLong(idleSeconds)
    dest?.writeFloat(baseFare)
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

enum class RideState {
  FOR_HIRE, HIRED, STOPPED
}
