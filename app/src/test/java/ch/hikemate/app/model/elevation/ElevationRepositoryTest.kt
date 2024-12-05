package ch.hikemate.app.model.elevation

import ch.hikemate.app.model.route.LatLong
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
  private lateinit var elevationServiceRepository: ElevationServiceRepository
  private val longList =
      listOf(515.0, 545.0, 117.0, 620.0, 480.0, 200.0, 750.0, 1020.0, 890.0, 1500.0, 340.0, 670.0)
  private val smallList = listOf(515.0, 545.0, 117.0)

  private val latLongList =
      listOf(LatLong(10.0, 10.0), LatLong(20.0, 20.0), LatLong(41.161758, -8.583933))

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

  private val requestEntityTooLargeResponse =
      Response.Builder()
          .code(413)
          .message("Request Entity Too Large")
          .body("Request Entity Too Large".toResponseBody(null))
          .protocol(Protocol.HTTP_1_1)
          .header("Content-Type", "text/plain")
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
    elevationServiceRepository = ElevationServiceRepository(client, dispatcher)
  }

  @Test
  fun worksWithSimpleAnswer() = runTest {
    val call: Call = mockk()
    coEvery { client.newCall(any()) } returns call

    coEvery { call.enqueue(any()) } answers
        {
          firstArg<Callback>().onResponse(call, simpleResponse)
        }

    var onSuccessCalled = false

    elevationServiceRepository.getElevation(
        latLongList,
        { list ->
          assertEquals(smallList, list)
          onSuccessCalled = true
        }) {
          fail("Failed to fetch routes from Overpass API")
        }

    // Wait for the coroutine to finish
    advanceUntilIdle()

    assert(onSuccessCalled)
    coVerify { call.enqueue(any()) }
  }

  @Test
  fun worksWithLongAnswer() = runTest {
    val call: Call = mockk()
    coEvery { client.newCall(any()) } returns call

    coEvery { call.enqueue(any()) } answers { firstArg<Callback>().onResponse(call, longResponse) }

    var onSuccessCalled = false

    elevationServiceRepository.getElevation(
        longLatLongList,
        { list ->
          assertEquals(longList, list)
          onSuccessCalled = true
        }) {
          fail("Failed to fetch routes from Overpass API")
        }

    // Wait for the coroutine to finish
    advanceUntilIdle()

    assert(onSuccessCalled)
    coVerify { call.enqueue(any()) }
  }

  @Test
  fun worksWith413() = runTest {
    val call: Call = mockk()
    coEvery { client.newCall(any()) } returns call

    coEvery { call.enqueue(any()) } answers
        {
          firstArg<Callback>().onResponse(call, requestEntityTooLargeResponse)
        } andThenAnswer
        {
          firstArg<Callback>().onResponse(call, longResponse)
        }

    var onSuccessCalled = false

    elevationServiceRepository.getElevation(
        longLatLongList,
        { list ->
          assertEquals(longList, list)
          onSuccessCalled = true
        }) {
          fail("Should not have failed")
        }

    // Wait for the coroutine to finish
    advanceUntilIdle()

    assert(onSuccessCalled)
    coVerify { call.enqueue(any()) }
  }

  @Test
  fun worksWith500() = runTest {
    val call: Call = mockk()
    coEvery { client.newCall(any()) } returns call

    coEvery { call.enqueue(any()) } answers
        {
          firstArg<Callback>().onResponse(call, serverErrorResponse)
        } andThenAnswer
        {
          firstArg<Callback>().onResponse(call, longResponse)
        }

    var onSuccessCalled = false

    elevationServiceRepository.getElevation(
        longLatLongList,
        { list ->
          assertEquals(longList, list)
          onSuccessCalled = true
        }) {
          fail("Should not have failed")
        }

    // Wait for the coroutine to finish
    advanceUntilIdle()

    assert(onSuccessCalled)
    coVerify { call.enqueue(any()) }
  }
}
