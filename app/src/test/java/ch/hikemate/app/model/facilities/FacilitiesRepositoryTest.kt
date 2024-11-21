package ch.hikemate.app.model.facilities

import android.util.Log
import ch.hikemate.app.model.route.Bounds
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
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
  private lateinit var mockResponse: Response
  private lateinit var mockOnSuccess: (List<Facility>) -> Unit
  private lateinit var mockOnFailure: (Exception) -> Unit
  private lateinit var facilitiesRepository: FacilitiesRepository

  @Before
  fun setUp() {
    mockClient = mock()
    mockCall = mock()
    mockResponse = mock()
    mockOnSuccess = mock()
    mockOnFailure = mock()
    facilitiesRepository = FacilitiesRepository(mockClient)

    `when`(mockClient.newCall(any())).thenReturn(mockCall)
  }

  @Test
  fun testGetFacilities() {
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
},
{
  "type": "node",
  "id": 992442027,
  "lat": 46.5098337,
  "lon": 6.6197940,
  "tags": {
    "amenity": "bench",
    "backrest": "yes",
    "check_date": "2021-05-25",
    "source": "survey"
  }
},
{
  "type": "node",
  "id": 992442066,
  "lat": 46.5099208,
  "lon": 6.6196272,
  "tags": {
    "amenity": "bench",
    "backrest": "yes",
    "check_date": "2021-05-25",
    "source": "survey"
  }
},
{
  "type": "node",
  "id": 992442092,
  "lat": 46.5098699,
  "lon": 6.6197201,
  "tags": {
    "amenity": "bench",
    "backrest": "yes",
    "check_date": "2021-05-25",
    "source": "survey"
  }
},
{
  "type": "node",
  "id": 3687479917,
  "lat": 46.5087671,
  "lon": 6.6163743,
  "tags": {
    "addr:street": "Jetée de la Compagnie",
    "amenity": "bar",
    "cuisine": "regional",
    "name": "Jetée de la compagnie",
    "opening_hours": "Mo-Sa 10:00-24:00, Su 09:00-24:00",
    "operator": "I lake Lausanne",
    "website": "http://www.jeteedelacompagnie.ch/"
  }
},
{
  "type": "node",
  "id": 5680464971,
  "lat": 46.5083383,
  "lon": 6.6170754,
  "tags": {
    "addr:housenumber": "5",
    "addr:street": "Jetée-de-la-Compagnie",
    "amenity": "bar",
    "email": "leminimumbar@gmail.com",
    "name": "Le Minimum",
    "opening_hours": "Mo-Su 12:00-00:00"
  }
}

  ]
}"""
                    .trimIndent()
                    .replace("\n", "")
                    .toResponseBody())
            .protocol(Protocol.HTTP_1_1)
            .header("Content-Type", "application/json")
            .request(mock())
            .build()

    `when`(mockCall.execute()).thenReturn(response)

    val callbackCaptor = argumentCaptor<okhttp3.Callback>()
    val latch = CountDownLatch(1)

    val bounds = Bounds(46.5, 6.6, 46.51, 6.62)
    var resultFacilities = emptyList<Facility>()

    facilitiesRepository.getFacilities(
        bounds = bounds,
        onSuccess = { facilities ->
          assertEquals(6, facilities.size)
          assertEquals(FacilityType.TOILETS, facilities[0].type)
          latch.countDown()
          Log.d(
              "FacilitiesRepositoryTest",
              "getFacilities with normal parameters succeeded. Got: ${facilities.size} facilities")
        },
        onFailure = { fail("getFacilities call with normal parameters failed") })

    verify(mockCall).enqueue(callbackCaptor.capture())
    callbackCaptor.firstValue.onResponse(mockCall, response)

    assert(latch.await(5, TimeUnit.SECONDS)) { "Test timed out" }
  }
}
