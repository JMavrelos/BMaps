package gr.blackswamp.bmaps.utils

import android.content.Context
import android.text.format.DateFormat
import com.mapbox.services.android.navigation.v5.navigation.NavigationTimeFormat.NONE_SPECIFIED
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.utils.time.TimeFormatter.formatTime
import java.util.*

fun RouteProgress.formatArrivalTime(context: Context): String {
  val time = Calendar.getInstance()
  val isTwentyFourHourFormat = DateFormat.is24HourFormat(context)
  return formatTime(time, durationRemaining(), NONE_SPECIFIED, isTwentyFourHourFormat)
}