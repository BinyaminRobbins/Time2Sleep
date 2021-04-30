package com.syntapps.time2sleep

import android.app.*
import android.content.*
import android.graphics.Color
import android.os.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.Serializable


class MyForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted: Boolean = false

    private val TAG = "MyService"
    private val handler = Handler()
    private var runnable: Runnable? = null

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
                        Log.i(TAG, "run: timePassedInMins = ${timeObj.timePassed}")
                        timeObj.oneMinutePassed()
                        Log.i(TAG, "run:timePassedInMins (after minute) = ${timeObj.timePassed}")

                        if ((timeObj.getTimeDifference() - timeObj.timePassed) == 0) {
                            Log.i(TAG, "run: 46: TIME UP!!")
                            handler.removeCallbacks(this)
                            stopMyService()
                        } else handler.postDelayed(this, 60000)
                    }
                }

                startMyService()
            } else {
                Log.i(TAG, "onStartCommand: non-recognized intent action : $action")
            }
        } else {
            Log.i(TAG, "onStartCommand: 50: NULL INTENT")
        }
        return START_STICKY
    }

    private fun startMyService() {
        if (isServiceStarted) return
        Log.i(TAG, "startMyService: 79: Starting foreground task...")
        isServiceStarted = true
        // we need this lock so our service gets not affected by Doze Mode
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyService::lock").apply {
                    acquire()
                }
            }

        // we're starting a loop in a coroutine
        GlobalScope.launch(Dispatchers.IO) {
            if (isServiceStarted) {
                launch(Dispatchers.IO) {
                    //run code
                    Log.i(TAG, "startMyService: 99: Coroutine Launched")
                    if (runnable != null) {
                        handler.postDelayed(runnable!!, 60000 /*(every 1 min)*/)
                    }
                    isServiceStarted = true
                }
                //delay(1 * 60 * 1000 /*1 minute*/)
            }
            Log.i(TAG, "End of startMyService() loop")
        }

    }

    private fun stopMyService() {
        Log.i(TAG, "Stopping Foreground Service from stopMyService()")
        sendBroadcast(Intent("Service Stopped").also {
            it.putExtra("TIME_OBJ", timeObj)
        })
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            Log.i(TAG, "Service stopped without ever being started: ${e.message}")
        }
        isServiceStarted = false
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

        //this is to create clickable notification that leads to MainActivity
        // Create an Intent for the activity you want to start
        val resultIntent = Intent(this, MainActivity::class.java)
        // Create the TaskStackBuilder
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(resultIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationCompat.Builder(this, "T2S_ID")
                .setSmallIcon(R.drawable.t2s_icon_notif)
                .setColor(this.getColor(R.color.colorPrimaryDark))
                .setContentTitle("Time2Sleep Service")
                .setContentText("Time2Sleep is running")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(resultPendingIntent)
        } else {
            TODO("VERSION.SDK_INT < M")
        }
    }

}