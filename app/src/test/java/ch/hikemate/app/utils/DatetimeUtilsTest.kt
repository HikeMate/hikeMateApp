package ch.hikemate.app.utils

import android.content.Context
import ch.hikemate.app.R
import com.google.firebase.Timestamp
import java.time.DateTimeException
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

private data class TestDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val expectedSeconds: Long,
    val expectedNanoSeconds: Int,
    val expectedFormattedDate: String? = null,
    val expectedResourceId: Int? = null,
    val expectedResourceArgsCount: Int? = null,
    val expectedWeekday: String? = null
)

class DatetimeUtilsTest {
  private val testDates: List<TestDate> =
      // The timestamps were generated using https://www.epochconverter.com/
      listOf(
          TestDate(2020, 3, 12, 1583971200, 0, "12 March 2020", R.string.datetime_utils_done_on, 1),
          TestDate(
              2020,
              3,
              13,
              1584057600,
              0,
              "13 March 2020",
              R.string.datetime_utils_planned_today,
              1),
          TestDate(
              2020,
              3,
              14,
              1584144000,
              0,
              "14 March 2020",
              R.string.datetime_utils_planned_tomorrow,
              1),
          TestDate(
              2020,
              3,
              15,
              1584230400,
              0,
              "15 March 2020",
              R.string.datetime_utils_planned_on_weekday,
              2,
              "Sunday"),
          TestDate(
              2020,
              3,
              16,
              1584316800,
              0,
              "16 March 2020",
              R.string.datetime_utils_planned_next_week,
              1),
          TestDate(
              2020,
              3,
              17,
              1584403200,
              0,
              "17 March 2020",
              R.string.datetime_utils_planned_next_week,
              1),
          TestDate(
              2020,
              4,
              1,
              1585699200,
              0,
              "1 April 2020",
              R.string.datetime_utils_planned_next_month,
              1),
          TestDate(
              2021,
              5,
              4,
              1620086400,
              0,
              "4 May 2021",
              R.string.datetime_utils_planned_next_year,
              1),
          TestDate(
              2022,
              7,
              15,
              1657843200,
              0,
              "15 July 2022",
              R.string.datetime_utils_planned_on_date,
              1),
          TestDate(2023, 9, 26, 1695686400, 0),
          TestDate(2024, 11, 7, 1730937600, 0),
          TestDate(2025, 12, 18, 1766016000, 0))

  @Test
  fun timestampFromYearMonthDayProvidesCorrectTimestamps() {
    // Given
    // See testDates

    for (test in testDates) {
      // When
      val timestamp = Timestamp.from(test.year, test.month, test.day)

      // Then
      if (timestamp.seconds != test.expectedSeconds ||
          timestamp.nanoseconds != test.expectedNanoSeconds) {
        var message = "Timestamp for ${test.year}-${test.month}-${test.day} is incorrect.\n\n"
        message += "Expected: ${test.expectedSeconds}s ${test.expectedNanoSeconds}ns\n"
        message += "Actual: ${timestamp.seconds}s ${timestamp.nanoseconds}ns"
        fail(message)
      }
    }
  }

  @Test
  fun timestampFromInvalidYearThrows() {
    // Given
    val invalidYears = listOf(-1, 0, 10000)
    val month = 10
    val day = 20

    for (year in invalidYears) {
      assertThrows(IllegalArgumentException::class.java) { Timestamp.from(year, month, day) }
    }
  }

  @Test
  fun timestampFromBorderlineValidYearDoesNotThrow() {
    // Given
    val borderlineYears = listOf(1, 9999)
    val month = 10
    val day = 20

    for (year in borderlineYears) {
      // When
      try {
        Timestamp.from(year, month, day)
      } catch (t: Throwable) {
        // Then
        fail("Expected Timestamp.from($year, $month, $day) to not throw, but got $t")
      }
    }
  }

  @Test
  fun timestampFromInvalidMonthThrows() {
    // Given
    val year = 2020
    val invalidMonths = listOf(-1, 0, 13)
    val day = 20

    for (month in invalidMonths) {
      assertThrows(DateTimeException::class.java) { Timestamp.from(year, month, day) }
    }
  }

  @Test
  fun timestampFromBorderlineMonthDoesNotThrow() {
    // Given
    val year = 2020
    val borderlineMonths = listOf(1, 12)
    val day = 20

    for (month in borderlineMonths) {
      // When
      try {
        Timestamp.from(year, month, day)
      } catch (t: Throwable) {
        // Then
        fail("Expected Timestamp.from($year, $month, $day) to not throw, but got $t")
      }
    }
  }

