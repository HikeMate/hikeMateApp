package ch.hikemate.app.model.route

import java.io.IOException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
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

  private val latLong =
      listOf(LatLong(10.0, 10.0), LatLong(20.0, 20.0), LatLong(41.161758, -8.583933))

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

    val callbackCapture = argumentCaptor<okhttp3.Callback>()

    `when`(call.enqueue(callbackCapture.capture())).then {
      callbackCapture.firstValue.onResponse(call, simpleResponse)
    }

    elevationServiceRepository.getElevation(
        latLong,
        { list ->
          assertEquals(listOf(515.0, 545.0, 117.0), list)
          println(list)
        }) {
          fail("Failed to fetch routes from Overpass API")
        }

    verify(client).newCall(any())
  }

  @Test
  fun failsWithErrorResponse() {
    // Mock an error response
    val responseBody = ResponseBody.create("application/json; charset=utf-8".toMediaType(), "")
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
    `when`(call.execute()).thenReturn(response)
    `when`(client.newCall(any())).thenReturn(call)

    elevationServiceRepository.getElevation(
        latLong, { fail("Expected failure but got success") }) { exception ->
          assertEquals("Failed to get elevation", exception.message)
        }

    verify(client).newCall(any())
  }

  @Test
  fun failsWithNoCoordinates() {
    elevationServiceRepository.getElevation(
        emptyList(), { fail("Expected failure but got success") }) { exception ->
          assertEquals("No coordinates provided", exception.message)
        }

    verify(client, never()).newCall(any())
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

    elevationServiceRepository.getElevation(
        latLong, { fail("Expected failure but got success") }) { exception ->
          assertEquals("Network error", exception.message)
        }

    verify(client).newCall(any())
  }
}
