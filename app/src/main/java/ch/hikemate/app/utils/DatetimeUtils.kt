package ch.hikemate.app.utils

import com.google.firebase.Timestamp
import java.time.LocalDateTime
import java.time.ZoneOffset

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