  @Test
  fun timestampFromInvalidDayThrows() {
    // Given
    val year = 2020
    val month = 10
    val invalidDays = listOf(-1, 0, 32)

    for (day in invalidDays) {
      assertThrows(DateTimeException::class.java) { Timestamp.from(year, month, day) }
    }
  }

  @Test
  fun timestampFromBorderlineDayDoesNotThrow() {
    // Given
    val year = 2020
    val month = 10
    val borderlineDays = listOf(1, 31)

    for (day in borderlineDays) {
      // When
      try {
        Timestamp.from(year, month, day)
      } catch (t: Throwable) {
        // Then
        fail("Expected Timestamp.from($year, $month, $day) to not throw, but got $t")
      }
    }
  }

  @Test
  fun toLocalDateReturnsCorrectLocalDate() {
    // Given
    // See testDates

    for (test in testDates) {
      // When
      val localDate = Timestamp.from(test.year, test.month, test.day).toLocalDate()

      // Then
      assertEquals(test.year, localDate.year)
      assertEquals(test.month, localDate.monthValue)
      assertEquals(test.day, localDate.dayOfMonth)
    }
  }

  @Test
  fun humanReadableFormatReturnsCorrectFormattedDate() {
    // Given
    // See testDates

    for (test in testDates) {
      if (test.expectedFormattedDate == null) {
        continue
      }

      // When
      val formattedDate =
          Timestamp.from(test.year, test.month, test.day)
              .humanReadableFormat(locale = Locale.ENGLISH)

      // Then
      assertEquals(test.expectedFormattedDate, formattedDate)
    }
  }

  @Test
  fun humanReadablePlannedLabelReturnsCorrectLabels() {
    for (test in testDates) {
      // Check whether the date has the expected values for this test
      if (test.expectedResourceId == null ||
          test.expectedResourceArgsCount == null ||
          test.expectedFormattedDate == null ||
          (test.expectedWeekday == null && test.expectedResourceArgsCount == 2)) {
        continue
      }

      // Given
      var message = "Test failed for ${test.year}-${test.month}-${test.day}.\n\n"
      val today = Timestamp.from(2020, 3, 13).toLocalDate()
      val prefix = "Planned "
      val context: Context = mock()
      `when`(context.getString(any(), any())).thenAnswer {
        if (test.expectedResourceArgsCount != 1) {
          message +=
              "Expected ${test.expectedResourceArgsCount} arguments in addition to the resource ID, but got 1."
          fail(message)
        }

        val resourceId = it.getArgument<Int>(0)
        val formatted = it.getArgument<String>(1)

        if (resourceId != test.expectedResourceId) {
          message += "Expected resource ID ${test.expectedResourceId}, but got $resourceId."
          fail(message)
        }

        if (formatted != test.expectedFormattedDate) {
          message += "Expected formatted date ${test.expectedFormattedDate}, but got $formatted."
          fail(message)
        }

        prefix + formatted
      }
      `when`(context.getString(any(), any(), any())).thenAnswer {
        if (test.expectedResourceArgsCount != 2) {
          message +=
              "Expected ${test.expectedResourceArgsCount} arguments in addition to the resource ID, but got 2."
          fail(message)
        }

        val resourceId = it.getArgument<Int>(0)
        val weekday = it.getArgument<String>(1)
        val formatted = it.getArgument<String>(2)

        if (resourceId != test.expectedResourceId) {
          message += "Expected resource ID ${test.expectedResourceId}, but got $resourceId."
          fail(message)
        }

        if (weekday != test.expectedWeekday) {
          message += "Expected weekday ${test.expectedWeekday}, but got $weekday."
          fail(message)
        }

        if (formatted != test.expectedFormattedDate) {
          message += "Expected formatted date ${test.expectedFormattedDate}, but got $formatted."
          fail(message)
        }

        "$prefix$formatted $weekday"
      }

      // When
      val label =
          Timestamp.from(test.year, test.month, test.day)
              .humanReadablePlannedLabel(context, Locale.ENGLISH, today)

      // Then
      val suffix = if (test.expectedResourceArgsCount == 2) " " + test.expectedWeekday else ""
      assertEquals(prefix + test.expectedFormattedDate + suffix, label)
    }
  }
}
