package com.syntapps.time2sleep

import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.text.format.DateFormat
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import es.dmoral.toasty.Toasty
import java.util.*

//This class represents the TimePicker Dialog created when the user selects the "New Timer" button
class TimePicker(
    private val myContext: Context,
    private val sharedPreferences: SharedPreferences,
    private val callback: MyCallback
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
        val prefName = getString(R.string.timePassedInMins)
        sharedPreferences.edit()
            .putInt(prefName, 0)
            .apply()

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
        cal.set(Calendar.MINUTE, minute)
        val timeSetForInMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val timeSetAtInMins =
            currentTimeInMinutes //currentTimeInMins gets set in the OnCreateDialog function

        setAlarm(
            myContext,
            cal.timeInMillis,
            timeSetForInMins - timeSetAtInMins
        )
        //timeSetDifference is the time difference between when the timer is set to go off
        // & between the time the timer is set at

        val h = Handler()

        val runnable = object : Runnable {
            override fun run() {
                var timePassedInMins = sharedPreferences.getInt(prefName, 0)
                try {
                    Log.i(TAG, "run()")
                    timePassedInMins++
                    //add 1 to the "timePassedInMinutes" since timer inception
                    sharedPreferences.edit().putInt(prefName, timePassedInMins).apply()
                    callback.updateProgressBar(
                        timePassedInMins,
                        timeSetForInMins - timeSetAtInMins,
                        null
                    )
                    val timePassedArray =
                        MainActivity2.timeFixToArray(((timeSetForInMins - timeSetAtInMins) - timePassedInMins).toDouble() / 60)
                    callback.updateText("${timePassedArray[0]} : ${timePassedArray[1]}")
                } finally {
                    // 100% guarantee that this always happens, even if
                    // your update method throws an exception
                    h.postDelayed(this, 60000)
                    if ((timeSetForInMins - timeSetAtInMins) - timePassedInMins <= 0) {
                        h.removeCallbacks(this)
                    }
                }
            }
        }

        sharedPreferences.edit().also {
            it.putInt("TIME_SET_FOR_IN_MINS", timeSetAtInMins)
            it.putInt("TIME_SET_AT_IN_MINS", timeSetAtInMins)
        }.apply()

        h.postDelayed(runnable, 60000)

        val arr = MainActivity2.timeFixToArray((timeSetForInMins - timeSetAtInMins).toDouble() / 60)
        callback.updateText("${arr[0]} : ${arr[1]}")
        callback.updateProgressBar(0, 0, 0)

        Toasty.info(
            myContext,
            "New Timer Set for ${arr[0]} hour/s & ${arr[1]} mins",
            Toast.LENGTH_SHORT,
            true
        ).show()

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val c = GregorianCalendar(TimeZone.getDefault())
        val hour = c.get(GregorianCalendar.HOUR_OF_DAY)
        val minute = c.get(GregorianCalendar.MINUTE)

        currentTimeInMinutes = (hour * 60) + minute

        // Create a new instance of TimePickerDialog and return it
        return TimePickerDialog(
            activity,
            this,
            hour,
            minute,
            DateFormat.is24HourFormat(activity)
        )
    }

    private fun setAlarm(
        myContext: Context,
        cTimeInMilis: Long,
        timeSetDifference: Int
    ) {
        Log.i(TAG, "setAlarm: 67: Setting Alarm")
        val alarmManager = myContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(myContext, Alarm::class.java).also {
            it.flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
            it.action = "T2S_ALARM"
            it.putExtra("timeSetDifference", timeSetDifference)
        }
        val receiver = ComponentName(myContext, Alarm::class.java)
        val pm = myContext.packageManager

        pm.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        val p = PendingIntent.getBroadcast(myContext, 1, i, 0)
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            cTimeInMilis + 1000, p
        )
    }

}