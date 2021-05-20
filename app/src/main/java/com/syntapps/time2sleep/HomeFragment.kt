package com.syntapps.time2sleep

import ProgressBarAnimation
import android.app.NotificationManager
import android.content.*
import android.content.Intent.getIntent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat.startForegroundService
import androidx.fragment.app.Fragment
import com.bhargavms.dotloader.DotLoader
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.fragment_homefragment_layout.*
import java.io.Serializable
import java.util.*
import kotlin.math.roundToInt


// TODO: 12/05/2021 reset timer
// TODO: 12/05/2021 on timer completed actions
// TODO: 12/05/2021 stop timer
// TODO: 12/05/2021 "more" menu

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
    private lateinit var dot_loader: DotLoader

    private var timePickerHandler = Handler()
    private lateinit var timePickerRunnable: Runnable
    private lateinit var br: BroadcastReceiver

    private lateinit var timeObj: MyTimeObj
    private lateinit var serviceIntent: Intent

    private var isTimerRunning = false
    val ONE_MIN: Long = 60000

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: Destroying Fragment...")
        sharedPrefs.edit().also {
            it.putInt("TIME_SET_FOR_IN_MINS", timeObj.timeSetFor)
            it.putInt("TIME_SET_AT_IN_MINS", timeObj.timeSetAt)
            it.putInt(getString(R.string.timePassedInMins), timeObj.timePassed)
            it.putBoolean("isTimerRunning", isTimerRunning)
        }.apply()
        Log.i(
            TAG,
            "onDestroy: TimeObj when frag destroyed: " +
                    "\n -timePassed : ${timeObj.timePassed} " +
                    "\n \t -timeSetAt : ${timeObj.timeSetAt} " +
                    "\n \t -timeSetFor : ${timeObj.timeSetFor}" +
                    "\n -> timeDiff : ${timeObj.getTimeDifference()
                    }"
        )
        super.onDestroy()
    }

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

        rootView.findViewById<Button>(R.id.newTimerButton).also {
            it.text = getString(R.string.newTimerButton)
            it.setOnClickListener(this)
        }
        rootView.findViewById<Button>(R.id.resetTimerButton).also {
            it.text = getString(R.string.resetTimerButton)
            it.setOnClickListener(this)
        }
        rootView.findViewById<Button>(R.id.cancelTimerButton).also {
            it.text = getString(R.string.cancelTimerButton)
            it.setOnClickListener(this)
        }

        timeLeftTV = rootView.findViewById(R.id.timeLeftTV)

        progressBar = rootView.findViewById(R.id.progressBar)

        dot_loader = rootView.findViewById(R.id.dot_loader)

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

                sharedPrefs.edit().putString("LAST_BUTTON_PRESSED", "newTimerButton").apply()

                val filter = IntentFilter()
                filter.addAction("TimePickerSet")

                TimePicker(fragContext).show(
                    fragmentManager!!,
                    null
                )
                try {
                    timePickerHandler.removeCallbacks(timePickerRunnable)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                fragContext.registerReceiver(br, filter)

            }
            R.id.cancelTimerButton -> {
                sharedPrefs.edit().putString("LAST_BUTTON_PRESSED", "cancelTimerButton").apply()

                cancelTimerButton.isClickable = false
                val r = Runnable {
                    cancelTimerButton.isClickable = true
                }
                val h = Handler()
                h.postDelayed(r, 2000)

                MyAlarm(fragContext).cancelAlarms()
                try {
                    timePickerHandler.removeCallbacks(timePickerRunnable)
                    myCallback.updateProgressBar(0, 0, 100)
                    myCallback.updateText(getString(R.string.startTime))
                    makeDotsVisible(false)
                    isTimerRunning = false
                } catch (e: Exception) {
                    Log.e(TAG, "onClick Exception: ${e.localizedMessage}")
                }
            }

            R.id.resetTimerButton -> {
                sharedPrefs.edit().putString("LAST_BUTTON_PRESSED", "resetTimerButton").apply()

                resetTimerButton.isClickable = false
                val r = Runnable {
                    resetTimerButton.isClickable = true
                }
                val h = Handler()
                h.postDelayed(r, 2000)

                try {
                    timePickerHandler.removeCallbacks(timePickerRunnable)
                } catch (e: Exception) {
                    Log.e(TAG, "onClick Exception: ${e.localizedMessage}")
                }

                val c = GregorianCalendar(TimeZone.getDefault())
                val hour = c.get(GregorianCalendar.HOUR_OF_DAY)
                val minute = c.get(GregorianCalendar.MINUTE)
                val s = c.get(GregorianCalendar.SECOND)

                val currentTimeInMinutes = (hour * 60) + minute + (s / 60)

                val timeSetForInMins: Int = currentTimeInMinutes + timeObj.getTimeDifference()
                //currentTimeInMins gets set in the OnCreateDialog function

                Log.i(TAG, "onClick: timediff = ${timeObj.getTimeDifference()}")
                MyAlarm(
                    fragContext,
                    c.timeInMillis + (timeObj.getTimeDifference() * ONE_MIN)
                ).setAlarm()

                val filter = IntentFilter("TimePickerSet")
                filter.addAction("TimePickerSet")
                fragContext.registerReceiver(br, filter)

                fragContext.sendBroadcast(Intent("TimePickerSet").also {
                    it.putExtra("TIME_SET_FOR_IN_MINS", timeSetForInMins)
                    it.putExtra("TIME_SET_AT_IN_MINS", currentTimeInMinutes)
                })

                Toasty.info(fragContext, "Timer Reset", Toast.LENGTH_SHORT, true)
                    .show()
            }
        }
    }

    private fun makeDotsVisible(makeVisible: Boolean) {
        if (makeVisible) {
            dot_loader.visibility = View.VISIBLE
        } else {
            dot_loader.visibility = View.INVISIBLE
        }
    }

    override fun onPause() {
        Log.i(TAG, "onPause ()")
        if (timeObj.getTimeDifference() - timeObj.timePassed > 0) {
            Toast.makeText(
                fragContext,
                "Making sure your timer keeps running...", Toast.LENGTH_SHORT
            ).show()

            try {
                timePickerHandler.removeCallbacks(timePickerRunnable)
            } catch (e: Exception) {
                Log.e(TAG, "onPause: ${e.localizedMessage}")
            }

            startMyService()
        }
        super.onPause()
    }

    override fun onResume() {
        Log.i(TAG, "onResume()")
        try { //stop service
            fragContext.stopService(serviceIntent) //this also sends broadcast
        } catch (e: Exception) {
            Log.e(TAG, "onResume: 159: ${e.printStackTrace()} :\n${e.message}")
        }
        super.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate: ()")
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

        serviceIntent =
            Intent(requireActivity().applicationContext, MyForegroundService::class.java).also {
                it.action = "START"
            }

        br = MyReceiver()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.i(TAG, "onActivityCreated: ()")
        super.onActivityCreated(savedInstanceState)
        //finally initialisations
        myCallback = object : MyCallback {

            override fun updateText(string: String, calledFromFunctionName: String?) {
                Log.i(TAG, "updateText(): $calledFromFunctionName")
                timeLeftTV.text = string
            }

            override fun updateProgressBar(
                timePassedInMinutes: Int,
                timeDifferenceInMins: Int,
                overrideSetPercentage: Int?
            ) {
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
                animation.duration = 650 // milis
                progressBar.startAnimation(animation)
            }
        }

        timePickerRunnable = object : Runnable {
            override fun run() {
                Log.i(TAG, "run()")
                timeObj.oneMinutePassed()
                //add 1 to the "timePassedInMinutes" since timer inception
                myCallback.updateProgressBar(
                    timeObj.timePassed,
                    timeObj.getTimeDifference(),
                    null
                )
                val timePassedArray =
                    timeFixToArray((timeObj.getTimeDifference() - timeObj.timePassed).toDouble() / 60)
                //update text
                myCallback.updateText("${timePassedArray[0]} : ${timePassedArray[1]}")

                //                  // 100% guarantee that this always happens, even if
                //                 // your update method throws an exception
                if (timeObj.getTimeDifference() - timeObj.timePassed == 0) {
                    //here we are basically checking if the time difference between when we set the timer to go off and the time
                    // we set the timer (17:09 - 17:03 = 6 mins timer) has passed
                    // i.e. if timer should be completed by now
                    val intentFilter = IntentFilter()
                    intentFilter.addAction("TimePickerSet")
                    intentFilter.addAction("Service Stopped")
                    makeDotsVisible(false)
                    isTimerRunning = false
                    timePickerHandler.removeCallbacks(this)

                } else {
                    //if timer has not ended after 1 min then set a handler/runnable post delayed for another 1 min. then check back
                    timePickerHandler.postDelayed(this, ONE_MIN)
                }
            }
        }

        val arr =
            timeFixToArray((timeObj.getTimeDifference() - timeObj.timePassed).toDouble() / 60)
        myCallback.updateText("${arr[0]} : ${arr[1]}")
        myCallback.updateProgressBar(
            timeObj.timePassed,
            timeObj.getTimeDifference(),
            null
        )
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

    private fun startMyService() {
        Log.i(TAG, "startMyService: Starting Foreground Service...")
        fragContext.registerReceiver(br, IntentFilter("Service Stopped"))
        serviceIntent.putExtra("TIME_OBJ", timeObj)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(requireActivity().applicationContext, serviceIntent)
            return
        }
        fragContext.startService(serviceIntent)
    }

    inner class MyReceiver : BroadcastReceiver(), Serializable {
        override fun onReceive(context: Context?, intent: Intent?) {

            fragContext.unregisterReceiver(this) //unregister in-order to ensure that only received once

            if (intent != null) {
                Log.i(TAG, "onReceive: intent received with action : ${intent.action}")

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
                        timePickerHandler.postDelayed(timePickerRunnable, ONE_MIN)

                        makeDotsVisible(true)

                        isTimerRunning = true
                    }

                    "Service Stopped" -> {
                        //instantiateRunnable()
                        timeObj = intent.getParcelableExtra("TIME_OBJ") as MyTimeObj
                        Log.i(
                            TAG,
                            "onReceive: Foreground Service Returned Time OBJ: " +
                                    "\n -timePassed : ${timeObj.timePassed} " +
                                    "\n \t -timeSetAt : ${timeObj.timeSetAt} " +
                                    "\n \t -timeSetFor : ${timeObj.timeSetFor}" +
                                    "\n -> timeDiff : ${timeObj.getTimeDifference()
                                    }"
                        )

                        if (timeObj.getTimeDifference() - timeObj.timePassed > 0) {
                            Log.i(TAG, "onReceive: timeLeft > 0")
                            val arr = timeFixToArray(
                                (timeObj.getTimeDifference() - timeObj.timePassed).toDouble() / 60
                            )
                            Log.i(TAG, "onReceive arr txt: ${arr[0]} : ${arr[1]}")
                            myCallback.updateText(
                                "${arr[0]} : ${arr[1]}",
                                "OnReceive: Service Stopped"
                            )

                            myCallback.updateProgressBar(
                                timeObj.timePassed,
                                timeObj.getTimeDifference(),
                                null
                            )

                            timePickerHandler.postDelayed(timePickerRunnable, ONE_MIN /* 1min */)
                            makeDotsVisible(true)

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                //cancel all notifications when app resumed
                                val notificationManager: NotificationManager =
                                    fragContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                notificationManager.cancelAll()
                            }

                        } else {
                            Log.i(TAG, "onReceive: timeLeft < 0")
                            myCallback.updateText(getString(R.string.startTime))

                            myCallback.updateProgressBar(
                                timeObj.timePassed,
                                timeObj.getTimeDifference(),
                                100
                            )
                            makeDotsVisible(false)
                        }
                    }
                }
            }
        }
    }
}
