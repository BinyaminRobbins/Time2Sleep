package com.syntapps.time2sleep

import android.content.Context
import android.view.View
import android.widget.Switch
import android.widget.Toast
import es.dmoral.toasty.Toasty
import java.io.Serializable

class SwitchObject(private var context: Context, private var title: String?) : Serializable {
    fun switchClicked(isChecked: Boolean) {
        if (isChecked) {
            Toasty.success(
                context,
                "$title Timer turned ON",
               Toasty.LENGTH_SHORT,
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