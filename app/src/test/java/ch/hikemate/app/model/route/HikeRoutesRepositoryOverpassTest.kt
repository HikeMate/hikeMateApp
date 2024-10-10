package ch.hikemate.app.model.route

import java.io.IOException
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
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HikeRoutesRepositoryOverpassTest {
  @Mock private lateinit var mockClient: OkHttpClient
  private lateinit var hikingRouteProviderRepositoryOverpass: HikeRoutesRepositoryOverpass
  private val bounds = Bounds(46.52291, 6.55989, 46.51402, 6.58243)

  private val emptyResponse =
      Response.Builder()
          .code(200)
          .message("OK")
          .body(
              """
                {
                  "version": 0.6,
                  "generator": "Overpass API 0.7.62.1 084b4234",
                  "osm3s": {
                    "timestamp_osm_base": "2024-10-10T19:14:42Z",
                    "copyright": "The data included in this document is from www.openstreetmap.org. The data is made available under ODbL."
                  },
                  "elements": []
                }
                  """
                  .trimIndent()
                  .replace("\n", "")
                  .toResponseBody())
          .header("Content-Type", "application/json")
          .protocol(Protocol.HTTP_1_1)
          .request(mock())
          .build()

  private val simpleResponse =
      Response.Builder()
          .code(200)
          .message("OK")
          .body(
              """
        {
    "version": 0.6,
    "generator": "Overpass API 0.7.62.1 084b4234",
    "osm3s": {
        "timestamp_osm_base": "2024-10-10T19:14:42Z",
        "copyright": "The data included in this document is from www.openstreetmap.org. The data is made available under ODbL."
    },
    "elements": [
        {
            "type": "relation",
            "id": 124582,
            "bounds": {
                "minlat": 45.8689061,
                "minlon": 6.4395807,
                "maxlat": 46.8283926,
                "maxlon": 7.2109599
            },
            "members": [
                {
                    "type": "way",
                    "ref": 936770892,
                    "role": "",
                    "geometry": [
                        {
                            "lat": 46.8240018,
                            "lon": 6.4395807
                        },
                        {
                            "lat": 46.823965,
                            "lon": 6.4396698
                        }
                    ]
                },
                {
                    "type": "way",
                    "ref": 24956166,
                    "role": "",
                    "geometry": [
                        {
                            "lat": 46.8236197,
                            "lon": 6.4400574
                        },
                        {
                            "lat": 46.8235322,
                            "lon": 6.4401168
                        },
                        {
                            "lat": 46.8234367,
                            "lon": 6.4401715
                        }
                    ]
                },
                {
                    "type": "node",
                    "ref": 1107816214,
                    "role": "",
                    "lat": 46.8232651,
                    "lon": 6.4402355
                }
            ],
            "tags": {
                "distance": "31",
                "from": "Lausanne",
                "int_name": "Camino de Santiago",
                "name": "ViaJacobi",
                "network": "nwn",
                "operator": "Wanderland Schweiz",
                "osmc:symbol": "green:green::4:white",
                "pilgrimage": "Camino de Santiago",
                "ref": "4",
                "religion": "christian",
                "route": "hiking",
                "stage": "17",
                "symbol": "weisse 4 auf grünem Rechteck und in südwestlicher Richtung Jakobsmuschel",
                "to": "Roll",
                "type": "route",
                "url": "https://www.schweizmobil.ch/fr/wanderland/etappe4.17"
            }
        }
    ]
}
                  """
                  .trimIndent()
                  .replace("\n", "")
                  .toResponseBody())
          .protocol(Protocol.HTTP_1_1)
          .header("Content-Type", "application/json")
          .request(mock())
          .build()

  private val failedResponse =
      Response.Builder()
          .code(419)
          .message("Too Many Requests")
          .body("Too Many Requests".toResponseBody())
          .protocol(Protocol.HTTP_1_1)
          .request(mock())
          .build()

  private val simpleRoutes: List<HikeRoute> =
      listOf(
          HikeRoute(
              "124582",
              Bounds(45.8689061, 6.4395807, 46.8283926, 7.2109599),
              listOf(
                  LatLong(46.8240018, 6.4395807),
                  LatLong(46.8239650, 6.4396698),
                  LatLong(46.8236197, 6.4400574),
                  LatLong(46.8235322, 6.4401168),
                  LatLong(46.8234367, 6.4401715),
                  LatLong(46.8232651, 6.4402355))))

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)

    hikingRouteProviderRepositoryOverpass = HikeRoutesRepositoryOverpass(mockClient)
  }

  @Test
  fun getRoutes_callsClient() {
    `when`(mockClient.newCall(any())).thenReturn(mock())
    hikingRouteProviderRepositoryOverpass.getRoutes(
        bounds,
        { routes ->
          assertNotEquals(routes.size, 0)
          print(routes)
        }) {
          fail("Failed to fetch routes from Overpass API")
        }
    verify(mockClient).newCall(any())
  }

  @Test
  fun getRoutes_worksOnEmptyAnswer() {
    val mockCall = mock(Call::class.java)
    `when`(mockClient.newCall(any())).thenReturn(mockCall)

    val callbackCapture = argumentCaptor<okhttp3.Callback>()

    `when`(mockCall.enqueue(callbackCapture.capture())).then {
      callbackCapture.firstValue.onResponse(mockCall, emptyResponse)
    }

    hikingRouteProviderRepositoryOverpass.getRoutes(
        bounds, { routes -> assertEquals(0, routes.size) }) {
          fail("Failed to fetch routes from Overpass API")
        }
  }

  @Test
  fun getRoutes_worksOnSimpleAnswer() {
    val mockCall = mock(Call::class.java)
    `when`(mockClient.newCall(any())).thenReturn(mockCall)

    val callbackCapture = argumentCaptor<okhttp3.Callback>()

    `when`(mockCall.enqueue(callbackCapture.capture())).then {
      callbackCapture.firstValue.onResponse(mockCall, simpleResponse)
    }

    hikingRouteProviderRepositoryOverpass.getRoutes(
        bounds, { routes -> assertEquals(simpleRoutes, routes) }) {
          fail("Failed to fetch routes from Overpass API")
        }
  }

  @Test
  fun getRoutes_failsCorrectlyWithServerResponse() {
    val failureCounter = AtomicInteger(0)
    val mockCall = mock(Call::class.java)
    `when`(mockClient.newCall(any())).thenReturn(mockCall)

    val callbackCapture = argumentCaptor<okhttp3.Callback>()

    `when`(mockCall.enqueue(callbackCapture.capture())).then {
      callbackCapture.firstValue.onResponse(mockCall, failedResponse)
    }

    hikingRouteProviderRepositoryOverpass.getRoutes(bounds, { fail("Overpass hasn't failed") }) {
      failureCounter.incrementAndGet()
    }
    assertEquals(1, failureCounter.get())
  }

  @Test
  fun getRoutes_failsCorrectlyWithNoServerResponse() {
    val failureCounter = AtomicInteger(0)
    val mockCall = mock(Call::class.java)
    `when`(mockClient.newCall(any())).thenReturn(mockCall)

    val callbackCapture = argumentCaptor<okhttp3.Callback>()

    `when`(mockCall.enqueue(callbackCapture.capture())).then {
      callbackCapture.firstValue.onFailure(
          mockCall, IOException("Failed to fetch routes from Overpass API"))
    }

    hikingRouteProviderRepositoryOverpass.getRoutes(bounds, { fail("Overpass hasn't failed") }) {
      failureCounter.incrementAndGet()
    }
    assertEquals(1, failureCounter.get())
  }
}
