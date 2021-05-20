package com.syntapps.time2sleep

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

class MyAlarm(
    private val myContext: Context,
    private val timeToSetForInMilis: Long = 0
) {

    private val TAG = "MyAlarm"

    private val alarmManager = myContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun cancelAlarms() {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                myContext,
                1,
                getAlarmIntent(),
                0
            )
        ) // cancel all pending alarms and set a new one
    }

    private fun getAlarmIntent(): Intent {
        return Intent(myContext, Alarm::class.java).also {
            it.flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
            it.action = "T2S_ALARM"
        }
    }

    fun setAlarm() {
        Log.i(TAG, "setAlarm: 67: Setting Alarm")
        val receiver = ComponentName(myContext, Alarm::class.java)
        val pm = myContext.packageManager

        pm.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        val p = PendingIntent.getBroadcast(myContext, 1, getAlarmIntent(), 0)
        alarmManager.cancel(p) // cancel all pending alarms and set a new one
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            timeToSetForInMilis + 60000,
            p
        )
    }
}