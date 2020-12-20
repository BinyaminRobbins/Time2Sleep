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
import java.util.*
import kotlin.collections.ArrayList

class TimePickerFragment(private val callback: MyCallback, private val myContext: Context) :
    DialogFragment(),
    TimePickerDialog.OnTimeSetListener {

    private val TAG = "TimePickerFragment_log"
    var hourOfDaySelected: Int = 0
    var minuteOfDaySelected: Int = 0

    private lateinit var toastHour: String
    private lateinit var toastMinute: String

    private var hourDiff: Int = 0
    private var minuteDiff: Int = 0

    private var timeChangedReceiver: BroadcastReceiver? = null

    private var timeSetAtInMins: Int = 0

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
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
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_TIME_TICK)
        filter.addAction(Intent.ACTION_TIME_CHANGED)
    val hourSelection = this.hourOfDaySelected
    val minSelection = this.minuteOfDaySelected
        timeChangedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i(TAG, "onReceive: recieved")
                val action = intent.action
                val selectedTimeInMinutes: Int = (hourSelection * 60) + minSelection
                if (action == Intent.ACTION_TIME_CHANGED || action == Intent.ACTION_TIME_TICK) {
                    Log.i(TAG, "onReceive: updating -> $action")
                    val timePassed = getCurrentTimeInMinutes() - timeSetAtInMins
                    Log.i(
                        TAG,
                        "onReceive: time passed in mins = $timePassed"
                    )
                    updateProgressBar()
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
        val selectedTimeInMinutes: Int = (this.hourOfDaySelected * 60) + this.minuteOfDaySelected

        //convert minutes to hours by dividing by 60
        val timeDiff =
            getTimeDifferenceInMinutes(getCurrentTimeInMinutes(), selectedTimeInMinutes)    //Returns the time difference in minutes. for ex:
                                                                                            // a time diff of 2 hrs 30 min = 2 * 60 + 30 = 150 mins (INT)
        var timeDiffInHours: Double = (timeDiff.toDouble() / 60)                           // Convert the 150 mins into hours (DOUBLE) = 150 / 60 = 2.5 hrs (DOUBLE)

        if (timeDiffInHours.toString().length > 3) {
            timeDiffInHours = String.format("%.2f", timeDiffInHours).toDouble()
        }

        val arr: ArrayList<String> = timeDiffInHours.toString().split(".") as ArrayList<String>
        if(arr[1].length < 2){
            arr[1] = ("${arr[1]}0")
        }
        arr[1] = ((arr[1].toDouble() / 100) * 60).toInt().toString()

        val iterator = arr.listIterator()
        while (iterator.hasNext()) {
            val oldValue = iterator.next()
            if (oldValue.length == 1) iterator.set("0$oldValue")
        }

        toastHour = arr[0]
        toastMinute = arr[1]
        hourDiff = arr[0].toInt()
        minuteDiff = arr[1].toInt()
        callback.updateText("${arr[0]}:${arr[1]}")

        timeSetAtInMins = getCurrentTimeInMinutes()

        updateProgressBar()
    }

    private fun updateProgressBar() {
        val timeDiff = (hourDiff * 60) + minuteDiff
//        Log.i(TAG, "updateProgressBar: time diff = $timeDiff")
//        Log.i(TAG, "updateProgressBar: currentTimeInMins = ${getCurrentTimeInMinutes()}")
//        Log.i(TAG, "updateProgressBar: timeSetAtInMins = $timeSetAtInMins")
        val p = (((getCurrentTimeInMinutes() - timeSetAtInMins) * 100) / timeDiff)
        callback.updateProgressBar(p)
       // Log.i(TAG, "updateProgressBar: currentMinsSetAt = $currentMinusSetAt%")
        Log.i(TAG, "updateProgressBar: overTimeDiff = $p%")
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
