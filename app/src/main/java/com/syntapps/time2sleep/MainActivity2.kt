package com.syntapps.time2sleep

import ProgressBarAnimation
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.*
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.syntapps.time2sleep.databinding.ActivityMain2Binding
import es.dmoral.toasty.Toasty
import java.lang.NullPointerException
import java.util.*
import kotlin.collections.ArrayList


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
    private var timeChangedReceiver: BroadcastReceiver? = null

    private var hourDiff: Int = 0 //time difference between selected hour and current hour
    private var minDiff: Int = 0  //time difference between selected minute and current minute
    private var timeSetAtInMins: Int = 0

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
                val filter = IntentFilter("com.syntapps.time2sleep.TimePicker")
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val action = intent?.action
                        if (action.equals("com.syntapps.time2sleep.TimePicker")) {
                            val hourOfDaySelected = intent?.getIntExtra("hourOfDaySelected", 0)
                            val minuteOfDaySelected = intent?.getIntExtra("minOfDaySelected", 0)
                            setTimeTxt(hourOfDaySelected!!, minuteOfDaySelected!!)

                            updateProgress()
                            setTimeReceiver()
                        }
                    }
                }
                registerReceiver(receiver, filter)
            }
            binding.resetTimerButton -> Toasty.info(this, "Reset Timer", Toast.LENGTH_SHORT, true)
                .show()
        }
    }

    override fun onPause() {
        sharedPrefs.edit().also {
            it.putInt("hourDiff", hourDiff)
            it.putInt("minDiff", minDiff)
            it.putInt("timeSetAtInMinutes", timeSetAtInMins)
        }.apply()
        Log.i(getString(R.string.serviceUpdateTAG), "onPause: Pausing...")
        // --> Get the required values from SharedPrefs and set service class to update
        //      the values on "ACTION_TIME_TICK" (Broadcast Receiver)
        setService()
        super.onPause()
    }

    override fun onRestart() {
        Log.i(getString(R.string.serviceUpdateTAG), "onRestart: Restarting Main2...")
        stopService(serviceIntent)
        sharedPrefs = getSharedPreferences(getString(R.string.sharedPrefsName), 0)
        val newText: String
        sharedPrefs.also {
            newText = "${it.getString(
                "fixedTime0",
                "00"
            )} " +
                    ":" +
                    " ${it.getString(
                        "fixedTime1",
                        "00"
                    )}"
        }
        txt.updateText(newText)
        // --> Get the values saved in SharedPrefs from the MyService() class and re-register
        //      the BroadcastReceiver / Stop the service from running.
        updateProgress()
        super.onRestart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.i(TAG, "onCreate: Creating...")

        txtV = binding.timeLeftTV

        binding.progressBar.progress = 100

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

        try{
            stopService(serviceIntent)
        }catch(e: UninitializedPropertyAccessException){
            e.printStackTrace()
        }
        val newText: String
        sharedPrefs.also {
            newText = "${it.getString(
                "fixedTime0",
                "00"
            )} " +
                    ":" +
                    " ${it.getString(
                        "fixedTime1",
                        "00"
                    )}"
        }
        txt.updateText(newText)
        // --> Get the values saved in SharedPrefs from the MyService() class and re-register
        //      the BroadcastReceiver / Stop the service from running.
        updateProgress()
    }

    private fun setTimeReceiver() {
        Toasty.info(
            this,
            "New Timer Set for:\n${hourDiff} hour/s & $minDiff minute/s",
            Toast.LENGTH_SHORT,
            true
        ).show()
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_TIME_TICK)
        timeChangedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (action == Intent.ACTION_TIME_TICK) {
                    updateProgress()
                    //convert the time diff into minutes ex: 1.5 hrs = 90 mins
                    val timeDiff: Double = ((hourDiff * 60) + minDiff).toDouble()
                    //get the time passed in mins since the timer was set
                    val timePassedInMins =
                        (CurrentTime().currentTimeInMinutes - timeSetAtInMins)
                    var num = (timeDiff - timePassedInMins) / 60
                    if (num.toString().length > 4) {
                        num = Math.round(num * 100.0) / 100.0
                    }
                    val fixedTime = fixTime(num)
                    txt.updateText("${fixedTime[0]}:${fixedTime[1]}")
                    sharedPrefs.edit().putString("fixedTime0", fixedTime?.get(0)).apply()
                    sharedPrefs.edit().putString("fixedTime1", fixedTime?.get(1)).apply()
                }
            }
        }
        try {
            unregisterReceiver(timeChangedReceiver)
        } catch (e: Exception) {
            Log.d(TAG, "Unregister Receiver Error: \n${e.printStackTrace()}")
        }
        registerReceiver(timeChangedReceiver, filter)
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

    companion object {
        fun fixTime(timeDiffInHrs: Double): ArrayList<String> {
            val arr: ArrayList<String> =
                timeDiffInHrs.toString().split(".") as ArrayList<String>
            //trim the string to the first 2 chars ie 1333333 becomes 13
            arr[1] = arr[1].take(2)

            if (arr[1].length < 2) {
                //if minutes is single dig i.e 8 minutes the change it to double dig i.e 09 minutes
                arr[1] = ("0${arr[1]}")
            }
            arr[1] =
                (Math.round((((arr[1].toDouble() / 100) * 60) * 100.0) / 100.0)).toString()

            val iterator = arr.listIterator()
            while (iterator.hasNext()) {
                val oldValue = iterator.next()
                if (oldValue.length == 1) iterator.set("0$oldValue")
            }
            return arr
        }
    }

    private fun updateProgress() {
        var p = 0
        try {
            val timeDiff = (hourDiff * 60) + minDiff
            p = ((CurrentTime().currentTimeInMinutes - timeSetAtInMins) * 100) / timeDiff
            txt.updateProgressBar(p)
        } catch (e: ArithmeticException) {
            txt.updateProgressBar(0)
            e.printStackTrace()
        }
        if (p == 100 && timeChangedReceiver != null) {
            unregisterReceiver(timeChangedReceiver)
        }
    }

    private fun setService() {
        serviceIntent = Intent(this, MyService::class.java).also {
            startService(it)
        }
    }

    private fun setTimeTxt(hourOfDaySelected: Int, minuteOfDaySelected: Int) {
        val selectedTimeInMinutes: Int = (hourOfDaySelected * 60) + minuteOfDaySelected

        //convert minutes to hours by dividing by 60
        val timeDiff =
            getTimeDifferenceInMinutes(
                CurrentTime().currentTimeInMinutes,
                selectedTimeInMinutes
            )    //Returns the time difference in minutes. for ex:
        // a time diff of 2 hrs 30 min = 2 * 60 + 30 = 150 mins (INT)
        var timeDiffInHours: Double =
            (timeDiff.toDouble() / 60)                           // Convert the 150 mins into hours (DOUBLE) = 150 / 60 = 2.5 hrs (DOUBLE)

        if (timeDiffInHours.toString().length > 4) {
//            timeDiffInHours = String.format("%.2f", timeDiffInHours).toDouble()
            //turns 0.067 into 0.07 (rounds to decimal)
            timeDiffInHours = Math.round(timeDiffInHours * 100.0) / 100.0
        }

        val timeArray = fixTime(timeDiffInHours)

        hourDiff = timeArray[0].toInt()
        minDiff = timeArray[1].toInt()

        sharedPrefs.edit().putString("fixedTime0", timeArray?.get(0)).apply()
        sharedPrefs.edit().putString("fixedTime1", timeArray?.get(1)).apply()

        txt.updateText("${timeArray[0]}:${timeArray[1]}")

        timeSetAtInMins = CurrentTime().currentTimeInMinutes
    }

}