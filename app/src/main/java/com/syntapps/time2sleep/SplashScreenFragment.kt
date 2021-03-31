package com.syntapps.time2sleep

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment


class SplashScreenFragment(private val myContext: Context) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_splash_screen_layout,
            container, false
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Handler().postDelayed(
            {
                // Create new fragment and transaction then replace()
                val newFragment: Fragment = HomeFragment(myContext)
                val transaction = fragmentManager!!.beginTransaction()
                transaction.replace(R.id.frameLayout, newFragment)
                transaction.commit()
            }, 500
        )
    }
}