package com.syntapps.time2sleep

import android.app.*
import android.content.*
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.math.roundToInt


class MyService : Service() {

    private val TAG = "MyService"
    private var timeChangedReceiver: BroadcastReceiver? = null
    private lateinit var sharedPrefs: SharedPreferences

    private var TIME_SET_DIFFERENCE: Int = 0
    private var TIME_SET_AT_IN_MINS: Int = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: 31: Destroying Service...")
        if (sharedPrefs.getBoolean("isReceiverRegistered", true)) {
            unRegisterBRReceiver(timeChangedReceiver)
        }
        super.onDestroy()
    }

    private fun registerBRReceiver(br: BroadcastReceiver?, intentFilter: IntentFilter) {
        registerReceiver(br, intentFilter)
        sharedPrefs.edit().putBoolean("isReceiverRegistered", true).apply()
    }

    private fun unRegisterBRReceiver(br: BroadcastReceiver?) {
        unregisterReceiver(br)
        sharedPrefs.edit().putBoolean("isReceiverRegistered", false).apply()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand:()")

        TIME_SET_AT_IN_MINS = intent?.getIntExtra("TIME_SET_AT_IN_MINS", 2319)!!
        val hourDiff = intent.getIntExtra("HOUR_DIFF", 2319)
        Log.i(TAG, "onStartCommand: 97: hourDiff intentExtra = $hourDiff")
        val minsDiff = intent.getIntExtra("MINS_DIFF", 2319)
        Log.i(TAG, "onStartCommand: 99: minDiff intentExtra = $minsDiff")

        sharedPrefs = getSharedPreferences(getString(R.string.sharedPrefsName), 0)
        sharedPrefs.edit().also {
            it.putInt("TIME_SET_AT_IN_MINS", TIME_SET_AT_IN_MINS)
            it.putInt("HOUR_DIFF", hourDiff)
            it.putInt("MINS_DIFF", minsDiff)
        }.apply()

        TIME_SET_DIFFERENCE = hourDiff * 60 + minsDiff - TIME_SET_AT_IN_MINS

        Log.i(TAG, "onStartCommand: 114: TIME_SET_AT_IN_MINS = $TIME_SET_AT_IN_MINS")
        val filter = IntentFilter(Intent.ACTION_TIME_TICK)

        timeChangedReceiver =
            TimeTickerReceiver(this, TIME_SET_DIFFERENCE, TIME_SET_AT_IN_MINS)

       // if (sharedPrefs.getBoolean("isReceiverRegistered", false) == false) {
            registerBRReceiver(timeChangedReceiver, filter)
        //}

       // updateTimer()

        sharedPrefs.edit().putInt("TIME_SET_AT_IN_MINS", TIME_SET_AT_IN_MINS).apply()

        return START_STICKY
    }
}