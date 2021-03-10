package com.syntapps.time2sleep

interface MyCallback {
    fun updateText(string: String)
    fun updateProgressBar(timePassedInMinutes: Int, timeDifferenceInMins: Int, overrideSetNum: Int?)
}