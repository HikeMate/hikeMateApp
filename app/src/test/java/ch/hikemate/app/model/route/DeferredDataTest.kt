package ch.hikemate.app.model.route

import org.junit.Assert.*
import org.junit.Test

class DeferredDataTest {
  private val data: String = "Hello world"

  private val notRequestedData: DeferredData<String> = DeferredData.NotRequested
  private val requestedData: DeferredData<String> = DeferredData.Requested
  private val obtainedData: DeferredData<String> = DeferredData.Obtained(data = data)

  @Test
  fun obtainedWorksAsExpected() {
    assertFalse(notRequestedData.obtained())
    assertFalse(requestedData.obtained())
    assertTrue(obtainedData.obtained())
  }

  @Test
  fun getOrNullWorksAsExpected() {
    assertNull(notRequestedData.getOrNull())
    assertNull(requestedData.getOrNull())
    assertNotNull(obtainedData.getOrNull())
  }

  @Test
  fun getOrThrowWorksAsExpected() {
    assertThrows(IllegalStateException::class.java) { notRequestedData.getOrThrow() }
    assertThrows(IllegalStateException::class.java) { requestedData.getOrThrow() }
    assertEquals(data, obtainedData.getOrThrow())
  }
}
