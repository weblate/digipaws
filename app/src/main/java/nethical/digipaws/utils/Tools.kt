package nethical.digipaws.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Tools {
    companion object {
        fun convertToMinutesFromMidnight(hour: Int, minute: Int): Int {
            return (hour * 60) + minute
        }

        fun convertMinutesTo24Hour(minutes: Int): Pair<Int, Int> {
            return Pair(minutes / 60, minutes % 60)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun getCurrentDate(): String {
            // Get the current date (without time)
            val currentDate = LocalDate.now()

            // Define a formatter for date in "dd MMMM yyyy" format
            val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
            // Convert the date to a human-readable format
            return currentDate.format(formatter)
        }

    }
}