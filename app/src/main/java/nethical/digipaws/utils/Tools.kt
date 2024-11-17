package nethical.digipaws.utils

class Tools {
    companion object {
        fun convertToMinutesFromMidnight(hour: Int, minute: Int): Int {
            return (hour * 60) + minute
        }

        fun convertMinutesTo24Hour(minutes: Int): Pair<Int, Int> {
            return Pair(minutes / 60, minutes % 60)
        }

    }
}