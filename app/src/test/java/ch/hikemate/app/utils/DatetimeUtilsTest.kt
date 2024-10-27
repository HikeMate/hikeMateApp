package ch.hikemate.app.utils

import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test

private data class TestDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val expectedSeconds: Long,
    val expectedNanoSeconds: Int
)

class DatetimeUtilsTest {
  @Test
  fun timestampFromYearMonthDayProvidesCorrectTimestamps() {
    // Given
    val tests: List<TestDate> =
        listOf(
            // This test data was generated using https://www.epochconverter.com/
            TestDate(2020, 3, 13, 1584057600, 0),
            TestDate(2021, 5, 4, 1620086400, 0),
            TestDate(2022, 7, 15, 1657843200, 0),
            TestDate(2023, 9, 26, 1695686400, 0),
            TestDate(2024, 11, 7, 1730937600, 0),
            TestDate(2025, 12, 18, 1766016000, 0))

    for (test in tests) {
      // When
      val timestamp = Timestamp.from(test.year, test.month, test.day)

      if (timestamp.seconds != test.expectedSeconds ||
          timestamp.nanoseconds != test.expectedNanoSeconds) {
        var message = "Timestamp for ${test.year}-${test.month}-${test.day} is incorrect.\n\n"
        message += "Expected: ${test.expectedSeconds}s ${test.expectedNanoSeconds}ns\n"
        message += "Actual: ${timestamp.seconds}s ${timestamp.nanoseconds}ns"
        fail(message)
      }
    }
  }
}
