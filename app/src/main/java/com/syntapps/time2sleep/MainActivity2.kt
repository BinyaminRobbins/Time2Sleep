package com.syntapps.time2sleep

import ProgressBarAnimation
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.syntapps.time2sleep.databinding.ActivityMain2Binding
import es.dmoral.toasty.Toasty
import kotlin.math.roundToInt


class MainActivity2 : AppCompatActivity(), CompoundButton.OnCheckedChangeListener,
    View.OnClickListener {

    private lateinit var serviceIntent: Intent
    private lateinit var binding: ActivityMain2Binding
    private val TAG = "MainActivity2_log"

    private lateinit var spotifySwitchObject: SwitchObject
    private lateinit var youtubeSwitchObject: SwitchObject
    private lateinit var netflixSwitchObject: SwitchObject
    private lateinit var airplaneModeSwitchObject: SwitchObject

    private lateinit var txtV: TextView
    private lateinit var txt: MyCallback
    private var broadcastReceiver: BroadcastReceiver? = null

    private var hourDiff: Int = 0 //time difference between selected hour and current hour
    private var minDiff: Int = 0  //time difference between selected minute and current minute
    private var timeSetAtInMins: Int = 0
    private var isReceiverRegistered: Boolean = false

    private var notificationManager: NotificationManager? = null

    private lateinit var sharedPrefs: SharedPreferences

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        when (buttonView) {
            binding.spotifySwitch -> spotifySwitchObject.switchClicked(isChecked)
            binding.youtubeSwitch -> youtubeSwitchObject.switchClicked(isChecked)
            binding.netflixSwitch -> netflixSwitchObject.switchClicked(isChecked)
            binding.airplaneModeSwitch -> airplaneModeSwitchObject.switchClicked(isChecked)
        }
    }

    //onClick method for the "New Timer" and "Reset Timer" Buttons
    override fun onClick(v: View?) {
        when (v) {
            binding.newTimerButton -> {
                TimePicker(this).show(supportFragmentManager, "timePicker")

                if (isReceiverRegistered) {
                    unRegisterBReceiver(broadcastReceiver)
                }
                val filter = IntentFilter()
                filter.addAction("OnTimeSet()")
                filter.addAction(Intent.ACTION_TIME_TICK)
                broadcastReceiver = MyReceiver()
                registerBReceiver(broadcastReceiver, filter)
                // TODO: 13/02/2021 Create loading icon that shows that progress bar is running and time is being tracked
                // probably 3 dot chaging icons

            }
            binding.resetTimerButton -> {
                Toasty.info(this, "Reset Timer", Toast.LENGTH_SHORT, true)
                    .show()
            }
            // TODO: 18/01/2021 Reset Timer
        }
    }

    private fun registerBReceiver(br: BroadcastReceiver?, f: IntentFilter) {
        registerReceiver(br, f)
        isReceiverRegistered = true
    }

    private fun unRegisterBReceiver(br: BroadcastReceiver?) {
        unregisterReceiver(br)
        isReceiverRegistered = false
    }

    override fun onPause() {
        Log.i(TAG, "onPause()")
        setService()
        super.onPause()
    }

    override fun onResume() {
        Log.i(TAG, "onResume()")
        try {
            stopService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (sharedPrefs.getBoolean("isZero", false) == false) {

            //timeleft: timeset - timepassed
            sharedPrefs.also {
                hourDiff = it.getInt("HOUR_DIFF", 2319)
                minDiff = it.getInt("MINS_DIFF", 2319)
                timeSetAtInMins = it.getInt("TIME_SET_AT_IN_MINS", 2319)
            }
            Log.i(TAG, "onResume: 106: hourDiff = $hourDiff")
            Log.i(TAG, "onResume: 107: minsDiff = $minDiff")
            Log.i(TAG, "onResume: 108: timeSetAtInMins = $timeSetAtInMins")

            val timeDiff: Int = ((hourDiff * 60) + minDiff)                     //90
            //get the time passed in mins since the timer was set
            val timePassedInMins =
                (CurrentTime().currentTimeInMinutes - timeSetAtInMins)          //20
            //get the time remaining for the the timer
            var timeRemainingInHours: Double =
                ((timeDiff - timePassedInMins).toDouble() / 60) //1.16666667
            if (timeRemainingInHours.toString().length > 5) {
                timeRemainingInHours =
                    (timeRemainingInHours * 100.0).roundToInt() / 100.0    //1.11666 -> 116 -> 1.16 ;)
            }
            //timeFixToArray() makes the single digit minutes in to multi digits -> ex: 1 min becomes 01
            // the timeFixToArray() function then returns an array with [0] being the 2 dig hour remaining
            // & [1] being the 2 dig minutes remaining
            val fixedTime = timeFixToArray(timeRemainingInHours)

            txt.updateText("${fixedTime[0]} : ${fixedTime[1]}")

            if (!isReceiverRegistered) {
                val filter = IntentFilter()
                filter.addAction("OnTimeSet()")
                filter.addAction(Intent.ACTION_TIME_TICK)
                broadcastReceiver = MyReceiver()
                registerBReceiver(broadcastReceiver, filter)
                // TODO: 13/02/2021 Create loading icon that shows that progress bar is running and time is being tracked
                // probably 3 dot chaging icons
            }
            updateProgress()
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager?.deleteNotificationChannel("com.syntapps.time2sleep")
            }
        }
        // --> Get the values saved in SharedPrefs from the MyService() class and re-register
        //      the BroadcastReceiver / Stop the service from running.

        super.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.i(TAG, "onCreate: Creating...")

        txtV = binding.timeLeftTV

        spotifySwitchObject = SwitchObject(this, "Spotify")
        binding.spotifySwitch.setOnCheckedChangeListener(this)
        youtubeSwitchObject = SwitchObject(this, "YouTube")
        binding.youtubeSwitch.setOnCheckedChangeListener(this)
        netflixSwitchObject = SwitchObject(this, "Netflix")
        binding.netflixSwitch.setOnCheckedChangeListener(this)
        airplaneModeSwitchObject = SwitchObject(this, "Airplane Mode")
        binding.airplaneModeSwitch.setOnCheckedChangeListener(this)

        binding.newTimerButton.setOnClickListener(this)
        binding.resetTimerButton.setOnClickListener(this)

        sharedPrefs = getSharedPreferences(getString(R.string.sharedPrefsName), 0)

        txt = object : MyCallback {

            override fun updateText(string: String) {
                binding.timeLeftTV.text = string

            }

            override fun updateProgressBar(progressPercentage: Int) {
                val anim = ProgressBarAnimation(
                    binding.progressBar,
                    binding.progressBar.progress.toFloat(),
                    progressPercentage.toFloat()
                )
                anim.duration = 1000
                binding.progressBar.startAnimation(anim)
            }
        }

        // --> Get the values saved in SharedPrefs from the MyService() class and re-register
        //      the BroadcastReceiver / Stop the service from running.
        txt.updateProgressBar(0)
        //cancel all current notifications in notifications bar
    }

    private fun getTimeDifferenceInMinutes(
        currentTimeInMinutes: Int,
        selectedTimeInMinutes: Int
    ): Int {

        return if (selectedTimeInMinutes >= currentTimeInMinutes) {
            selectedTimeInMinutes - currentTimeInMinutes
        } else {
            (selectedTimeInMinutes - currentTimeInMinutes) * -1
        }
    }

    fun timeFixToArray(timeDiffInHrs: Double): ArrayList<String> {
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

    private fun updateProgress(funcName: String = "") {
        Log.i(TAG, "updateProgress: 220: funcName? = $funcName")
        var percentage = 0
        val timeDiffInMins = (hourDiff * 60) + minDiff
        try {
            percentage =
                ((CurrentTime().currentTimeInMinutes - timeSetAtInMins) * 100) / timeDiffInMins
        } catch (e: ArithmeticException) {
            Log.d(TAG, "updateProgress: 227: Error calculating progress perc. (this is from catch)")
            e.printStackTrace()
        }

        if (percentage != 100) {
            txt.updateProgressBar(percentage)
        } else {
            txt.updateProgressBar(100)
            if (isReceiverRegistered) {
                unRegisterBReceiver(broadcastReceiver)
                sharedPrefs.edit().putBoolean("isZero", true).apply()
                //this function will not be run again once unRegisterBReciever is called
                // , so there is no need to worry about percentage > 100
            }
        }
        Log.i(TAG, "updateProgress: \np = $percentage")
    }

    private fun setService() {
        serviceIntent = Intent(this, MyService::class.java).also {
            it.putExtra("TIME_SET_AT_IN_MINS", timeSetAtInMins)
            it.putExtra("HOUR_DIFF", hourDiff)
            it.putExtra("MINS_DIFF", minDiff)
            startService(it)
        }
    }

    private fun setTimeTxt(hourOfDaySelected: Int, minuteOfDaySelected: Int) {
        val selectedTimeInMinutes: Int = (hourOfDaySelected * 60) + minuteOfDaySelected

        //convert minutes to hours by dividing by 60
        val timeDiffInMinutes =
            getTimeDifferenceInMinutes(
                CurrentTime().currentTimeInMinutes,
                selectedTimeInMinutes
            )  //Returns the time difference in minutes. for ex:
        // a time diff of 2 hrs 30 min = 2 * 60 + 30 = 150 mins (INT)
        var timeDiffInHours: Double =
            (timeDiffInMinutes.toDouble() / 60)                         // Convert the 150 mins into hours (DOUBLE) = 150 / 60 = 2.5 hrs (DOUBLE)

        if (timeDiffInHours.toString().length > 4 || timeDiffInHours.toString().length == 3) {
//            timeDiffInHours = String.format("%.2f", timeDiffInHours).toDouble()
            //turns 0.067 into 0.07 (rounds to decimal)
            timeDiffInHours = (timeDiffInHours * 100.0).roundToInt() / 100.0

        }

        val timeArray = timeFixToArray(timeDiffInHours)
        //time array will return an array of the current time [0] = hours [1] = minutes
        //ex: 01:20 => [0] = 1 , [1] = 20

        hourDiff = timeArray[0].toInt()
        minDiff = timeArray[1].toInt()

        txt.updateText("${timeArray[0]} : ${timeArray[1]}")

        // Thread {
        timeSetAtInMins = CurrentTime().currentTimeInMinutes
        //  }.start()
    }

    private fun createNotificationChannel(id: String, name: String, description: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(id, name, importance).also {
                it.description = description
                it.enableLights(true)
                it.lightColor = Color.RED
                it.enableVibration(true)
                it.vibrationPattern =
                    longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            }
            notificationManager?.createNotificationChannel(channel)
        } else {
            Log.i(TAG, "createNotificationChannel: 104: 104")
        }
    }

    inner class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            when (intent?.action) {

                "OnTimeSet()" -> {
                    intent.also {
                        setTimeTxt(
                            it.getIntExtra(
                                "hourOfDaySelected",
                                9
                            ),
                            it.getIntExtra(
                                "minOfDaySelected",
                                9
                            )
                        )
                    }
                    txt.updateProgressBar(0)
                    //updateProgress("OnReceive -> OnTimeSet()")

                    if (context != null) {
                        Toasty.info(
                            context,
                            "New Timer Set for:\n${hourDiff} hour/s & $minDiff minute/s",
                            Toast.LENGTH_SHORT,
                            true
                        ).show()
                    }

                    notificationManager =
                        getSystemService(
                            Context.NOTIFICATION_SERVICE
                        ) as NotificationManager

                    createNotificationChannel(
                        "com.syntapps.time2sleep",
                        "Time2Sleep App",
                        "Your timer is up!"
                    )
                }

                Intent.ACTION_TIME_TICK -> {
                    updateProgress("OnReceive -> TIME_TICK")
                    //convert the time diff into minutes ex: 1.5 hrs = 90 mins
                    val timeDiff: Int = ((hourDiff * 60) + minDiff)                     //90
                    //get the time passed in mins since the timer was set
                    val timePassedInMins =
                        (CurrentTime().currentTimeInMinutes - timeSetAtInMins)          //20
                    //get the time remaining for the the timer
                    var timeRemainingInHours: Double =
                        ((timeDiff - timePassedInMins).toDouble() / 60) //1.16666667
                    if (timeRemainingInHours.toString().length > 5) {
                        timeRemainingInHours =
                            (timeRemainingInHours * 100.0).roundToInt() / 100.0    //1.11666 -> 116 -> 1.16 ;)
                    }
                    //timeFixToArray() makes the single digit minutes in to multi digits -> ex: 1 min becomes 01
                    // the timeFixToArray() function then returns an array with [0] being the 2 dig hour remaining
                    // & [1] being the 2 dig minutes remaining
                    val fixedTime = timeFixToArray(timeRemainingInHours)

                    txt.updateText("${fixedTime[0]} : ${fixedTime[1]}")

                }
            }
        }

    }
}