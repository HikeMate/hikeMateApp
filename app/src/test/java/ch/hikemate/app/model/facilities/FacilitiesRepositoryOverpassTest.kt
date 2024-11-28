package ch.hikemate.app.model.facilities

import android.util.Log
import ch.hikemate.app.model.route.Bounds
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

// Important: We need to use RobolectricTestRunner to run tests that use Android classes
// such as JsonReader, which is used in the actual implementation of the app.
@RunWith(RobolectricTestRunner::class)
class FacilitiesRepositoryOverpassTest {

  companion object {

    // Valid JSON Header in JSON Format. Only Valid when the response is closed by
    // VALID_RESPONSE_HEADER_CLOSER
    private const val VALID_RESPONSE_HEADER =
        """
      {
        "version": 0.6,
        "generator": "Overpass API 0.7.62.1 084b4234",
        "osm3s": {
          "timestamp_osm_base": "2024-11-21T17:01:26Z",
          "copyright": "The data included in this document is from www.openstreetmap.org. The data is made available under ODbL."
        },
        "elements": ["""

    // Valid JSON Footer in JSON Format. Only Valid when the response is opened by
    // VALID_RESPONSE_HEADER
    private const val VALID_RESPONSE_FOOTER = """
      ]
      }"""

    // Single, valid amenity element in JSON format
    private const val VALID_RESPONSE_ELEMENT =
        """
      {
        "type": "node",
        "id": 3138275148,
        "lat": 51.0341166,
        "lon": 7.0464572,
        "tags": {
          "amenity": "toilets"
        }
      }"""

    private const val VALID_RESPONSE_ELEMENT_2 =
        """
      {
        "type": "node",
        "id": 313827518,
        "lat": 50.9620,
        "lon": 5.12740,
        "tags": {
          "amenity": "parking"
        }
      }"""
  }

  private lateinit var client: OkHttpClient
  private lateinit var mockCall: Call
  private lateinit var facilitiesRepository: FacilitiesRepositoryOverpass

  private val callbackCaptor = argumentCaptor<okhttp3.Callback>()

  private val testBoundsNormal = Bounds(46.5, 6.6, 46.51, 6.62)

  private fun buildResponse(request: String): Response {
    return Response.Builder()
        .code(200)
        .message("OK")
        .body(request.trimIndent().replace("\n", "").toResponseBody())
        .protocol(Protocol.HTTP_1_1)
        .header("Content-Type", "application/json")
        .request(mock())
        .build()
  }

  private fun setupMockResponse(response: Response) {
    `when`(mockCall.enqueue(callbackCaptor.capture())).then {
      callbackCaptor.firstValue.onResponse(mockCall, response)
    }
  }

  private fun setupNetworkFailure() {
    `when`(mockCall.enqueue(callbackCaptor.capture())).then {
      callbackCaptor.firstValue.onFailure(mockCall, IOException("Network failure"))
    }
  }

  @Before
  fun setUp() {
    client = mock()
    mockCall = mock()

    facilitiesRepository = FacilitiesRepositoryOverpass(client)

    `when`(client.newCall(any())).thenReturn(mockCall)
  }

  @Test
  fun testGetFacilities_callsClient() {
    `when`(client.newCall(any())).thenReturn(mock())
    facilitiesRepository.getFacilities(
        testBoundsNormal, { routes -> assertNotEquals(routes.size, 0) }) {
          fail("Failed to fetch from Overpass API")
        }
    verify(client).newCall(any())
  }

  @Test
  fun testGetFacilities_andFilterAmenitiesWithNormalInput() {

    val response =
        buildResponse(VALID_RESPONSE_HEADER + VALID_RESPONSE_ELEMENT + VALID_RESPONSE_FOOTER)

    val callbackCaptor = argumentCaptor<okhttp3.Callback>()
    val latch = CountDownLatch(1)
    var onSuccessCalled = false

    setupMockResponse(response)

    facilitiesRepository.getFacilities(
        bounds = testBoundsNormal,
        onSuccess = { facilities ->
          assertEquals(1, facilities.size)
          assertEquals(FacilityType.TOILETS, facilities[0].type)
          onSuccessCalled = true
          latch.countDown()
          Log.d(
              "FacilitiesRepositoryTest",
              "getFacilities with normal parameters succeeded. Got: ${facilities.size} facilities")
        },
        onFailure = { fail("getFacilities call failed") })

    verify(mockCall).enqueue(callbackCaptor.capture())

    assert(latch.await(5, TimeUnit.SECONDS)) { "Test timed out" }

    assertTrue(onSuccessCalled)
  }

  @Test
  fun testGetFacilitiesWithMultipleAmenities() {
    val response =
        buildResponse(
            VALID_RESPONSE_HEADER +
                VALID_RESPONSE_ELEMENT +
                "," +
                VALID_RESPONSE_ELEMENT_2 +
                VALID_RESPONSE_FOOTER)

    val callbackCaptor = argumentCaptor<okhttp3.Callback>()
    val latch = CountDownLatch(1)
    var onSuccessCalled = false

    setupMockResponse(response)

    facilitiesRepository.getFacilities(
        bounds = testBoundsNormal,
        onSuccess = { facilities ->
          assertEquals(2, facilities.size)
          assertEquals(FacilityType.TOILETS, facilities[0].type)
          assertEquals(FacilityType.PARKING, facilities[1].type)
          onSuccessCalled = true
          latch.countDown()
        },
        onFailure = { fail("Should not fail") })

    verify(mockCall).enqueue(callbackCaptor.capture())

    assert(latch.await(5, TimeUnit.SECONDS)) { "Test timed out" }

    assertTrue(onSuccessCalled)
  }

