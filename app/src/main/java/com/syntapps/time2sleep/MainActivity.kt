package com.syntapps.time2sleep

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)
        super.onCreate(savedInstanceState)

        //transition the first fragment (Splash Screen) into the frame layout of Main Activity XML File

        val firstFragment = SplashScreenFragment(this)
        firstFragment.arguments = intent.extras

        supportFragmentManager.beginTransaction().add(R.id.frameLayout, firstFragment).commit()
    }
}