package com.syntapps.time2sleep

import android.app.Service
import android.content.*
import android.os.IBinder
import android.util.Log

class MyService : Service() {

    private val TAG = "MyService_log"
    var timeChangedReceiver: BroadcastReceiver? = null
    private lateinit var fixedTime: ArrayList<String>
    private var sharedPreferences: SharedPreferences =
        getSharedPreferences(getString(R.string.sharedPrefsName), 0)

    init {
        Log.d(getString(R.string.serviceUpdateTAG), "Service is running... ")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(getString(R.string.serviceUpdateTAG), "onDestroy: Service Destroyed")
        sharedPreferences.edit().putString("fixedTime0",fixedTime[0]).apply()
        sharedPreferences.edit().putString("fixedTime1",fixedTime[1]).apply()
        unregisterReceiver(timeChangedReceiver)
        stopSelf()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_TIME_TICK)
            timeChangedReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val action = intent.action
                    if (action == Intent.ACTION_TIME_TICK) {
                        //updateProgress()
                        //convert the time diff into minutes ex: 1.5 hrs = 90 mins
                        val timeDiff: Double = ((sharedPreferences.getInt(
                            "hourDiff",
                            0
                        ) * 60) + sharedPreferences.getInt("minDiff", 0)).toDouble()
                        //get the time passed in mins since the timer was set
                        val timePassedInMins =
                            (CurrentTime().currentTimeInMinutes - sharedPreferences.getInt(
                                "timeSetAtInMinutes",
                                0
                            ))
                        var num = (timeDiff - timePassedInMins) / 60
                        if (num.toString().length > 4) {
                            num = Math.round(num * 100.0) / 100.0
                        }
                        fixedTime = MainActivity2.fixTime(num)
                    }
                }
            }
            registerReceiver(timeChangedReceiver, filter)
        return START_STICKY
    }
}