  @Test
  fun testGetFacilitiesWithTruncatedResponse() {
    val response =
        buildResponse(
            VALID_RESPONSE_HEADER +
                VALID_RESPONSE_ELEMENT +
                """
            {
            "type": "node",
            "id": 992442027,
            "lat": 46.5098337,
            """ // This node is truncated
                +
                VALID_RESPONSE_FOOTER)

    setupMockResponse(response)

    val failureCounter = AtomicInteger(0)

    facilitiesRepository.getFacilities(
        bounds = testBoundsNormal,
        onSuccess = { _ -> fail("Should fail") },
        onFailure = { _ ->
          Log.d(
              "FacilitiesRepositoryTest",
              "getFacilities with truncated response failed as expected")
          failureCounter.incrementAndGet()
        })

    verify(mockCall).enqueue(callbackCaptor.capture())
    assertEquals(1, failureCounter.get())
  }

  @Test
  fun testGetFacilitiesWithEmptyResponse() {
    val response =
        buildResponse(
            VALID_RESPONSE_HEADER +
                VALID_RESPONSE_FOOTER) // There are no nodes returned in this response

    setupMockResponse(response)

    var onSuccessCalled = false

    facilitiesRepository.getFacilities(
        bounds = testBoundsNormal,
        onSuccess = { facilities ->
          assertEquals(0, facilities.size)
          onSuccessCalled = true
        },
        onFailure = { _ -> fail("getFacilities call with empty response failed") })

    verify(mockCall).enqueue(callbackCaptor.capture())
    assertTrue(onSuccessCalled)
  }

  @Test
  fun testGetFacilitiesNoServerResponse() {
    val failureCounter = AtomicInteger(0)

    setupNetworkFailure()

    facilitiesRepository.getFacilities(
        bounds = testBoundsNormal,
        onSuccess = { fail("Should fail") },
        onFailure = { failureCounter.incrementAndGet() })

    assertEquals(1, failureCounter.get())
  }

  // ----- filterAmenities() tests -----

  @Test
  fun testAmenityFilter_faultyAmenity_faultyValueInAmenityElement() {
    val response =
        buildResponse(
            VALID_RESPONSE_HEADER +
                VALID_RESPONSE_ELEMENT +
                """"
            {
            "type": "node",
            "id": 992442066,
            "lat": one,
            "lon": 6.6196272,
            "tags": {
              "amenity": "bench",
              "backrest": "yes",
              "check_date": "2021-05-25",
              "source": "survey"
            },
            """ +
                VALID_RESPONSE_FOOTER) // lat is "one", instead of a number
    setupMockResponse(response)

    val failureCounter = AtomicInteger(0)

    facilitiesRepository.getFacilities(
        bounds = testBoundsNormal,
        onSuccess = { _ ->
          fail("getFacilities call with broken response succeeded, but it should fail")
        },
        onFailure = { _ -> failureCounter.incrementAndGet() })

    verify(mockCall).enqueue(callbackCaptor.capture())
    assertEquals(1, failureCounter.get())
  }

  @Test
  fun testAmenityFilter_faultyAmenity_missingValueFromAmenityElement() {
    val response =
        buildResponse(
            VALID_RESPONSE_HEADER +
                VALID_RESPONSE_ELEMENT +
                """
        {
            "type": "node",
            "id": 992442066,
            "lat": 6.6196272,
            "tags": {
                "amenity": "bench",
                "backrest": "yes",
                "check_date": "2021-05-25",
                "source": "survey"
            }
        },""" +
                VALID_RESPONSE_FOOTER) // lon is missing

    setupMockResponse(response)

    val failureCounter = AtomicInteger(0)

    facilitiesRepository.getFacilities(
        bounds = testBoundsNormal,
        onSuccess = { _ ->
          fail("getFacilities call with broken response succeeded, but it should fail")
        },
        onFailure = { _ -> failureCounter.incrementAndGet() })

    verify(mockCall).enqueue(callbackCaptor.capture())
    assertEquals(1, failureCounter.get())
  }

  @Test
  fun testAmenityFilter_faultyAmenity_invalidAmenityType() {
    val response =
        buildResponse(
            VALID_RESPONSE_HEADER +
                """
            {
              "type": "node",
              "id": 269580058,
              "lat": 46.5091848,
              "lon": 6.6194478,
              "tags": {
                "amenity": "bar",
                "fee": "no",
                "unisex": "yes",
                "wheelchair": "yes"
              }
            },""" +
                VALID_RESPONSE_ELEMENT +
                VALID_RESPONSE_FOOTER) // Includes an element with amenity type "bar", which is not
    // a valid facility type
    setupMockResponse(response)

    facilitiesRepository.getFacilities(
        bounds = testBoundsNormal,
        onSuccess = { facilities ->
          assertEquals(facilities.size, 1)
          assertEquals(facilities[0].type, FacilityType.TOILETS)
        },
        onFailure = { fail("Should not fail") })

    verify(mockCall).enqueue(callbackCaptor.capture())
  }
}
