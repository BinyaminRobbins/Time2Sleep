package com.syntapps.time2sleep

import ProgressBarAnimation
import android.app.NotificationManager
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import es.dmoral.toasty.Toasty
import java.util.*
import kotlin.concurrent.timer
import kotlin.math.roundToInt

class HomeFragment(private val fragContext: Context) : Fragment(),
    CompoundButton.OnCheckedChangeListener,
    View.OnClickListener {

    private val TAG = "HomeFragment"
    private lateinit var sharedPrefs: SharedPreferences

    private lateinit var serviceIntent: Intent
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
    private var timePickerHandler: Handler? = null
    private var timePickerRunnable: Runnable? = null
    private var timePassedInMins = 0

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
                TimePicker(fragContext, sharedPrefs, br, filter).show(
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

    override fun onPause() {
        Toast.makeText(
            fragContext,
            "Making sure your timer keeps running...", Toast.LENGTH_LONG
        )
            .show()
        sharedPrefs.edit()
            .putInt(getString(R.string.timePassedInMins), timePassedInMins).apply()

        timePickerHandler?.removeCallbacks(timePickerRunnable)

        requireActivity().applicationContext.startService(serviceIntent)
        super.onPause()
    }

    override fun onResume() {
        // TODO: 10/03/2021 get the previous timer text and pbar progress
        Log.i(TAG, "onResume()")
        try { //stop service
            requireActivity().applicationContext.stopService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val timeSetFor = sharedPrefs.getInt("TIME_SET_FOR_IN_MINS", 0)
        val timeSetAt = sharedPrefs.getInt("TIME_SET_AT_IN_MINS", 0)
        val timePassed = sharedPrefs.getInt(getString(R.string.timePassedInMins), 0)
        Log.i(TAG, "onResume: 129: timePassedInMins = $timePassed")
        val arr = timeFixToArray(
            ((timeSetFor - timeSetAt) - timePassed).toDouble() / 60
        )
        myCallback.updateText("${arr[0]} : ${arr[1]}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //cancel all notifications when app resumed
            val notificationManager: NotificationManager =
                fragContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancelAll()
        }
        timePickerHandler?.postDelayed(timePickerRunnable, 60000/* 1min */)
        super.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //onCreate for non-graphical initializations

        sharedPrefs = activity!!.applicationContext.getSharedPreferences(
            getString(R.string.sharedPrefsName),
            0
        )
        br = MyReceiver()
        timePassedInMins = sharedPrefs.getInt(getString(R.string.timePassedInMins), 0)
        serviceIntent = Intent(fragContext, MyService::class.java)

        //this might fail //todo check!!
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
                overrideSetNum: Int?
            ) {
                Log.i("TimePicker", "updateProgressBar: 132: Updating Progress Bar")
                var progressPercentage: Float = if (overrideSetNum != null) {
                    overrideSetNum.toFloat()
                } else {
                    (timePassedInMinutes.toDouble() / timeDifferenceInMins.toDouble()).toFloat() * 100
                }
                //define custom animation
                val animation = ProgressBarAnimation(
                    progressBar,
                    progressBar.progress.toFloat(),
                    progressPercentage
                )
                animation.duration = 1000 // 1 second
                progressBar.startAnimation(animation)
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

    inner class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            fragContext.unregisterReceiver(this) //unregister in-order to ensure that only received once

            sharedPrefs.edit().putInt(getString(R.string.timePassedInMins), 0).apply()
            timePassedInMins = sharedPrefs.getInt(getString(R.string.timePassedInMins), 0)
            Log.i(TAG, "onReceive: 238: timePassedInMins = $timePassedInMins")

            Log.i(TAG, "onReceive: 233: Broadcast Received with action ${intent?.action}")

            val timeSetForInMins = intent?.getIntExtra("TIME_SET_FOR_IN_MINS", 0)!!
            val timeSetAtInMins = intent?.getIntExtra("TIME_SET_AT_IN_MINS", 0)!!

            timePickerHandler =
                Handler(Looper.getMainLooper()) //this should ensure there is ony one handler at a time
            if (timePickerRunnable != null) timePickerHandler?.removeCallbacks(timePickerRunnable)
            // TODO: 02/04/2021 change handler to this class and not in TimePicker in order to cancel all runnig handler/runnables when new timer set

            val arr = timeFixToArray((timeSetForInMins - timeSetAtInMins).toDouble() / 60)
            myCallback.updateText("${arr[0]} : ${arr[1]}")
            myCallback.updateProgressBar(0, 0, 0)

            Toasty.info(
                fragContext,
                "New timer set for ${arr[0]} hour/s & ${arr[1]} mins",
                Toast.LENGTH_SHORT,
                true
            ).show()

            timePickerRunnable = object : Runnable {
                override fun run() {
                    Log.i(TAG, "run()")
                    timePassedInMins += 1
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
                        sharedPrefs.edit().putInt(getString(R.string.timePassedInMins), 0).apply()
                        timePickerHandler?.removeCallbacks(this)
                        fragContext.registerReceiver(this@MyReceiver, IntentFilter("TimePickerSet"))

                    } else {
                        //if timer has not ended after 1 min then set a handler/runnable post delayed for another 1 min. then check back
                        timePickerHandler?.postDelayed(this, 60000)
                    }
                }
            }

            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (timePickerHandler != null) if (timePickerHandler!!.hasCallbacks(
                        timePickerRunnable
                    )
                ) {
                    Log.i(TAG, "onReceive: 274: Attempting to remove all callbacks from mHandler")
                    timePickerHandler?.removeCallbacks(timePickerRunnable)
                }
            } else {
                Log.e(TAG, "VERSION.SDK_INT < Q - could not tell if handler has callbacks")
            }*/

            timePickerHandler?.postDelayed(timePickerRunnable, 60000)

        }
    }
}
