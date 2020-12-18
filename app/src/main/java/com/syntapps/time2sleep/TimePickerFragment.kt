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
import java.util.*
import kotlin.collections.ArrayList


// TODO: 09/12/2020 CountDownTimer that runs in background when New Timer is set
class TimePickerFragment(private val callback: MyCallback, private val myContext: Context) :
    DialogFragment(),
    TimePickerDialog.OnTimeSetListener {

    private val TAG = "TimePickerFragment_log"
    var hourOfDaySelected: Int = 0
    var minuteOfDaySelected: Int = 0


    private lateinit var timeChangedReceiver: BroadcastReceiver

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        this.hourOfDaySelected = hourOfDay
        this.minuteOfDaySelected = minute
        setTimeTxt()
      //  displayTimerToast()
        setReceiver()
    }

    /*private fun displayTimerToast() {
        Toasty.info(
            myContext,
            "New Timer Set for:\n$hourTimeDiff hour/s & $minuteTimeDiff minute/s",
            Toast.LENGTH_SHORT,
            true
        ).show()
    }*/

    private fun setReceiver() {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_TIME_TICK)
        filter.addAction(Intent.ACTION_TIME_CHANGED)
        timeChangedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                //  val action = intent.action
                Toast.makeText(context, "received", Toast.LENGTH_SHORT).show()
                Log.i(TAG, "onReceive: received")
                /* if (action == Intent.ACTION_TIME_CHANGED || action == Intent.ACTION_TIMEZONE_CHANGED) {
                     Toast.makeText(context, "received", Toast.LENGTH_SHORT).show()
                     Log.i(TAG, "onReceive: received")
                 }*/
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
        Log.i(TAG, "setTimeTxt: selected time i mins = $selectedTimeInMinutes")

        //convert minutes to hours by dividing by 60
        val timeDiff =
            getTimeDifferenceInMinutes(getCurrentTimeInMinutes(), selectedTimeInMinutes)    //Returns the time difference in minutes. for ex:
        Log.i(TAG, "setTimeTxt: timediffinmins = $timeDiff")                            // a time diff of 2 hrs 30 min = 2 * 60 + 30 = 150 mins (INT)
        var timeDiffInHours: Double = (timeDiff / 60).toDouble()                           // Convert the 150 mins into hours (DOUBLE) = 150 / 60 = 2.5 hrs (DOUBLE)

        if (timeDiffInHours.toString().length > 4) {
            timeDiffInHours = String.format("%.2f", timeDiffInHours).toDouble()
        }
        Log.i(TAG, "setTimeTxt: timediff hours = $timeDiffInHours")

        val arr: ArrayList<String> = timeDiffInHours.toString().split(".") as ArrayList<String>
        Log.i(TAG, "setTimeTxt: $arr")
        arr[1] = ((arr[1].toDouble() / 100) * 60).toString()
        Log.i(TAG, "setTimeTxt: revised arr = $arr")

        val iterator = arr.listIterator()
        while (iterator.hasNext()) {
            val oldValue = iterator.next()
            if (oldValue.length == 1) iterator.set("0$oldValue")
        }

        callback.updateText("${arr[0]}:${arr[1]}")

        updateProgressBar(30)
    }

    private fun updateProgressBar(timePassedInMins: Int) {
       /* val timeDiffInMins = (hourTimeDiff * 60) + minuteTimeDiff
        val percentage = if (timeDiffInMins == 0) {
            timeDiffInMins * 100
        } else {
            (timePassedInMins * 100) / timeDiffInMins
        }
        callback.updateProgressBar(percentage)*/
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

    override fun onResume() {
        super.onResume()
        setReceiver()
    }

    override fun onPause() {
        super.onPause()
        myContext.unregisterReceiver(timeChangedReceiver)
    }
}
