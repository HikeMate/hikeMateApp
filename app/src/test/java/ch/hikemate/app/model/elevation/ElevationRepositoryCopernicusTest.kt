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
class ElevationRepositoryCopernicusTest {
  private lateinit var client: OkHttpClient
  private lateinit var elevationRepositoryCopernicus: ElevationRepositoryCopernicus

  private val dispatcher = UnconfinedTestDispatcher()

  private val simpleList = listOf(515.0, 545.0, 117.0)

  private val simpleLatLongList =
      listOf(LatLong(10.1, 10.1), LatLong(20.1, 20.1), LatLong(41.261758, -8.683933))

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

  private val chunkSizedList: List<LatLong> = run {
    val list = mutableListOf<LatLong>()
    for (i in 0 until ElevationRepositoryCopernicus.MAX_COORDINATES_PER_REQUEST) {
      list.add(
          LatLong(
              i.toDouble() / ElevationRepositoryCopernicus.MAX_COORDINATES_PER_REQUEST,
              i.toDouble() / ElevationRepositoryCopernicus.MAX_COORDINATES_PER_REQUEST))
    }
    list
  }

  private val chunkSizedResponse =
      Response.Builder()
          .code(200)
          .message("OK")
          .body(
              """
    {
      "elevations": [
        ${chunkSizedList.joinToString(",") { "${it.lat}" }}
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

  private val twiceChunkSizedList: List<LatLong> = run {
    val list = mutableListOf<LatLong>()
    val size = ElevationRepositoryCopernicus.MAX_COORDINATES_PER_REQUEST * 2
    for (i in 0 until size) {
      list.add(LatLong(i.toDouble() / size, i.toDouble() / size))
    }
    list
  }

  private val twiceChunkSizedResponsePart1 =
      Response.Builder()
          .code(200)
          .message("OK")
          .body(
              """
    {
      "elevations": [
        ${twiceChunkSizedList.take(ElevationRepositoryCopernicus.MAX_COORDINATES_PER_REQUEST).joinToString(",") { "${it.lat}" }}
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

  private val twiceChunkSizedResponsePart2 =
      Response.Builder()
          .code(200)
          .message("OK")
          .body(
              """
    {
      "elevations": [
        ${twiceChunkSizedList.drop(ElevationRepositoryCopernicus.MAX_COORDINATES_PER_REQUEST).joinToString(",") { "${it.lat}" }}
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

  @Before
  fun setup() {
    Dispatchers.setMain(dispatcher)
    client = mockk()
    // We test with a small cache size to make sure the cache is working correctly
    elevationRepositoryCopernicus = ElevationRepositoryCopernicus(client, dispatcher)
  }

  @Test
  fun worksWithMultipleRequestsOnMultipleChunks() =
      runTest(timeout = 5.seconds) {
        val call: Call = mockk()
        coEvery { client.newCall(any()) } returns call

        coEvery { call.enqueue(any()) } answers
            {
              firstArg<Callback>().onResponse(call, chunkSizedResponse)
            } andThenAnswer
            {
              firstArg<Callback>().onResponse(call, simpleResponse)
            }

        val onSuccessLatch = CountDownLatch(2)

        elevationRepositoryCopernicus.getElevation(
            chunkSizedList,
            { list ->
              assertEquals(chunkSizedList.map { it.lat }, list)
              onSuccessLatch.countDown()
            }) {
              fail("Failed to fetch routes from Overpass API")
            }

        elevationRepositoryCopernicus.getElevation(
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
        coVerify(exactly = 2) { call.enqueue(any()) }
      }

  @Test
  fun worksWithOneRequestOnMultipleChunks() =
      runTest(timeout = 5.seconds) {
        val call: Call = mockk()
        coEvery { client.newCall(any()) } returns call

        coEvery { call.enqueue(any()) } answers
            {
              firstArg<Callback>().onResponse(call, twiceChunkSizedResponsePart1)
            } andThenAnswer
            {
              firstArg<Callback>().onResponse(call, twiceChunkSizedResponsePart2)
            }

        val onSuccessLatch = CountDownLatch(1)

        elevationRepositoryCopernicus.getElevation(
            twiceChunkSizedList,
            { list ->
              assertEquals(twiceChunkSizedList.map { it.lat }, list)
              onSuccessLatch.countDown()
            }) {
              fail("Failed to fetch routes from Overpass API")
            }

        // Wait for the coroutine to finish
        advanceUntilIdle()

        assertEquals(0L, onSuccessLatch.count)
        coVerify(exactly = 2) { call.enqueue(any()) }
      }

  @Test
  fun worksWithSameRequestOnMultipleChunksMultipleTimesWithoutDelay() =
      runTest(timeout = 5.seconds) {
        val call: Call = mockk()
        coEvery { client.newCall(any()) } returns call

        coEvery { call.enqueue(any()) } answers
            {
              firstArg<Callback>().onResponse(call, twiceChunkSizedResponsePart1)
            } andThenAnswer
            {
              firstArg<Callback>().onResponse(call, twiceChunkSizedResponsePart2)
            }

        val onSuccessLatch = CountDownLatch(2)

        elevationRepositoryCopernicus.getElevation(
            twiceChunkSizedList,
            { list ->
              assertEquals(twiceChunkSizedList.map { it.lat }, list)
              onSuccessLatch.countDown()
            }) {
              fail("Failed to fetch routes from Overpass API")
            }

        elevationRepositoryCopernicus.getElevation(
            twiceChunkSizedList,
            { list ->
              assertEquals(twiceChunkSizedList.map { it.lat }, list)
              onSuccessLatch.countDown()
            }) {
              fail("Failed to fetch routes from Overpass API")
            }

        // Wait for the coroutine to finish
        advanceUntilIdle()

        assertEquals(0L, onSuccessLatch.count)
        coVerify(exactly = 2) { call.enqueue(any()) }
      }

  @Test
  fun worksWithSameRequestOnMultipleChunksMultipleTimesWithDelay() =
      runTest(timeout = 5.seconds) {
        val call: Call = mockk()
        coEvery { client.newCall(any()) } returns call

        coEvery { call.enqueue(any()) } answers
            {
              firstArg<Callback>().onResponse(call, twiceChunkSizedResponsePart1)
            } andThenAnswer
            {
              firstArg<Callback>().onResponse(call, twiceChunkSizedResponsePart2)
            }

        val onSuccessLatch = CountDownLatch(2)

        elevationRepositoryCopernicus.getElevation(
            twiceChunkSizedList,
            { list ->
              assertEquals(twiceChunkSizedList.map { it.lat }, list)
              onSuccessLatch.countDown()
            }) {
              fail("Failed to fetch routes from Overpass API")
            }

        // Wait for the coroutine to finish
        while (onSuccessLatch.count > 1) {
          advanceUntilIdle()
        }

        elevationRepositoryCopernicus.getElevation(
            twiceChunkSizedList,
            { list ->
              assertEquals(twiceChunkSizedList.map { it.lat }, list)
              onSuccessLatch.countDown()
            }) {
              fail("Failed to fetch routes from Overpass API")
            }

        // Wait for the coroutine to finish
        advanceUntilIdle()

        assertEquals(0L, onSuccessLatch.count)
        coVerify(exactly = 2) { call.enqueue(any()) }
      }
}
