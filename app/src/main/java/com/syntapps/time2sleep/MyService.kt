package com.syntapps.time2sleep

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.IBinder
import android.util.Log


class MyService : Service() {

    private val TAG = "MyService"
    private lateinit var sharedPrefs: SharedPreferences
    private val handler = Handler()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: 31: Destroying Service...")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand:()")
        sharedPrefs = getSharedPreferences(getString(R.string.sharedPrefsName), 0)
        val timeSetForInMins = sharedPrefs.getInt("TIME_SET_FOR_IN_MINS", 0)
        val timeSetAtInMins = sharedPrefs.getInt("TIME_SET_AT_IN_MINS", 0)
        val runnable = object : Runnable {
            override fun run() {
                var timePassedInMins =
                    sharedPrefs.getInt(getString(R.string.timePassedInMins), 2319)
                Log.i(TAG, "run: 34: timePassedInMins INITIAL = $timePassedInMins")
                try {
                    timePassedInMins += 1
                    sharedPrefs.edit().putInt(getString(R.string.timePassedInMins), 0).apply()
                } finally /*to be run even if try{} block fails*/ {
                    handler.postDelayed(this, 60000)
                    if ((timeSetForInMins - timeSetAtInMins) - timePassedInMins == 0) {
                        handler.removeCallbacks(this)
                    }

                }
            }
        }

        handler.postDelayed(runnable, 60000 /*(every 1 min)*/)

        return START_STICKY
    }

}