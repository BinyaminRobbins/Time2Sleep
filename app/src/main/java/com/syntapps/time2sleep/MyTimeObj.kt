package com.syntapps.time2sleep

import android.os.Parcel
import android.os.Parcelable


class MyTimeObj : Parcelable {
    var timeSetFor: Int = 0
    var timeSetAt: Int = 0
    var timePassed: Int = 0


    constructor(
        timeSetForInMins: Int,
        timeSetAtInMins: Int,
        timePassedInMins: Int
    ) {
        timeSetFor = timeSetForInMins
        timeSetAt = timeSetAtInMins
        timePassed = timePassedInMins
    }

    constructor(parcel: Parcel) {
        timeSetFor = parcel.readInt()
        timeSetAt = parcel.readInt()
        timePassed = parcel.readInt()
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.let {
            it.writeInt(timeSetFor)
            it.writeInt(timeSetAt)
            it.writeInt(timePassed)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MyTimeObj> {
        override fun createFromParcel(parcel: Parcel): MyTimeObj {
            return MyTimeObj(parcel)
        }

        override fun newArray(size: Int): Array<MyTimeObj?> {
            return arrayOfNulls(size)
        }
    }


    fun oneMinutePassed() {
        timePassed += 1
    }

    fun getTimeDifference(): Int {
        //the spread between when the timer was set and when it was set to go off
        return (timeSetFor - timeSetAt)
    }


}