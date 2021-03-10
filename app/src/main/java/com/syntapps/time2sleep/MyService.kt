package com.syntapps.time2sleep

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log


class MyService : Service() {

    private val TAG = "MyService"
    private var stringExtraName: String = ""
    private lateinit var sharedPrefs: SharedPreferences
    private var timePassedInMins: Int = 0
    private val handler = Handler()
    private lateinit var runnable: Runnable

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: 31: Destroying Service...")
        sharedPrefs.edit().putInt(stringExtraName, timePassedInMins).apply()
        handler.removeCallbacks(runnable)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand:()")
        sharedPrefs = getSharedPreferences(getString(R.string.sharedPrefsName), 0)
        stringExtraName = intent?.getStringExtra("sharedStringName") as String
        timePassedInMins = sharedPrefs.getInt(stringExtraName, 0)

        Thread {
            runnable = Runnable {
                // Some code..
                timePassedInMins++
                Log.i(TAG, "onStartCommand: 68: Handler.pD()")
            }

            handler.postDelayed(runnable, 60000 /*(every 1 min)*/)
        }.start()

        return START_STICKY
    }

}