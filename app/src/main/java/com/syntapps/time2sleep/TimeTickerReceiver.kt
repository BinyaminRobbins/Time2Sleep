package com.syntapps.time2sleep

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlin.math.roundToInt

class TimeTickerReceiver() : BroadcastReceiver() {

    private val TAG = "TimeTickerReceiver"
    private var timeFixToArray: ArrayList<String>? = null
    
    private lateinit var myContext: Context
    private var timeSetInMins: Int = 2319
    private var timeSetDifference: Int = 2319

    constructor (
        MY_CONTEXT: Context,
            TIME_SET_DIFFERENCE: Int,
        TIME_SET_AT_IN_MINS: Int
    ) : this() {
        myContext = MY_CONTEXT
        timeSetInMins = TIME_SET_AT_IN_MINS
        timeSetDifference = TIME_SET_DIFFERENCE

    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_TIME_TICK) {
            Log.i(TAG, "onReceive: 21: TIME_TICK")
            updateTimer(timeSetDifference)
        }
    }

    private fun updateTimer(timeSetDifference: Int) {
        Log.i(TAG, "process: 46: processing...")
        //convert the time diff into minutes ex: 1.5 hrs = 90 mins
        var timeRemainingInHRS: Double =
            (timeSetDifference - getTimePassedInMins(timeSetInMins)).toDouble() / 60

        if (timeRemainingInHRS.toString().length > 4 || timeRemainingInHRS.toString().length == 3) {
            timeRemainingInHRS = (timeRemainingInHRS * 100.0).roundToInt() / 100.0
        }

        timeFixToArray = timeFixToArray(timeRemainingInHRS)

        if (timeFixToArray!![0] == "0" || timeFixToArray!![0] == "00") {
            if (timeFixToArray!![1] == "0" || timeFixToArray!![1] == "00") {
                unregister()
            }
        }
    }

    private fun unregister() {
        try {
            myContext.getSharedPreferences(myContext.getString(R.string.sharedPrefsName), 0)
                .edit().putBoolean("isReceiverRegistered", false).apply()
            myContext.unregisterReceiver(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getTimePassedInMins(timeSetInMins: Int): Int {
        //get the time passed in mins since the timer was set
        return CurrentTime().currentTimeInMinutes - timeSetInMins
    }

    private fun timeFixToArray(timeDiffInHrs: Double): ArrayList<String> {
        val arr: ArrayList<String> =
            timeDiffInHrs.toString().split(".") as ArrayList<String>
        //trim the string to the first 2 chars ie 1333333 becomes 13
        arr[1] = arr[1].take(2)

        if (arr[1].length < 2) {
            //if minutes is single dig i.e 8 minutes the change it to double dig i.e 09 minutes
            arr[1] = ("${arr[1]}0")
        }
        arr[1] =
            ((((arr[1].toDouble() / 100) * 60) * 100.0) / 100.0).roundToInt().toString()

        val iterator = arr.listIterator()
        while (iterator.hasNext()) {
            val oldValue = iterator.next()
            if (oldValue.length == 1) iterator.set("0$oldValue")
        }
        return arr
    }

}