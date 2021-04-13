package com.syntapps.time2sleep

import android.app.*
import android.content.*
import android.graphics.Color
import android.os.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.syntapps.time2sleep.HomeFragment.MyReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MyBoundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted: Boolean = false

    private val TAG = "MyService"
    private lateinit var sharedPrefs: SharedPreferences
    private val handler = Handler()
    private var runnable: Runnable? = null
    private var timePassedInMins: Int = 0
    private var br: BroadcastReceiver? = null
    private var filter: IntentFilter? = null

    private val mBinder = MyLocalBinder()

    override fun onBind(intent: Intent?): IBinder {
        // TODO: 13/04/2021 ("Return the communication channel to the service")
        return mBinder
    }

    inner class MyLocalBinder : Binder() {
        fun getService(): MyBoundService {
            return this@MyBoundService

        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate: 32: SERVICE CREATED")

        sharedPrefs = getSharedPreferences(getString(R.string.sharedPrefsName), 0)
        val timeSetForInMins = sharedPrefs.getInt("TIME_SET_FOR_IN_MINS", 0)
        val timeSetAtInMins = sharedPrefs.getInt("TIME_SET_AT_IN_MINS", 0)
        runnable = object : Runnable {
            override fun run() {
                Log.i(TAG, "run: 31: run()")
                timePassedInMins =
                    sharedPrefs.getInt(getString(R.string.timePassedInMins), 0)
                Log.i(TAG, "run: 40: timePassedInMins = $timePassedInMins")

                timePassedInMins += 1
                sharedPrefs.edit()
                    .putInt(getString(R.string.timePassedInMins), timePassedInMins).apply()

                if ((timeSetForInMins - timeSetAtInMins) - timePassedInMins == 0) {
                    Log.i(TAG, "run: 46: TIME UP!!")
                    sharedPrefs.edit()
                        .putInt(getString(R.string.timePassedInMins), timePassedInMins).apply()
                    handler.removeCallbacks(this)
                    stopSelf()
                } else handler.postDelayed(this, 60000)
            }
        }

        val notification = createNotification()
        startForeground(1, notification.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand executed with start ID: $startId")

        if (intent != null) {
            val action = intent.action
            Log.i(TAG, "onStartCommand: 43: started intent with action: $action")

            registerReceiver(br, filter)

            sendBroadcast(Intent("UpdateUI").also {

            })

            when (action) {
                getString(R.string.Action_StartService) -> startMyService()
                getString(R.string.Action_StopService) -> stopMyService()
                else -> Log.i(TAG, "onStartCommand: 47: No action in the received intent")
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
                    handler.postDelayed(runnable, 60000 /*(every 1 min)*/)
                    isServiceStarted = true
                }
                //delay(1 * 60 * 1000 /*1 minute*/)
            }
            Log.i(TAG, "End of startMyService() loop")
        }

    }

    private fun stopMyService() {
        Log.i(TAG, "Stopping Foreground Service from stopMyService()")
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