package com.syntapps.time2sleep

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.os.Bundle
import android.text.format.DateFormat
import androidx.fragment.app.DialogFragment
import java.util.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class TimePicker(private val myContext: Context) : DialogFragment(),
    TimePickerDialog.OnTimeSetListener {

    private var hourOfDaySelected: Int = 0
    private var minOfDaySelected: Int = 0

    override fun onTimeSet(
        view: android.widget.TimePicker?,
        hourOfDay: Int,
        minute: Int
    ) {
        hourOfDaySelected = hourOfDay
        minOfDaySelected = minute

        val intent = Intent("com.syntapps.time2sleep.TimePicker")
        intent.putExtra("hourOfDaySelected", hourOfDaySelected)
        intent.putExtra("minOfDaySelected", minOfDaySelected)
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