package com.syntapps.time2sleep

import ProgressBarAnimation
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.syntapps.time2sleep.databinding.ActivityMain2Binding
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_main2.*


open class MainActivity2 : AppCompatActivity(), CompoundButton.OnCheckedChangeListener,
    View.OnClickListener {

    private lateinit var binding: ActivityMain2Binding
    private val TAG = "MainActivity2_log"

    private lateinit var spotifySwitchObject: SwitchObject
    private lateinit var youtubeSwitchObject: SwitchObject
    private lateinit var netflixSwitchObject: SwitchObject
    private lateinit var airplaneModeSwitchObject: SwitchObject
    open lateinit var txtV: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

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

    }

    // TODO: 18/12/2020 save timer to shared prefs and set when app opens

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        when (buttonView) {
            binding.spotifySwitch -> spotifySwitchObject.switchClicked(isChecked)
            binding.youtubeSwitch -> youtubeSwitchObject.switchClicked(isChecked)
            binding.netflixSwitch -> netflixSwitchObject.switchClicked(isChecked)
            binding.airplaneModeSwitch -> airplaneModeSwitchObject.switchClicked(isChecked)
        }

    }

    override fun onClick(v: View?) {
        when (v) {
            binding.newTimerButton -> {
                showTimePickerDialog()
            }
            binding.resetTimerButton -> Toasty.info(this, "Reset Timer", Toast.LENGTH_SHORT, true)
                .show()
        }
    }

    private fun showTimePickerDialog() {
        val txt = object : MyCallback {
            override fun updateText(string: String) {
                binding.timeLeftTV.text = string
            }

            override fun updateProgressBar(progressPercentage: Int) {
                val anim = ProgressBarAnimation(binding.progressBar,progressBar.progress.toFloat(), progressPercentage.toFloat())
                anim.duration = 1000
                binding.progressBar.startAnimation(anim)
            }

        }
        TimePickerFragment(txt,this).show(supportFragmentManager, "timePicker")
    }
}