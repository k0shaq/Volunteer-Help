package com.example.volunteerhelp.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {
    private val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("uk", "UA"))

    fun format(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return formatter.format(Date(timestamp))
    }
}
