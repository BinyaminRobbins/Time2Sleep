package com.syntapps.time2sleep

import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import androidx.fragment.app.DialogFragment
import java.io.Serializable
import java.util.*

//This class represents the TimePicker Dialog created when the user selects the "New Timer" button
class TimePicker(
    private val myContext: Context
) :
    DialogFragment(),
    TimePickerDialog.OnTimeSetListener {

    private val TAG = "TimePicker"

    private var currentTimeInMinutes: Int = 0

    override fun onTimeSet(
        view: android.widget.TimePicker?,
        hourOfDay: Int,
        minute: Int
    ) {
        Log.i(TAG, "onTimeSet: 39: TimeSet()")
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
        cal.set(Calendar.MINUTE, minute)
        val timeSetForInMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val timeSetAtInMins =
            currentTimeInMinutes
        //currentTimeInMins gets set in the OnCreateDialog function

        MyAlarm(
            myContext,
            cal.timeInMillis + (cal.get(Calendar.SECOND).toLong() / 60)
        ).setAlarm()

        Log.i(TAG, "onTimeSet: timeSetForInMins: $timeSetForInMins")
        Log.i(TAG, "onTimeSet: timeSetAtInMins: $timeSetAtInMins")

        myContext.sendBroadcast(Intent("TimePickerSet").also {
            it.putExtra("TIME_SET_FOR_IN_MINS", timeSetForInMins)
            it.putExtra("TIME_SET_AT_IN_MINS", timeSetAtInMins)
        })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val c = GregorianCalendar(TimeZone.getDefault())
        val hour = c.get(GregorianCalendar.HOUR_OF_DAY)
        val minute = c.get(GregorianCalendar.MINUTE)
        val s = c.get(GregorianCalendar.SECOND)

        currentTimeInMinutes = (hour * 60) + minute + (s / 60)

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