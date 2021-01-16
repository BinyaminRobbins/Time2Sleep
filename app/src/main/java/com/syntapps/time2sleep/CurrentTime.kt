package com.syntapps.time2sleep

import java.util.*

class CurrentTime {
    val currentTimeInMinutes: Int
        get() {
            val calInst = GregorianCalendar(TimeZone.getDefault())
            val hour =
                calInst[GregorianCalendar.HOUR_OF_DAY]   //HOUR_OF_DAY denotes the hour of the day per 24-hr clock. ex: 10:05 PM = 22
            val minute =
                calInst[GregorianCalendar.MINUTE]      //HOUR denotes the hour per 12-hour AM-PM clock. ex: 10:05 PM = 10
            return (hour * 60 + minute)
        }
}