package ch.hikemate.app.model.elevation

import ch.hikemate.app.model.route.LatLong
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.concurrent.CountDownLatch
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ElevationRepositoryTest {
  private lateinit var client: OkHttpClient
  private lateinit var elevationRepository: ElevationRepository
  private val longList =
      listOf(515.0, 545.0, 117.0, 620.0, 480.0, 200.0, 750.0, 1020.0, 890.0, 1500.0, 340.0, 670.0)
  private val simpleList = listOf(515.0, 545.0, 117.0)

  private val simpleLatLongList =
      listOf(LatLong(10.1, 10.1), LatLong(20.1, 20.1), LatLong(41.261758, -8.683933))

  private val longLatLongList =
      listOf(
          LatLong(10.0, 10.0),
          LatLong(20.0, 20.0),
          LatLong(41.161758, -8.583933),
          LatLong(25.5, 15.5),
          LatLong(35.0, 30.0),
          LatLong(45.0, -5.0),
          LatLong(50.0, 50.0),
          LatLong(60.5, 60.5),
          LatLong(40.0, -70.0),
          LatLong(50.0, 90.0),
          LatLong(55.0, 100.0),
          LatLong(30.0, -120.0),
      )

  private val longResponse =
      Response.Builder()
          .code(200)
          .message("OK")
          .body(
              """
{
   "elevations":
   [
      515.0,
      545.0,
      117.0,
      620.0,
      480.0,
      200.0,
      750.0,
      1020.0,
      890.0,
      1500.0,
      340.0,
      670.0
   ]
}
            """
                  .trimIndent()
                  .replace("\n", "")
                  .toResponseBody("application/json; charset=utf-8".toMediaType()))
          .protocol(Protocol.HTTP_1_1)
          .header("Content-Type", "application/json")
          .request(mockk())
          .build()

  private val simpleResponse =
      Response.Builder()
          .code(200)
          .message("OK")
          .body(
              """
{
   "elevations":
   [
      515.0,
      545.0,
      117.0
   ]
}
            """
                  .trimIndent()
                  .replace("\n", "")
                  .toResponseBody("application/json; charset=utf-8".toMediaType()))
          .protocol(Protocol.HTTP_1_1)
          .header("Content-Type", "application/json")
          .request(mockk())
          .build()

  private val simpleAndLongResponse =
      Response.Builder()
          .code(200)
          .message("OK")
          .body(
              """
{
   "elevations":
   [
      515.0,
      545.0,
      117.0,
      515.0,
      545.0,
      117.0,
      620.0,
      480.0,
      200.0,
      750.0,
      1020.0,
      890.0,
      1500.0,
      340.0,
      670.0
   ]
}
            """
                  .trimIndent()
                  .replace("\n", "")
                  .toResponseBody("application/json; charset=utf-8".toMediaType()))
          .protocol(Protocol.HTTP_1_1)
          .header("Content-Type", "application/json")
          .request(mockk())
          .build()

  private val serverErrorResponse =
      Response.Builder()
          .code(500)
          .message("Internal Server Error")
          .body("Internal Server Error".toResponseBody(null))
          .protocol(Protocol.HTTP_1_1)
          .header("Content-Type", "text/plain")
          .request(mockk())
          .build()

  private val dispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(dispatcher)
    client = mockk()
    // We test with a small cache size to make sure the cache is working correctly
    elevationRepository = ElevationRepositoryCopernicus(client, dispatcher, 10)
  }

  @Test
  fun worksWithSimpleAnswer() =
      runTest(timeout = 5.seconds) {
        val call: Call = mockk()
        coEvery { client.newCall(any()) } returns call

        coEvery { call.enqueue(any()) } answers
            {
              firstArg<Callback>().onResponse(call, simpleResponse)
            }

        val onSuccessLatch = CountDownLatch(1)

        elevationRepository.getElevation(
            simpleLatLongList,
            { list ->
              assertEquals(simpleList, list)
              onSuccessLatch.countDown()
            }) {
              fail("Failed to fetch routes from Overpass API")
            }

        // Wait for the coroutine to finish
        advanceUntilIdle()

        assertEquals(0L, onSuccessLatch.count)
        coVerify { call.enqueue(any()) }
      }

  @Test
  fun worksWithLongAnswer() =
      runTest(timeout = 5.seconds) {
        val call: Call = mockk()
        coEvery { client.newCall(any()) } returns call

        coEvery { call.enqueue(any()) } answers
            {
              firstArg<Callback>().onResponse(call, longResponse)
            }

        val onSuccessLatch = CountDownLatch(1)

        elevationRepository.getElevation(
            longLatLongList,
            { list ->
              assertEquals(longList, list)
              onSuccessLatch.countDown()
            }) {
              fail("Failed to fetch routes from Overpass API")
            }

        // Wait for the coroutine to finish
        advanceUntilIdle()

        assertEquals(0L, onSuccessLatch.count)
        coVerify { call.enqueue(any()) }
      }

  @Test
  fun worksWith500() =
      runTest(timeout = 5.seconds) {
        val call: Call = mockk()
        coEvery { client.newCall(any()) } returns call

        coEvery { call.enqueue(any()) } answers
            {
              firstArg<Callback>().onResponse(call, serverErrorResponse)
            } andThenAnswer
            {
              firstArg<Callback>().onResponse(call, longResponse)
            }

        val onSuccessLatch = CountDownLatch(1)

        elevationRepository.getElevation(
            longLatLongList,
            { list ->
              assertEquals(longList, list)
              onSuccessLatch.countDown()
            }) {
              fail("Should not have failed")
            }

        // Wait for the coroutine to finish
        advanceUntilIdle()

        assertEquals(0L, onSuccessLatch.count)
        coVerify { call.enqueue(any()) }
      }

  @Test
  fun keepsReceiving500() =
      runTest(timeout = 5.seconds) {
        val call: Call = mockk()
        coEvery { client.newCall(any()) } returns call

        coEvery { call.enqueue(any()) } answers
            {
              firstArg<Callback>().onResponse(call, serverErrorResponse)
            }

        val onFailureLatch = CountDownLatch(1)

        elevationRepository.getElevation(longLatLongList, { fail("Should not have succeeded") }) {
          onFailureLatch.countDown()
        }

        // Wait for the coroutine to finish
        advanceUntilIdle()

        assertEquals(0L, onFailureLatch.count)
        coVerify { call.enqueue(any()) }
      }

  @Test
  fun worksWithMultipleRequests() =
      runTest(timeout = 5.seconds) {
        val call: Call = mockk()
        coEvery { client.newCall(any()) } returns call

        coEvery { call.enqueue(any()) } answers
            {
              firstArg<Callback>().onResponse(call, simpleAndLongResponse)
            }

        val onSuccessLatch = CountDownLatch(2)

        elevationRepository.getElevation(
            simpleLatLongList,
            { list ->
              assertEquals(simpleList, list)
              onSuccessLatch.countDown()
            }) {
              fail("Failed to fetch routes from Overpass API")
            }

        elevationRepository.getElevation(
            longLatLongList,
            { list ->
              assertEquals(longList, list)
              onSuccessLatch.countDown()
            }) {
              fail("Failed to fetch routes from Overpass API")
            }

        // Wait for the coroutine to finish
        advanceUntilIdle()

        assertEquals(0L, onSuccessLatch.count)
        coVerify(exactly = 1) { call.enqueue(any()) }
      }

  @Test
  fun worksWithRequestsOnMultipleChunks() =
      runTest(timeout = 5.seconds) {
        val call: Call = mockk()
        coEvery { client.newCall(any()) } returns call

        coEvery { call.enqueue(any()) } answers
            {
              firstArg<Callback>().onResponse(call, simpleAndLongResponse)
            }

        val onSuccessLatch = CountDownLatch(2)

        elevationRepository.getElevation(
            simpleLatLongList,
            { list ->
              assertEquals(simpleList, list)
              onSuccessLatch.countDown()
            }) {
              fail("Failed to fetch routes from Overpass API")
            }

        elevationRepository.getElevation(
            longLatLongList,
            { list ->
              assertEquals(longList, list)
              onSuccessLatch.countDown()
            }) {
              fail("Failed to fetch routes from Overpass API")
            }

        // Wait for the coroutine to finish
        advanceUntilIdle()

        assertEquals(0L, onSuccessLatch.count)
        coVerify(exactly = 1) { call.enqueue(any()) }
      }

  @Test
  fun worksWithSameRequestMultipleTime() =
      runTest(timeout = 5.seconds) {
        val call: Call = mockk()
        coEvery { client.newCall(any()) } returns call

        coEvery { call.enqueue(any()) } answers
            {
              firstArg<Callback>().onResponse(call, simpleResponse)
            }

        val onSuccessLatch = CountDownLatch(2)

        elevationRepository.getElevation(
            simpleLatLongList,
            { list ->
              assertEquals(simpleList, list)
              onSuccessLatch.countDown()
            }) {
              fail("Failed to fetch routes")
            }

        elevationRepository.getElevation(
            simpleLatLongList,
            { list ->
              assertEquals(simpleList, list)
              onSuccessLatch.countDown()
            }) {
              fail("Failed to fetch routes")
            }

        // Wait for the coroutine to finish
        advanceUntilIdle()

        assertEquals(0L, onSuccessLatch.count)
        coVerify(exactly = 1) { call.enqueue(any()) }
      }
}
