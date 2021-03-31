package com.syntapps.time2sleep

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class Alarm : BroadcastReceiver() {

    private val TAG = "Alarm"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals("T2S_ALARM")) {
            Log.i(TAG, "onReceive: 127: Alarm Received")
            val i = Intent(context!!, HomeFragment::class.java).also {
                it.putExtra("timeToDisplay", "00 : 00")
                it.putExtra("progressToDisplay", 100)
                it.action = "Update UI"
            }
            context.sendBroadcast(i)

            /*val pm: PowerManager = context .getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakelock =
                pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,) as PowerManager.WakeLock
            wakelock.acquire(1 * 60 * 1000L *//*10 minutes*//*)
*/
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(context)
            }

            sendNotification(context)

//            wakelock.release()
        }
    }

    private fun createNotificationChannel(myContext: Context) {
// Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Time2Sleep App Timer"
            val descriptionText = "Time2Sleep App Timer Notification Channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("T2S_ID", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                myContext.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(myContext: Context) {
        val builder = NotificationCompat.Builder(myContext, "T2S_ID")
            .setSmallIcon(R.mipmap.ic_launcher_xd)
            .setContentTitle("Time2Sleep App Timer")
            .setContentText("Your Sleep Timer is Up")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(myContext)) {
            // notificationId is a unique int for each notification that you must define
            notify(123, builder.build())
        }
    }
}