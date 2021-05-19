package com.syntapps.time2sleep

import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)
        super.onCreate(savedInstanceState)

        Log.i(TAG, "onCreate: onCreate()")

        val fragment = HomeFragment(this)
        fragment.arguments = intent.extras
        supportFragmentManager.beginTransaction().add(R.id.frameLayout, fragment)
            .commit()
    }
}