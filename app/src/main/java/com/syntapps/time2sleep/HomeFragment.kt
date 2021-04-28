package com.syntapps.time2sleep

import ProgressBarAnimation
import android.app.NotificationManager
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat.startForegroundService
import androidx.fragment.app.Fragment
import es.dmoral.toasty.Toasty
import java.io.Serializable
import java.util.*
import kotlin.math.roundToInt

class HomeFragment(private val fragContext: Context) : Fragment(),
    CompoundButton.OnCheckedChangeListener,
    View.OnClickListener {

    private val TAG = "HomeFragment"
    private lateinit var sharedPrefs: SharedPreferences

    private lateinit var myCallback: MyCallback

    private lateinit var spotifySwitchObject: SwitchObject
    private lateinit var youtubeSwitchObject: SwitchObject
    private lateinit var netflixSwitchObject: SwitchObject
    private lateinit var airplaneModeSwitchObject: SwitchObject

    private lateinit var spotifySwitch: Switch
    private lateinit var youtubeSwitch: Switch
    private lateinit var netflixSwitch: Switch
    private lateinit var airplaneModeSwitch: Switch

    private lateinit var timeLeftTV: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var br: BroadcastReceiver

    private var timePickerHandler = Handler()
    private lateinit var timePickerRunnable: Runnable

    private lateinit var timeObj: MyTimeObj

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //for view variables declaring

        val rootView = inflater.inflate(
            R.layout.fragment_homefragment_layout,
            container, false
        )

        spotifySwitchObject = SwitchObject(fragContext, "Spotify")
        spotifySwitch = rootView.findViewById(R.id.spotifySwitch)

        youtubeSwitchObject = SwitchObject(fragContext, "YouTube")
        youtubeSwitch = rootView.findViewById(R.id.youtubeSwitch)

        netflixSwitchObject = SwitchObject(fragContext, "Netflix")
        netflixSwitch = rootView.findViewById(R.id.netflixSwitch)

        airplaneModeSwitchObject = SwitchObject(fragContext, "Airplane Mode")
        airplaneModeSwitch = rootView.findViewById(R.id.airplaneModeSwitch)

        spotifySwitch.setOnCheckedChangeListener(this)
        youtubeSwitch.setOnCheckedChangeListener(this)
        netflixSwitch.setOnCheckedChangeListener(this)
        airplaneModeSwitch.setOnCheckedChangeListener(this)

        rootView.findViewById<Button>(R.id.newTimerButton).setOnClickListener(this)
        rootView.findViewById<Button>(R.id.resetTimerButton).setOnClickListener(this)

        timeLeftTV = rootView.findViewById(R.id.timeLeftTV)

        progressBar = rootView.findViewById(R.id.progressBar)

        return rootView
    }


    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        when (buttonView) {
            spotifySwitch -> spotifySwitchObject.switchClicked(isChecked)
            youtubeSwitch -> youtubeSwitchObject.switchClicked(isChecked)
            netflixSwitch -> netflixSwitchObject.switchClicked(isChecked)
            airplaneModeSwitch -> airplaneModeSwitchObject.switchClicked(isChecked)
        }
    }

    //onClick method for the "New Timer" and "Reset Timer" Buttons
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.newTimerButton -> {

                val filter = IntentFilter()
                filter.addAction("TimePickerSet")

                TimePicker(fragContext, br, filter).show(
                    fragmentManager!!,
                    null
                )
                //fragContext.registerReceiver(broadcastReceiver, filter)

                // TODO: 13/02/2021 Create loading icon that shows that progress bar is running and time is being tracked
                // probably 3 dot changing icons
            }
            R.id.resetTimerButton -> {
                Toasty.info(fragContext, "Reset Timer", Toast.LENGTH_SHORT, true)
                    .show()
            }
            // TODO: 18/01/2021 Reset Timer
        }
    }


    override fun onDestroy() {
        Log.i(TAG, "onDestroy: 121: Destroying Application...")
        sharedPrefs.edit().also {
            it.putInt("TIME_SET_FOR_IN_MINS", timeObj.timeSetFor)
            it.putInt("TIME_SET_AT_IN_MINS", timeObj.timeSetAt)
            it.putInt(getString(R.string.timePassedInMins), timeObj.timePassed)
        }.apply()
        super.onDestroy()
    }


    override fun onPause() {
        Toast.makeText(
            fragContext,
            "Making sure your timer keeps running...", Toast.LENGTH_LONG
        ).show()

        timePickerHandler.removeCallbacks(timePickerRunnable)

        actionOnService("START")

        super.onPause()
    }


    override fun onResume() {
        // TODO: 10/03/2021 get the previous timer text and pbar progress
        Log.i(TAG, "onResume()")
        try { //stop service
            actionOnService("STOP")
        } catch (e: Exception) {
            Log.e(TAG, "onResume: 159: ${e.printStackTrace()} :\n${e.message}")
        }

        val timeSetFor = timeObj.timeSetFor
        val timeSetAt = timeObj.timeSetAt
        val timePassed = timeObj.timePassed
        Log.i(TAG, "onResume: 129: timePassedInMins = $timePassed")
        val timeDiff = timeObj.getTimeDifference()
        if (timeDiff > 0) {
            Log.i(TAG, "onResume: 155: timeDiff is greater than 0")
            val arr = timeFixToArray(
                (timeDiff).toDouble() / 60
            )
            myCallback.updateText("${arr[0]} : ${arr[1]}")

            myCallback.updateProgressBar(
                timePassed,
                timeSetFor - timeSetAt,
                null
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //cancel all notifications when app resumed
                val notificationManager: NotificationManager =
                    fragContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancelAll()
            }
            timePickerHandler.postDelayed(timePickerRunnable, 60000/* 1min */)
        } else {
            Log.i(TAG, "onResume: 174: timeDiff is <= 0")
            myCallback.updateText("00 : 00")

            myCallback.updateProgressBar(
                timePassed,
                timeSetFor - timeSetAt,
                100
            )
        }
        super.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //onCreate for non-graphical initializations

        sharedPrefs = fragContext.getSharedPreferences(
            getString(R.string.sharedPrefsName),
            0
        )

        timeObj = MyTimeObj(
            sharedPrefs.getInt("TIME_SET_FOR_IN_MINS", 0),
            sharedPrefs.getInt("TIME_SET_AT_IN_MINS", 0),
            sharedPrefs.getInt(getString(R.string.timePassedInMins), 0)
        )

        br = MyReceiver()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //finally initialisations
        myCallback = object : MyCallback {

            override fun updateText(string: String) {
                timeLeftTV.text = string
                Log.i(TAG, "updateText: 146: updating text( $string )")
            }

            override fun updateProgressBar(
                timePassedInMinutes: Int,
                timeDifferenceInMins: Int,
                overrideSetPercentage: Int?
            ) {
                Log.i(TAG, "updateProgressBar: 132: Updating Progress Bar")
                val progressPercentage: Float = if (overrideSetPercentage != null) {
                    overrideSetPercentage.toFloat()
                } else {
                    (timePassedInMinutes.toDouble() / timeDifferenceInMins.toDouble()).toFloat() * 100
                }
                //define custom animation
                val animation = ProgressBarAnimation(
                    progressBar,
                    progressBar.progress.toFloat(),
                    progressPercentage
                )
                animation.duration = 650 // 1 second
                progressBar.startAnimation(animation)
            }
        }

        timePickerRunnable = object : Runnable {
            override fun run() {
                val timeSetForInMins = timeObj.timeSetFor
                val timeSetAtInMins = timeObj.timeSetAt
                val timePassedInMins = timeObj.timePassed
                Log.i(TAG, "run()")
                timeObj.oneMinutePassed()
                Log.i(TAG, "run: 262: timePassedInMins = $timePassedInMins")
                //add 1 to the "timePassedInMinutes" since timer inception
                myCallback.updateProgressBar(
                    timePassedInMins,
                    timeSetForInMins - timeSetAtInMins,
                    null
                )
                val timePassedArray =
                    timeFixToArray(((timeSetForInMins - timeSetAtInMins) - timePassedInMins).toDouble() / 60)
                //update text
                myCallback.updateText("${timePassedArray[0]} : ${timePassedArray[1]}")

                //                  // 100% guarantee that this always happens, even if
                //                 // your update method throws an exception
                if ((timeSetForInMins - timeSetAtInMins) - timePassedInMins == 0) {
                    //here we are basically checking if the time difference between when we set the timer to go off and the time
                    // we set the timer (17:09 - 17:03 = 6 mins timer) has passed
                    // i.e. if timer should be completed by now
                    Log.i(TAG, "run: 87: time is up!!")
                    timeObj.timePassed = 0
                    timePickerHandler.removeCallbacks(this)
                    fragContext.registerReceiver(
                        br,
                        IntentFilter("TimePickerSet")
                    )

                } else {
                    //if timer has not ended after 1 min then set a handler/runnable post delayed for another 1 min. then check back
                    timePickerHandler.postDelayed(this, 60000)
                }
            }
        }
    }

    companion object {
        //used in "HomeFragment" and "TimePickerDialog"
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

    private fun actionOnService(action: String) {
        Intent(requireActivity().applicationContext, MyForegroundService::class.java).also {
            if (action == "START" || action == "STOP") {
                it.action = action
                if (action == "START") {
                    it.putExtra("TIME_OBJ", timeObj)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.i(TAG, "Starting the service in >=26 Mode")
                    startForegroundService(requireActivity().applicationContext, it)
                    return
                } else {
                    Log.i(TAG, "Starting the service in < 26 Mode")
                    fragContext.startService(it)
                }
            } else return
        }
    }

    inner class MyReceiver : BroadcastReceiver(), Serializable {
        override fun onReceive(context: Context?, intent: Intent?) {

            fragContext.unregisterReceiver(this) //unregister in-order to ensure that only received once

            Log.i(
                TAG,
                "onReceive: 233: Broadcast Received with action ${intent?.action}"
            )

            if (intent != null) {
                when (intent.action) {
                    "TimePickerSet" -> {
                        intent.also {
                            timeObj.timeSetFor = it.getIntExtra("TIME_SET_FOR_IN_MINS", 0)
                            timeObj.timeSetAt = it.getIntExtra("TIME_SET_AT_IN_MINS", 0)
                            timeObj.timePassed = 0
                        }
                        val arr =
                            timeFixToArray((timeObj.timeSetFor - timeObj.timeSetAt).toDouble() / 60)
                        myCallback.updateText("${arr[0]} : ${arr[1]}")
                        myCallback.updateProgressBar(0, 0, 0)

                        Toasty.info(
                            fragContext,
                            "New timer set for ${arr[0]} hour/s & ${arr[1]} mins",
                            Toast.LENGTH_SHORT,
                            true
                        ).show()
                        try {
                            timePickerHandler.removeCallbacks(timePickerRunnable)
                        } catch (e: Exception) {
                            Log.e(TAG, "onReceive: ${e.localizedMessage}")
                        }
                        timePickerHandler.postDelayed(timePickerRunnable, 60000)
                    }

                    "Service Stopped" -> {
                        timeObj = intent.getParcelableExtra("TIME_OBJ") as MyTimeObj
                    }

                }
            }
        }
    }
}