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
  private val bounds = Bounds(46.51402, 6.55989, 46.52291, 6.58243)
  private val containedBounds = Bounds(46.51502, 6.56989, 46.52191, 6.58143)
  private val nonContainedBounds = Bounds(46.51402, 6.55989, 46.52291, 6.68243)

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

  private val simpleResponse2 =
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
                            "lat": 46.823965,
                            "lon": 6.4396698
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
                            "lat": 46.823965,
                            "lon": 6.4396698
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

  private val doubleResponse =
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
                            "lat": 46.823965,
                            "lon": 6.4396698
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
        },
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
                            "lat": 46.823965,
                            "lon": 6.4396698
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

  private val responseWithAlternativeRoutes =
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
                            "lat": 46.823965,
                            "lon": 6.4396698
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
                },
                {
                    "type": "relation",
                    "ref": 124582,
                    "role": "alternative"
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

  private val responseWithFromAndToButNoName =
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
                        }
                    ]
                }
            ],
            "tags": {
                "from": "Lausanne",
                "to": "Roll"
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

  private val responseWithNoName =
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
                        }
                    ]
                }
            ],
            "tags": {
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

  private val responseWithCorruptedHikes =
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
                        }
                    ]
                },
                {
                    "type": "way",
                    "ref": 936770892,
                    "role": "",
                    "geometry": [
                        {
                            "lat": 47.8240018,
                            "lon": 6.4395807
                        }
                    ]
                }
            ],
            "tags": {
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

  private val simpleRoutes: List<HikeRoute> =
      listOf(
          HikeRoute(
              "124582",
              Bounds(45.8689061, 6.4395807, 46.8283926, 7.2109599),
              listOf(
                  LatLong(46.8240018, 6.4395807),
                  LatLong(46.8239650, 6.4396698),
                  LatLong(46.8235322, 6.4401168),
                  LatLong(46.8234367, 6.4401715)),
              "Camino de Santiago",
              "Lausanne - Roll"))

  private val combinedRoutes: List<HikeRoute> =
      listOf(
          HikeRoute(
              "124582",
              Bounds(45.8689061, 6.4395807, 46.8283926, 7.2109599),
              listOf(LatLong(46.8240018, 6.4395807)),
              "Lausanne - Roll",
              null))

  private val noNameRoutes: List<HikeRoute> =
      listOf(
          HikeRoute(
              "124582",
              Bounds(45.8689061, 6.4395807, 46.8283926, 7.2109599),
              listOf(LatLong(46.8240018, 6.4395807)),
              null,
              null))

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

  // We currently only support the main route, not alternative routes
  @Test
  fun getRoutes_worksOnAlternativeRoutes() {
    val mockCall = mock(Call::class.java)
    `when`(mockClient.newCall(any())).thenReturn(mockCall)

    val callbackCapture = argumentCaptor<okhttp3.Callback>()

    `when`(mockCall.enqueue(callbackCapture.capture())).then {
      callbackCapture.firstValue.onResponse(mockCall, responseWithAlternativeRoutes)
    }

    hikingRouteProviderRepositoryOverpass.getRoutes(
        bounds, { routes -> assertEquals(simpleRoutes, routes) }) {
          fail("Failed to fetch routes from Overpass API")
        }
  }

  @Test
  fun getRoutes_cacheWorks() {
    val mockCall = mock(Call::class.java)
    `when`(mockClient.newCall(any())).thenReturn(mockCall)

    val callbackCapture = argumentCaptor<okhttp3.Callback>()

    `when`(mockCall.enqueue(callbackCapture.capture())).then {
      callbackCapture.firstValue.onResponse(mockCall, simpleResponse)
    }
    assertEquals(hikingRouteProviderRepositoryOverpass.getCacheSize(), 0)

    hikingRouteProviderRepositoryOverpass.getRoutes(
        bounds, { routes -> assertEquals(simpleRoutes, routes) }) {
          fail("Failed to fetch routes from Overpass API")
        }

    assertEquals(hikingRouteProviderRepositoryOverpass.getCacheSize(), 1)

    hikingRouteProviderRepositoryOverpass.getRoutes(
        containedBounds, { routes -> assertEquals(simpleRoutes, routes) }) {
          fail("Failed to fetch routes from Overpass API")
        }
    assertEquals(hikingRouteProviderRepositoryOverpass.getCacheSize(), 1)
  }

  @Test
  fun getRoutes_retrievesFromApiWhenNotInCache() {
    val mockCall = mock(Call::class.java)
    `when`(mockClient.newCall(any())).thenReturn(mockCall)

    val callbackCapture = argumentCaptor<okhttp3.Callback>()

    // Mock the first response
    `when`(mockCall.enqueue(callbackCapture.capture())).then {
      callbackCapture.firstValue.onResponse(mockCall, simpleResponse)
    }

    assertEquals(0, hikingRouteProviderRepositoryOverpass.getCacheSize())

    // Test fetching routes not in cache
    hikingRouteProviderRepositoryOverpass.getRoutes(
        bounds, { routes -> assertEquals(simpleRoutes, routes) }) {
          fail("Failed to fetch routes from Overpass API")
        }

    assertEquals(1, hikingRouteProviderRepositoryOverpass.getCacheSize())

    val mockCall2 = mock(Call::class.java)
    `when`(mockClient.newCall(any())).thenReturn(mockCall2)

    val callbackCapture2 = argumentCaptor<okhttp3.Callback>()

    // Mock the first response
    `when`(mockCall2.enqueue(callbackCapture2.capture())).then {
      callbackCapture2.firstValue.onResponse(mockCall2, simpleResponse2)
    }

    hikingRouteProviderRepositoryOverpass.getRoutes(
        nonContainedBounds, { routes -> assertEquals(simpleRoutes, routes) }) {
          fail("Failed to fetch routes from Overpass API")
        }

    assertEquals(2, hikingRouteProviderRepositoryOverpass.getCacheSize())
  }

  @Test
  fun getRoutes_combinesFromAndToTags() {
    val mockCall = mock(Call::class.java)
    `when`(mockClient.newCall(any())).thenReturn(mockCall)

    val callbackCapture = argumentCaptor<okhttp3.Callback>()

    `when`(mockCall.enqueue(callbackCapture.capture())).then {
      callbackCapture.firstValue.onResponse(mockCall, responseWithFromAndToButNoName)
    }

    hikingRouteProviderRepositoryOverpass.getRoutes(
        bounds, { routes -> assertEquals(combinedRoutes, routes) }) {
          fail("Failed to fetch routes from Overpass API")
        }
  }

  @Test
  fun getRoutes_worksOnNoName() {
    val mockCall = mock(Call::class.java)
    `when`(mockClient.newCall(any())).thenReturn(mockCall)

    val callbackCapture = argumentCaptor<okhttp3.Callback>()

    `when`(mockCall.enqueue(callbackCapture.capture())).then {
      callbackCapture.firstValue.onResponse(mockCall, responseWithNoName)
    }

    hikingRouteProviderRepositoryOverpass.getRoutes(
        bounds, { routes -> assertEquals(noNameRoutes, routes) }) {
          fail("Failed to fetch routes from Overpass API")
        }
  }

  @Test
  fun getRoutes_returnsEmptyListOnCorruptedHikes() {
    val mockCall = mock(Call::class.java)
    `when`(mockClient.newCall(any())).thenReturn(mockCall)

    val callbackCapture = argumentCaptor<okhttp3.Callback>()

    `when`(mockCall.enqueue(callbackCapture.capture())).then {
      callbackCapture.firstValue.onResponse(mockCall, responseWithCorruptedHikes)
    }

    hikingRouteProviderRepositoryOverpass.getRoutes(bounds, { assertEquals(0, it.size) }) {
      fail("Should not have failed routes")
    }
  }

  @Test
  fun getRouteById_failsOnEmptyResponse() {
    val mockCall = mock(Call::class.java)
    `when`(mockClient.newCall(any())).thenReturn(mockCall)

    val callbackCapture = argumentCaptor<okhttp3.Callback>()

    `when`(mockCall.enqueue(callbackCapture.capture())).then {
      callbackCapture.firstValue.onResponse(mockCall, emptyResponse)
    }

    var failCalled = false

    hikingRouteProviderRepositoryOverpass.getRouteById(
        "124582", { fail("onSuccess shouldn't have been called") }, { failCalled = true })

    assert(failCalled)
  }

  @Test
  fun getRouteById_failsOnSeveralRoutesFound() {
    val mockCall = mock(Call::class.java)
    `when`(mockClient.newCall(any())).thenReturn(mockCall)

    val callbackCapture = argumentCaptor<okhttp3.Callback>()

    `when`(mockCall.enqueue(callbackCapture.capture())).then {
      callbackCapture.firstValue.onResponse(mockCall, doubleResponse)
    }

    var failCalled = false

    hikingRouteProviderRepositoryOverpass.getRouteById(
        "124582", { fail("onSuccess shouldn't have been called") }, { failCalled = true })

    assert(failCalled)
  }

  @Test
  fun getRouteById_failsOnFailedResponse() {
    val mockCall = mock(Call::class.java)
    `when`(mockClient.newCall(any())).thenReturn(mockCall)

    val callbackCapture = argumentCaptor<okhttp3.Callback>()

    `when`(mockCall.enqueue(callbackCapture.capture())).then {
      callbackCapture.firstValue.onResponse(mockCall, failedResponse)
    }

    var failCalled = false

    hikingRouteProviderRepositoryOverpass.getRouteById(
        "124582", { fail("onSuccess shouldn't have been called") }, { failCalled = true })

    assert(failCalled)
  }

  @Test
  fun getRouteById_succeedsOnSingleRoute() {
    val mockCall = mock(Call::class.java)
    `when`(mockClient.newCall(any())).thenReturn(mockCall)

    val callbackCapture = argumentCaptor<okhttp3.Callback>()

    `when`(mockCall.enqueue(callbackCapture.capture())).then {
      callbackCapture.firstValue.onResponse(mockCall, simpleResponse)
    }

    var successCalled = false

    hikingRouteProviderRepositoryOverpass.getRouteById(
        "124582", { successCalled = true }, { fail("onFailure shouldn't have been called") })

    assert(successCalled)
  }
}
