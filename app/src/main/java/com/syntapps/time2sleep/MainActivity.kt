package com.syntapps.time2sleep

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)

        // window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // supportActionBar?.hide()
        Handler().postDelayed({
            val intent = Intent(applicationContext, MainActivity2::class.java)
            startActivity(intent)
            finish()
        }, 500)
    }
}