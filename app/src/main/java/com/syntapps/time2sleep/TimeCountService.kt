package com.syntapps.time2sleep

import android.app.Service
import android.content.Intent
import android.os.IBinder

class TimeCountService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return START_NOT_STICKY
    }
}