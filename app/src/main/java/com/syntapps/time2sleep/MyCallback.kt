package com.syntapps.time2sleep


interface MyCallback {
    fun updateText(string: String, calledFromFunctionName: String? = null)
    fun updateProgressBar(
        timePassedInMinutes: Int,
        timeDifferenceInMins: Int,
        overrideSetPercentage: Int?
    )
}