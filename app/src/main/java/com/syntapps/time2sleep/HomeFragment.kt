package com.syntapps.time2sleep

import ProgressBarAnimation
import android.app.NotificationManager
import android.content.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import es.dmoral.toasty.Toasty
import java.util.ArrayList
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
                TimePicker(fragContext, sharedPrefs, myCallback).show(fragmentManager!!, null)

                val filter = IntentFilter()
                filter.addAction("Update UI")
                val broadcastReceiver = MyReceiver()
                fragContext.registerReceiver(broadcastReceiver, filter)

                // TODO: 13/02/2021 Create loading icon that shows that progress bar is running and time is being tracked
                // probably 3 dot chaging icons
            }
            R.id.resetTimerButton -> {
                Toasty.info(fragContext, "Reset Timer", Toast.LENGTH_SHORT, true)
                    .show()
            }
            // TODO: 18/01/2021 Reset Timer
        }
    }

    override fun onPause() {
        Toast.makeText(fragContext, "Making sure your timer keeps running...", Toast.LENGTH_LONG)
            .show()
        fragContext.startService(serviceIntent)
        super.onPause()
    }


    override fun onResume() {
        // TODO: 10/03/2021 get the previous timer text and pbar progress
        Log.i(TAG, "onResume()")
        try { //stop service
            fragContext.stopService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val timeSetFor = sharedPrefs.getInt("TIME_SET_FOR_IN_MINS", 0)
        val timeSetAt = sharedPrefs.getInt("TIME_SET_AT_IN_MINS", 0)
        val timePassed = sharedPrefs.getInt(getString(R.string.timePassedInMins), 0)
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
        super.onResume()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        sharedPrefs = fragContext.getSharedPreferences(getString(R.string.sharedPrefsName), 0)

        myCallback = object : MyCallback {

            override fun updateText(string: String) {
                timeLeftTV.text = string
                Log.i(TAG, "updateText: 146: updating text()")
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
        serviceIntent = Intent(fragContext, MyService::class.java)
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
}