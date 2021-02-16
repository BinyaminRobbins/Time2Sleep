package com.syntapps.time2sleep

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.math.roundToInt

class TimeTickerReceiver() : BroadcastReceiver() {

    private val TAG = "TimeTickerReceiver"
    private var timeFixToArray: ArrayList<String>? = null

    private lateinit var myContext: Context
    private var notificationManager: NotificationManager? = null
    private var timeSetInMins: Int = 2319
    private var timeSetDifference: Int = 2319
    private lateinit var channelID: String
    private var notifManager: NotificationManager? = null

    constructor (
        MY_CONTEXT: Context,
        notificationManager: NotificationManager?,
        TIME_SET_DIFFERENCE: Int,
        TIME_SET_AT_IN_MINS: Int,
        CHANNEL_ID: String
    ) : this() {
        myContext = MY_CONTEXT
        notifManager = notificationManager
        timeSetDifference = TIME_SET_DIFFERENCE
        timeSetInMins = TIME_SET_AT_IN_MINS
        channelID = CHANNEL_ID

    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Time2Sleep"
            val descriptionText = "Your timer is done!"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                myContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_TIME_TICK) {
            Log.i(TAG, "onReceive: 21: TIME_TICK")
            createNotificationChannel()
            updateTimer(timeSetDifference)
        }
    }

    private fun updateTimer(timeSetDifference: Int) {
        Log.i(TAG, "updateTimer: 66: updating...")
        //convert the time diff into minutes ex: 1.5 hrs = 90 mins
        Log.i(TAG, "time set diff = $timeSetDifference")
        var timeRemainingInHRS: Double =
            (timeSetDifference - getTimePassedInMins(timeSetInMins)).toDouble() / 60
        Log.i(TAG, "time remaining in hrs = $timeRemainingInHRS")

        if (timeRemainingInHRS.toString().length > 4 || timeRemainingInHRS.toString().length == 3) {
            timeRemainingInHRS = (timeRemainingInHRS * 100.0).roundToInt() / 100.0
        }

        Log.i(TAG, "TRIH = $timeRemainingInHRS")

        timeFixToArray = timeFixToArray(timeRemainingInHRS)

        if (timeFixToArray!![0] == "0" || timeFixToArray!![0] == "00") {
            if (timeFixToArray!![1] == "0" || timeFixToArray!![1] == "00") {
                Log.i(TAG, "updateTimer: 79: Attempting Send Notification")
                sendNotification()
                unregister()
            }
        }
    }

    private fun unregister() {
        try {
            myContext.getSharedPreferences(myContext.getString(R.string.sharedPrefsName), 0)
                .edit().putBoolean("isReceiverRegistered", false).apply()
            myContext.unregisterReceiver(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getTimePassedInMins(timeSetInMins: Int): Int {
        //get the time passed in mins since the timer was set
        return CurrentTime().currentTimeInMinutes - timeSetInMins
    }

    private fun timeFixToArray(timeDiffInHrs: Double): ArrayList<String> {
        val arr: ArrayList<String> =
            timeDiffInHrs.toString().split(".") as ArrayList<String>
        //trim the string to the first 2 chars ie 1333333 becomes 13
        arr[1] = arr[1].take(2)

        if (arr[1].length < 2) {
            //if minutes is single dig i.e 8 minutes the change it to double dig i.e 09 minutes
            arr[1] = ("${arr[1]}0")
        }
        arr[1] =
            ((((arr[1].toDouble() / 100) * 60) * 100.0) / 100.0).roundToInt().toString()

        val iterator = arr.listIterator()
        while (iterator.hasNext()) {
            val oldValue = iterator.next()
            if (oldValue.length == 1) iterator.set("0$oldValue")
        }
        return arr
    }

    private fun sendNotification() {
        val notificationID = 101
        val channelID = "com.syntapps.time2sleep"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notification = NotificationCompat.Builder(myContext, channelID).also {
                it.setContentTitle("Time2Sleep App")
                it.setContentText("Your time is up!")
                it.setSmallIcon(R.mipmap.ic_launcher_xd_round)
                it.setChannelId(channelID)
                it.setAutoCancel(true)
            }

            with(NotificationManagerCompat.from(myContext)) {
                // notificationId is a unique int for each notification that you must define
                notify(notificationID, notification.build())
            }
            notifManager?.notify(notificationID, notification.build())

        } else {
            Log.i(TAG, "sendNotification: 140: 140")
        }

    }
}