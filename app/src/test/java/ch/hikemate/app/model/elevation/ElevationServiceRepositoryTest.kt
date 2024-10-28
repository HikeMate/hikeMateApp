package ch.hikemate.app.model.elevation

import ch.hikemate.app.model.route.LatLong
import java.io.IOException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ElevationServiceRepositoryTest {
  @Mock private lateinit var client: OkHttpClient
  private lateinit var elevationServiceRepository: ElevationServiceRepository
  private val longList =
      listOf(515.0, 545.0, 117.0, 620.0, 480.0, 200.0, 750.0, 1020.0, 890.0, 1500.0, 340.0, 670.0)
  private val smallList = listOf(515.0, 545.0, 117.0)

  private val latLong =
      listOf(LatLong(10.0, 10.0), LatLong(20.0, 20.0), LatLong(41.161758, -8.583933))

  private val longResponse =
      Response.Builder()
          .code(200)
          .message("OK")
          .body(
              """
{
   "results":
   [
      {
         "longitude": 10.0,
         "elevation": 515,
         "latitude": 10.0
      },
      {
         "longitude": 20.0,
         "elevation": 545,
         "latitude": 20.0
      },
      {
         "latitude": 41.161758,
         "elevation": 117,
         "longitude": -8.583933
      },
      {
         "longitude": 15.5,
         "elevation": 620,
         "latitude": 25.5
      },
      {
         "longitude": 30.0,
         "elevation": 480,
         "latitude": 35.0
      },
      {
         "longitude": -5.0,
         "elevation": 200,
         "latitude": 45.0
      },
      {
         "longitude": 50.0,
         "elevation": 750,
         "latitude": 50.0
      },
      {
         "longitude": 60.5,
         "elevation": 1020,
         "latitude": 60.5
      },
      {
         "longitude": -70.0,
         "elevation": 890,
         "latitude": 40.0
      },
      {
         "longitude": 90.0,
         "elevation": 1500,
         "latitude": 50.0
      },
      {
         "longitude": 100.0,
         "elevation": 340,
         "latitude": 55.0
      },
      {
         "longitude": -120.0,
         "elevation": 670,
         "latitude": 30.0
      }
   ]
}
            """
                  .trimIndent()
                  .replace("\n", "")
                  .toResponseBody("application/json; charset=utf-8".toMediaType()))
          .protocol(Protocol.HTTP_1_1)
          .header("Content-Type", "application/json")
          .request(mock())
          .build()

  private val simpleResponse =
      Response.Builder()
          .code(200)
          .message("OK")
          .body(
              """
{
   "results":
   [
      {
         "longitude":10.0,
         "elevation":515,
         "latitude":10.0
      },
      {
         "longitude":20.0,
         "elevation":545,
         "latitude":20.0
      },
      {
         "latitude":41.161758,
         "elevation":117,
         "longitude":-8.583933
      }
   ]
}
            """
                  .trimIndent()
                  .replace("\n", "")
                  .toResponseBody("application/json; charset=utf-8".toMediaType()))
          .protocol(Protocol.HTTP_1_1)
          .header("Content-Type", "application/json")
          .request(mock())
          .build()

  @Before
  fun setup() {
    MockitoAnnotations.openMocks(this)
    elevationServiceRepository = ElevationServiceRepository(client)
  }

  @Test
  fun worksWithSimpleAnswer() {
    val call = mock(Call::class.java)
    `when`(client.newCall(any())).thenReturn(call)

    val callbackCapture = argumentCaptor<Callback>()

    `when`(call.enqueue(callbackCapture.capture())).then {
      callbackCapture.firstValue.onResponse(call, simpleResponse)
    }

    elevationServiceRepository.getElevation(latLong, 0, { list -> assertEquals(smallList, list) }) {
      fail("Failed to fetch routes from Overpass API")
    }

    verify(client).newCall(any())
  }

  @Test
  fun worksWithLongAnswer() {
    val call = mock(Call::class.java)
    `when`(client.newCall(any())).thenReturn(call)

    val callbackCapture = argumentCaptor<Callback>()

    `when`(call.enqueue(callbackCapture.capture())).then {
      callbackCapture.firstValue.onResponse(call, longResponse)
    }

    elevationServiceRepository.getElevation(latLong, 0, { list -> assertEquals(longList, list) }) {
      fail("Failed to fetch routes from Overpass API")
    }

    verify(client).newCall(any())
  }

  @Test
  fun cacheWorks_toStoreAlreadyQueriedIDs() {

    val call = mock(Call::class.java)
    `when`(client.newCall(any())).thenReturn(call)

    val callbackCapture = argumentCaptor<Callback>()

    `when`(call.enqueue(callbackCapture.capture())).then {
      callbackCapture.firstValue.onResponse(call, longResponse)
    }

    assertEquals(0, elevationServiceRepository.getCacheSize())
    elevationServiceRepository.getElevation(latLong, 0, { list -> assertEquals(longList, list) }) {
      fail("Failed to fetch routes from Overpass API")
    }
    assertEquals(1, elevationServiceRepository.getCacheSize())

    val call2 = mock(Call::class.java)
    `when`(client.newCall(any())).thenReturn(call2)

    elevationServiceRepository.getElevation(latLong, 0, { list -> assertEquals(longList, list) }) {
      fail("Failed to fetch routes from Overpass API")
    }
    assertEquals(1, elevationServiceRepository.getCacheSize())
    verify(client).newCall(any())
  }

  @Test
  fun cacheWorks_toStoreDifferentIDs() {
    // Prepare mock responses
    val call = mock(Call::class.java)
    `when`(client.newCall(any())).thenReturn(call)

    val callbackCapture = argumentCaptor<Callback>()

    // Simulate the first API response
    `when`(call.enqueue(callbackCapture.capture())).thenAnswer {
      // Return the mocked longResponse on the first call
      callbackCapture.firstValue.onResponse(call, longResponse)
    }

    // Check initial cache size
    assertEquals(0, elevationServiceRepository.getCacheSize())

    // First API call
    elevationServiceRepository.getElevation(latLong, 0, { list -> assertEquals(longList, list) }) {
      fail("Failed to fetch routes from Overpass API")
    }

    // Check cache size after first call
    assertEquals(1, elevationServiceRepository.getCacheSize())

    // Prepare for second call with a new mock
    val call2 = mock(Call::class.java)
    `when`(client.newCall(any())).thenReturn(call2)

    val callbackCapture2 = argumentCaptor<Callback>()

    // Simulate the second API response
    `when`(call2.enqueue(callbackCapture2.capture())).thenAnswer {
      // Return the same mocked longResponse for the second call
      callbackCapture2.firstValue.onResponse(call2, simpleResponse)
    }

    // Second API call
    elevationServiceRepository.getElevation(latLong, 1, { list -> assertEquals(smallList, list) }) {
      fail("Failed to fetch routes from Overpass API")
    }

    // Verify cache size after second call
    assertEquals(2, elevationServiceRepository.getCacheSize())

    // Verify that newCall was invoked twice
    verify(client, times(2)).newCall(any())
  }

  @Test
  fun failsWithErrorResponse() {
    // Mock an error response
    val responseBody = "".toResponseBody("application/json; charset=utf-8".toMediaType())
    val response =
        Response.Builder()
            .code(500)
            .message("Internal Server Error")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("https://api.open-elevation.com/api/v1/lookup").build())
            .body(responseBody)
            .build()

    // Mock the call
    val call = mock(Call::class.java)
    `when`(client.newCall(any())).thenReturn(call)

    val callbackCapture = argumentCaptor<Callback>()
    `when`(call.enqueue(callbackCapture.capture())).then {
      callbackCapture.firstValue.onResponse(call, response)
    }

    assertThrows(Exception::class.java) {
      elevationServiceRepository.getElevation(
          latLong, 0, { fail("Expected failure but got success") }) { exception ->
            throw exception
          }
    }

    verify(client).newCall(any())
  }

  @Test
  fun emptyWhenNoCoordinates() {
    elevationServiceRepository.getElevation(
        emptyList(), 0, { list -> assertEquals(emptyList<LatLong>(), list) }) {
          fail("Failed to fetch routes from Overpass API")
        }
  }

  @Test
  fun failsWithIOException() {
    // Simulate an IOException
    val call = mock(Call::class.java)
    `when`(call.enqueue(any())).thenAnswer {
      val callback = it.arguments[0] as Callback
      callback.onFailure(call, IOException("Network error"))
    }
    `when`(client.newCall(any())).thenReturn(call)

    assertThrows("Failed to get elevation", IOException::class.java) {
      elevationServiceRepository.getElevation(
          latLong, 0, { fail("Expected failure but got success") }) { exception ->
            throw exception
          }
    }

    verify(client).newCall(any())
  }
}
