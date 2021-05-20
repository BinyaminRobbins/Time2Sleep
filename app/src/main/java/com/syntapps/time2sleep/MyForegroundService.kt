package com.syntapps.time2sleep

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat


class MyForegroundService : Service() {

    private val TAG = "MyService"
    private val handler = Handler()
    private var runnable: Runnable? = null
    var ONE_MIN: Long = 60000

    private lateinit var timeObj: MyTimeObj

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopMyService()
        Log.i(TAG, "onDestroy: Destroying Service...")
        super.onDestroy()
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate: 32: SERVICE CREATED")

        val notification = createNotification()
        startForeground(1, notification.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            Log.i(TAG, "onStartCommand: 43: started intent with action: $action")
            if (action == "START") {
                timeObj = intent.getParcelableExtra("TIME_OBJ") as MyTimeObj
                Log.i(TAG, "onStartCommand: timePassed = ${timeObj.timePassed}")
                runnable = object : Runnable {
                    override fun run() {
                        Log.i(TAG, "run: run()")
                        timeObj.oneMinutePassed()

                        if ((timeObj.getTimeDifference() - timeObj.timePassed) == 0) {
                            Log.i(TAG, "run: 46: TIME UP!!")
                            handler.removeCallbacks(this)
                            stopMyService()
                        } else handler.postDelayed(this, ONE_MIN)
                    }
                }
                handler.postDelayed(runnable!!, ONE_MIN /*(every 1 min)*/)
            } else {
                Log.i(TAG, "onStartCommand: non-recognized intent action : $action")
            }
        } else {
            Log.i(TAG, "onStartCommand: 50: NULL INTENT")
        }
        return START_STICKY
    }

    private fun stopMyService() {
        Log.i(TAG, "Stopping Foreground Service from stopMyService()")
        stopForeground(true)
        stopSelf()
        sendBroadcast(Intent("Service Stopped").also {
            it.putExtra("TIME_OBJ", timeObj)
        })
    }

    private fun createNotification(): NotificationCompat.Builder {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Time2Sleep App Timer"
            val descriptionText = "Time2Sleep App Timer Notification Channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("T2S_ID", name, importance).apply {
                description = descriptionText
            }

            channel.let {
                it.enableVibration(true)
                it.enableLights(true)
                it.lightColor = Color.RED
                it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            }

            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

       /* //this is to create clickable notification that leads to MainActivity
        // Create an Intent for the activity you want to start
        val resultIntent = Intent(applicationContext, MainActivity::class.java).also {
            it.putExtra("fromNotification", true)
        }
        // Create the TaskStackBuilder
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(resultIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }
*/
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationCompat.Builder(this, "T2S_ID")
                .setSmallIcon(R.drawable.ic_stat_group_12)
                .setColor(this.getColor(R.color.colorPrimaryDark))
                .setContentTitle("Time2Sleep Timer")
                .setContentText("Your Time2Sleep timer is running")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400))
          //      .setContentIntent(resultPendingIntent)
        } else {
            TODO("VERSION.SDK_INT < M")
        }
    }

}