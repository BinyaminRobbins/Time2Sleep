package com.syntapps.time2sleep

import android.content.Context
import android.widget.Toast
import es.dmoral.toasty.Toasty

class SwitchObject(private var context: Context, private var title: String) {


    fun switchClicked(isChecked: Boolean) {
        if (isChecked) {
            Toasty.success(
                context,
                "$title Timer turned ON",
                Toast.LENGTH_SHORT,
                true
            ).show()
        } else {
            Toasty.error(
                context,
                "$title Timer turned OFF",
                Toast.LENGTH_SHORT,
                true
            ).show()
        }

    }
}