package ch.hikemate.app.utils

import android.content.Context
import ch.hikemate.app.R
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Create a [Timestamp] from a year, month, and day.
 *
 * For example, from(2022, 1, 1) will create a [Timestamp] for January 1st, 2022.
 *
 * @param year No offset needed (for a date in 2024, use 2024). Needs to be between 1 and 9999 (both
 *   inclusive).
 * @param month Month of the desired timestamp. 1-based (January is 1, February is 2, ...). Needs to
 *   be between 1 and 12 (both included).
 * @param day Day of the desired timestamp. 1-based. Must be within the range of the given month.
 *   Providing an invalid day might result in an unexpected timestamp.
 * @return A [Timestamp] representing the given year, month, and day.
 */
fun Timestamp.Companion.from(year: Int, month: Int, day: Int): Timestamp {
  val localDateTime = LocalDateTime.of(year, month, day, 0, 0)
  val instant = localDateTime.toInstant(ZoneOffset.UTC)
  return Timestamp(instant.epochSecond, instant.nano)
}

fun Timestamp.toLocalDate(): LocalDate {
  return this.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}

fun Timestamp.humanReadableFormat(locale: Locale = Locale.getDefault()): String {
  val asDate = this.toLocalDate()
  val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", locale)
  return asDate.format(formatter)
}

fun Timestamp.humanReadablePlannedLabel(
    context: Context,
    locale: Locale = Locale.getDefault(),
    currentDate: LocalDate = LocalDate.now()
): String {
  val plannedDate = this.toLocalDate()
  val daysDifference =
      Duration.between(currentDate.atStartOfDay(), plannedDate.atStartOfDay()).toDays()

  val formattedDate = this.humanReadableFormat(locale)

  val daysUntilNextSunday = 7 - currentDate.dayOfWeek.value

  return when {
    // Date in the past, for example "Done on 1st of January 2022"
    daysDifference < 0 -> context.getString(R.string.datetime_utils_done_on, formattedDate)
    // Date today, for example "Planned today (18th of October 2024)"
    daysDifference == 0L -> context.getString(R.string.datetime_utils_planned_today, formattedDate)
    // Date tomorrow, for example "Planned tomorrow (19th of October 2024)"
    daysDifference == 1L ->
        context.getString(R.string.datetime_utils_planned_tomorrow, formattedDate)
    // Date before the next Sunday, meaning in the same week, for example "Planned on Sunday (20th
    // of October 2024)"
    daysDifference <= daysUntilNextSunday ->
        context.getString(
            R.string.datetime_utils_planned_on_weekday,
            plannedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
            formattedDate)
    // Date before the Sunday after that, meaning next week, for example "Planned next week (25th of
    // October 2024)"
    daysDifference <= daysUntilNextSunday + 7 ->
        context.getString(R.string.datetime_utils_planned_next_week, formattedDate)
    // Date in the next month, for example "Planned next month (18th of November 2024)"
    plannedDate.year == currentDate.year && plannedDate.month == currentDate.month + 1 ->
        context.getString(R.string.datetime_utils_planned_next_month, formattedDate)
    // Date in the next year, for example "Planned next year (18th of October 2025)"
    plannedDate.year == currentDate.year + 1 ->
        context.getString(R.string.datetime_utils_planned_next_year, formattedDate)
    // Later than next year, for example "Planned on 18th of October 2026"
    else -> context.getString(R.string.datetime_utils_planned_on_date, formattedDate)
  }
}

fun Timestamp.toFormattedString(): String {
  val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
  return dateFormat.format(this.toDate())
}
