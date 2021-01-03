package com.syntapps.time2sleep

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.widget.TimePicker
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import es.dmoral.toasty.Toasty
import java.lang.ArithmeticException
import java.util.*
import kotlin.collections.ArrayList

class TimePickerFragment(private val callback: MyCallback, private val myContext: Context) :
    DialogFragment(),
    TimePickerDialog.OnTimeSetListener {

    private val TAG = "TimePickerFragment_log"
    private val METHOD_CALL = "METHOD_CALL"
    private val ARR = "ARRAY"
    var hourOfDaySelected: Int = 0
    var minuteOfDaySelected: Int = 0

    private lateinit var toastHour: String
    private lateinit var toastMinute: String

    private var hourDiff: Int = 0
    private var minuteDiff: Int = 0

    private var timeChangedReceiver: BroadcastReceiver? = null

    private var timeSetAtInMins: Int = 0

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        Log.i(METHOD_CALL, "onTimeSet()")
        this.hourOfDaySelected = hourOfDay
        this.minuteOfDaySelected = minute
        setTimeTxt()

        Toasty.info(
            myContext,
            "New Timer Set for:\n$toastHour hour/s & $toastMinute minute/s",
            Toast.LENGTH_SHORT,
            true
        ).show()

        if (timeChangedReceiver != null) {
            myContext.unregisterReceiver(timeChangedReceiver)
        }
        setReceiver()
    }

    private fun setReceiver() {
        Log.i(METHOD_CALL, "setReceiver()")
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_TIME_TICK)
        timeChangedReceiver = object : BroadcastReceiver() {
            // TODO: 21/12/2020 edit time left text on time change
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (action == Intent.ACTION_TIME_TICK) {
                    Log.i(TAG, "onReceive: received")
                    updateProgressBar()
                    //convert the time diff into minutes ex: 1.5 hrs = 90 mins
                    val timeDiff: Double = ((hourDiff * 60) + minuteDiff).toDouble()
                    Log.i(TAG, "onReceive: timeDiff = $timeDiff")
                    //get the time passed in mins since the timer was set
                    val timePassedInMins = (getCurrentTimeInMinutes() - timeSetAtInMins)
                    Log.i(TAG, "onReceive: timepassedinmins = $timePassedInMins")
                    var num = (timeDiff - timePassedInMins) / 60
                    if(num.toString().length > 4) {
                        num = Math.round(num * 100.0) / 100.0
                    }
                    Log.i(TAG, "onReceive: NUM = $num")
                    val fixedTime = fixTime(num)
                    callback.updateText("${fixedTime[0]}:${fixedTime[1]}")
                }
            }
        }
        myContext.registerReceiver(timeChangedReceiver, filter)
    }

    companion object {
        fun getCurrentTimeInMinutes(): Int {
            val tzone = TimeZone.getDefault()
            val calInst = GregorianCalendar(tzone)
            val hour =
                calInst[GregorianCalendar.HOUR_OF_DAY]   //HOUR_OF_DAY denotes the hour of the day per 24-hr clock. ex: 10:05 PM = 22
            val minute =
                calInst[GregorianCalendar.MINUTE]      //HOUR denotes the hour per 12-hour AM-PM clock. ex: 10:05 PM = 10
            return (hour * 60 + minute)
        }
    }


    private fun getTimeDifferenceInMinutes(
        currentTimeInMinutes: Int,
        selectedTimeInMinutes: Int
    ): Int {

        return if (selectedTimeInMinutes >= currentTimeInMinutes) {
            selectedTimeInMinutes - currentTimeInMinutes
        } else {
            (selectedTimeInMinutes - currentTimeInMinutes) * -1
        }
    }

    private fun setTimeTxt() {
        Log.i(METHOD_CALL, "setTimeTxt()")
        val selectedTimeInMinutes: Int = (this.hourOfDaySelected * 60) + this.minuteOfDaySelected

        //convert minutes to hours by dividing by 60
        val timeDiff =
            getTimeDifferenceInMinutes(
                getCurrentTimeInMinutes(),
                selectedTimeInMinutes
            )    //Returns the time difference in minutes. for ex:
        // a time diff of 2 hrs 30 min = 2 * 60 + 30 = 150 mins (INT)
        var timeDiffInHours: Double =
            (timeDiff.toDouble() / 60)                           // Convert the 150 mins into hours (DOUBLE) = 150 / 60 = 2.5 hrs (DOUBLE)
        Log.i(TAG, "setTimeTxt: timeDiffInHrs = $timeDiffInHours")

        if (timeDiffInHours.toString().length > 4) {
//            timeDiffInHours = String.format("%.2f", timeDiffInHours).toDouble()
            //turns 0.067 into 0.07 (rounds to decimal)
            timeDiffInHours = Math.round(timeDiffInHours * 100.0) / 100.0
        }

        val timeArray = fixTime(timeDiffInHours)

        toastHour = timeArray[0]
        toastMinute = timeArray[1]
        hourDiff = timeArray[0].toInt()
        minuteDiff = timeArray[1].toInt()
        callback.updateText("${timeArray[0]}:${timeArray[1]}")

        timeSetAtInMins = getCurrentTimeInMinutes()

        updateProgressBar()
    }

    private fun fixTime(timeDiffInHrs: Double): ArrayList<String> {
        val arr: ArrayList<String> = timeDiffInHrs.toString().split(".") as ArrayList<String>
        //trim the string to the first 2 chars ie 1333333 becomes 13
        arr[1] = arr[1].take(2)

        Log.i(ARR, "fixTime__1: $arr")
        if (arr[1].length < 2) {
            //if minutes is single dig i.e 8 minutes the change it to double dig i.e 09 minutes
            arr[1] = ("0${arr[1]}")
        }
        arr[1] = (Math.round((((arr[1].toDouble() / 100) * 60) * 100.0) / 100.0)).toString()
        Log.i(ARR, "fixTime__2: ${arr[1]}")

        val iterator = arr.listIterator()
        while (iterator.hasNext()) {
            val oldValue = iterator.next()
            if (oldValue.length == 1) iterator.set("0$oldValue")
        }
        Log.i(ARR, "fixTime__3: arr = $arr")
        return arr
    }

    private fun updateProgressBar() {
        Log.i(METHOD_CALL, "updateProgressBar()")
        try {
            val timeDiff = (hourDiff * 60) + minuteDiff
            val p = (((getCurrentTimeInMinutes() - timeSetAtInMins) * 100) / timeDiff)
            Log.i(TAG, "updateProgressBar: overTimeDiff = $p%")
            callback.updateProgressBar(p)
        } catch (e: ArithmeticException) {
            callback.updateProgressBar(0)
            e.printStackTrace()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use the current time as the default values for the picker
        val c = GregorianCalendar(TimeZone.getDefault())
        val hour = c.get(GregorianCalendar.HOUR_OF_DAY)
        val minute = c.get(GregorianCalendar.MINUTE)

        // Create a new instance of TimePickerDialog and return it
        return TimePickerDialog(
            activity,
            this,
            hour,
            minute,
            DateFormat.is24HourFormat(activity)
        )
    }
}
