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
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify

class FacilitiesRepositoryTest {

  private lateinit var mockClient: OkHttpClient
  private lateinit var mockCall: Call
  private lateinit var facilitiesRepository: FacilitiesRepository

  private val callbackCaptor = argumentCaptor<okhttp3.Callback>()

  private val testBoundsNormal = Bounds(46.5, 6.6, 46.51, 6.62)

  private fun buildResponse(request: String): Response {
    return Response.Builder()
        .code(200)
        .message("OK")
        .body(request.toResponseBody())
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

  @Before
  fun setUp() {
    mockClient = mock()
    mockCall = mock()

    facilitiesRepository = FacilitiesRepository(mockClient)

    `when`(mockClient.newCall(any())).thenReturn(mockCall)
  }

  @Test
  fun getRoutes_callsClient() {
    `when`(mockClient.newCall(any())).thenReturn(mock())
    facilitiesRepository.getFacilities(
        testBoundsNormal,
        { routes ->
          assertNotEquals(routes.size, 0)
          print(routes)
        }) {
          fail("Failed to fetch from Overpass API")
        }
    verify(mockClient).newCall(any())
  }

  @Test
  fun testGetFacilities_andFilterAmenitiesWithNormalInput() {

    val response =
        Response.Builder()
            .code(200)
            .message("OK")
            .body(
                """{
  "version": 0.6,
  "generator": "Overpass API 0.7.62.1 084b4234",
  "osm3s": {
    "timestamp_osm_base": "2024-11-20T15:14:50Z",
    "copyright": "The data included in this document is from www.openstreetmap.org. The data is made available under ODbL."
  },
  "elements": [

{
  "type": "node",
  "id": 269580058,
  "lat": 46.5091848,
  "lon": 6.6194478,
  "tags": {
    "amenity": "toilets",
    "fee": "no",
    "unisex": "yes",
    "wheelchair": "yes"
  }
}
  ]
}"""
                    .toResponseBody())
            .protocol(Protocol.HTTP_1_1)
            .header("Content-Type", "application/json")
            .request(mock())
            .build()

    val callbackCaptor = argumentCaptor<okhttp3.Callback>()
    val latch = CountDownLatch(1)

    setupMockResponse(response)

    facilitiesRepository.getFacilities(
        bounds = testBoundsNormal,
        onSuccess = { facilities ->
          assertEquals(1, facilities.size)
          assertEquals(FacilityType.TOILETS, facilities[0].type)
          latch.countDown()
          Log.d(
              "FacilitiesRepositoryTest",
              "getFacilities with normal parameters succeeded. Got: ${facilities.size} facilities")
        },
        onFailure = { fail("getFacilities call failed") })

    verify(mockCall).enqueue(callbackCaptor.capture())

    assert(latch.await(5, TimeUnit.SECONDS)) { "Test timed out" }
  }

  @Test
  fun testGetFacilitiesWithTruncatedResponse() {
    val response =
        Response.Builder()
            .code(200)
            .message("OK")
            .body(
                """
          {
  "version": 0.6,
  "generator": "Overpass API 0.7.62.1 084b4234",
  "osm3s": {
    "timestamp_osm_base": "2024-11-20T17:56:17Z",
    "copyright": "The data included in this document is from www.openstreetmap.org. The data is made available under ODbL."
  },
  "elements": [

{
  "type": "node",
  "id": 269580058,
  "lat": 46.5091848,
  "lon": 6.6194478,
  "tags": {
    "amenity": "toilets",
    "fee": "no",
    "unisex": "yes",
    "wheelchair": "yes"
  }
},
{
  "type": "node",
  "id": 992442027,
  "lat": 46.5098337,
"""
                    .toResponseBody())
            .protocol(Protocol.HTTP_1_1)
            .header("Content-Type", "application/json")
            .request(mock())
            .build()

    setupMockResponse(response)

    val failureCounter = AtomicInteger(0)

    facilitiesRepository.getFacilities(
        bounds = testBoundsNormal,
        onSuccess = { _ ->
          fail("getFacilities call with empty response succeeded, but it should fail")
        },
        onFailure = { _ ->
          Log.d("FacilitiesRepositoryTest", "getFacilities with empty response failed as expected")
          failureCounter.incrementAndGet()
        })

    verify(mockCall).enqueue(callbackCaptor.capture())
    assertEquals(1, failureCounter.get())
  }

  @Test
  fun testGetFacilitiesWithEmptyResponse() {
    val response =
        Response.Builder()
            .code(200)
            .message("OK")
            .body(
                """
          {
  "version": 0.6,
  "generator": "Overpass API 0.7.62.1 084b4234",
  "osm3s": {
    "timestamp_osm_base": "2024-11-20T17:56:17Z",
    "copyright": "The data included in this document is from www.openstreetmap.org. The data is made available under ODbL."
  },
  "elements": []
  }"""
                    .toResponseBody())
            .protocol(Protocol.HTTP_1_1)
            .header("Content-Type", "application/json")
            .request(mock())
            .build()

    setupMockResponse(response)

    facilitiesRepository.getFacilities(
        bounds = testBoundsNormal,
        onSuccess = { facilities -> assertEquals(0, facilities.size) },
        onFailure = { _ -> fail("getFacilities call with empty response failed") })

    verify(mockCall).enqueue(callbackCaptor.capture())
  }

  @Test
  fun testGetFacilitiesNoServerResponse() {
    val failureCounter = AtomicInteger(0)

    `when`(mockCall.enqueue(callbackCaptor.capture())).then {
      callbackCaptor.firstValue.onFailure(
          mockCall, IOException("Failed to fetch routes from Overpass API"))
    }

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
            """{
  "version": 0.6,
  "generator": "Overpass API 0.7.62.1 084b4234",
  "osm3s": {
    "timestamp_osm_base": "2024-11-20T15:14:50Z",
    "copyright": "The data included in this document is from www.openstreetmap.org. The data is made available under ODbL."
  },
  "elements": [

{
  "type": "node",
  "id": 269580058,
  "lat": 46.5091848,
  "lon": 6.6194478,
  "tags": {
    "amenity": "toilets",
    "fee": "no",
    "unisex": "yes",
    "wheelchair": "yes"
  }
},
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
  }
  ]
}""")
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
            """{
    "version": 0.6,
    "generator": "Overpass API 0.7.62.1 084b4234",
    "osm3s": {
        "timestamp_osm_base": "2024-11-20T15:14:50Z",
        "copyright": "The data included in this document is from www.openstreetmap.org. The data is made available under ODbL."
    },
    "elements": [
        {
            "type": "node",
            "id": 269580058,
            "lat": 46.5091848,
            "lon": 6.6194478,
            "tags": {
                "amenity": "toilets",
                "fee": "no",
                "unisex": "yes",
                "wheelchair": "yes"
            }
        },
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
        }
    ]
})""")

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
            """{
  "version": 0.6,
  "generator": "Overpass API 0.7.62.1 084b4234",
  "osm3s": {
    "timestamp_osm_base": "2024-11-20T15:14:50Z",
    "copyright": "The data included in this document is from www.openstreetmap.org. The data is made available under ODbL."
  },
  "elements": [

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
},
{
  "type": "node",
  "id": 992442066,
  "lat": 6.6196272,
  "lon": 6.6196272,
  "tags": {
    "amenity": "bench",
    "backrest": "yes",
    "check_date": "2021-05-25",
    "source": "survey"
  }
  ]
}""")
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
}
