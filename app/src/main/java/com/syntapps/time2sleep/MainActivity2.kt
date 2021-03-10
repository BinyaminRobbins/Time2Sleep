package com.syntapps.time2sleep

import ProgressBarAnimation
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
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
import java.util.ArrayList
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
    private lateinit var myCallback: MyCallback

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
                TimePicker(this, sharedPrefs, myCallback).show(supportFragmentManager, "timePicker")

                val filter = IntentFilter()
                filter.addAction("Update UI")
                val broadcastReceiver = MyReceiver()
                registerReceiver(broadcastReceiver, filter)

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

        val timeSetFor = sharedPrefs.getInt("TIME_SET_FOR_IN_MINS", 0)
        val timeSetAt = sharedPrefs.getInt("TIME_SET_AT_IN_MINS", 0)
        val timePassed = sharedPrefs.getInt(getString(R.string.timePassedInMins), 0)
        val arr = timeFixToArray((timeSetFor - (timeSetAt + timePassed)).toDouble() / 60)
        myCallback.updateText("${arr[0]} : ${arr[1]}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager?.deleteNotificationChannel("com.syntapps.time2sleep")
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

        myCallback = object : MyCallback {

            override fun updateText(string: String) {
                binding.timeLeftTV.text = string

            }

            override fun updateProgressBar(
                timePassedInMinutes: Int,
                timeDifferenceInMins: Int,
                overrideSetNum: Int?
            ) {
                if (overrideSetNum != null) {
                    val animation = ProgressBarAnimation(
                        binding.progressBar,
                        binding.progressBar.progress.toFloat(),
                        overrideSetNum.toFloat()
                    )
                    animation.duration = 1000 // 1 second
                    binding.progressBar.startAnimation(animation)
                } else {
                    Log.i(TAG, "updateProgressBar: 145: updating...")
                    //calculate the pecentage to fill the progress bar based on time set and current time
                    Log.i(TAG, "updateProgressBar: 147: Time Passes = $timePassedInMinutes")
                    Log.i(TAG, "updateProgressBar: 148: TimeDiffInMins = $timeDifferenceInMins")
                    val progressPercentage: Float =
                        (timePassedInMinutes.toDouble() / timeDifferenceInMins.toDouble()).toFloat() * 100
                    Log.i(TAG, "updateProgressBar: 149: progressPercentage = $progressPercentage")

                    val animation = ProgressBarAnimation(
                        binding.progressBar,
                        binding.progressBar.progress.toFloat(),
                        progressPercentage
                    )
                    animation.duration = 1000 // 1 second
                    binding.progressBar.startAnimation(animation)
                }
            }
        }

        // --> Get the values saved in SharedPrefs from the MyService() class and re-register
        //      the BroadcastReceiver / Stop the service from running.
        binding.progressBar.progress = 0
        //cancel all current notifications in notifications bar
    }

    private fun setService() {
        serviceIntent = Intent(this, MyService::class.java).also {
            it.putExtra("stringExtraName", getString(R.string.timePassedInMins))
            startService(intent)
        }
    }

    companion object {
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
    }

    inner class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            when (intent?.action) {
                "Update UI" -> {
                    val s = intent.getStringExtra("timeToDisplay") as String
                    myCallback.updateText(s)
                    val t = intent.getIntExtra("progressToDisplay", 100)
                    myCallback.updateProgressBar(0, 0, t)

                }
            }
        }
    }

}
