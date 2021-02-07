package com.syntapps.time2sleep

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import androidx.fragment.app.DialogFragment
import java.util.*

class TimePicker(private val myContext: Context) : DialogFragment(),
    TimePickerDialog.OnTimeSetListener {

    private var hourOfDaySelected: Int = 0
    private var minOfDaySelected: Int = 0

    private val TAG = "TimePicker"

    override fun onTimeSet(
        view: android.widget.TimePicker?,
        hourOfDay: Int,
        minute: Int
    ) {
        hourOfDaySelected = hourOfDay   //hour of day selected
        minOfDaySelected = minute       // min of day selected
        // ex: 23:45 => hourOfDay = 23, minOfDay = 45

        val intent = Intent().also {
            it.action = "OnTimeSet()"
            it.putExtra("hourOfDaySelected", hourOfDaySelected)
            it.putExtra("minOfDaySelected", minOfDaySelected)
        }
        Log.i(TAG, "onTimeSet: 36: hourOfDaySelected = $hourOfDaySelected")
        Log.i(TAG, "onTimeSet: 37: minOfDaySelected = $minOfDaySelected")

        myContext.sendBroadcast(intent)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
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