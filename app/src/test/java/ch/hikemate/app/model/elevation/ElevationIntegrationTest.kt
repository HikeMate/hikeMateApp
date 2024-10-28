package ch.hikemate.app.model.elevation

import ch.hikemate.app.model.route.LatLong
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class ElevationIntegrationTest {

  private lateinit var client: OkHttpClient
  private lateinit var repository: ElevationServiceRepository
  private val coordinates =
      listOf(LatLong(10.0, 10.0), LatLong(20.0, 20.0), LatLong(41.161758, -8.583933))
  private val longCoordinates =
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
          LatLong(15.0, 10.0),
          LatLong(12.34, 56.78),
          LatLong(23.45, 67.89),
          LatLong(34.56, 78.90),
          LatLong(45.67, 89.01),
          LatLong(56.78, 90.12),
          LatLong(67.89, 101.23),
          LatLong(78.90, 112.34),
          LatLong(89.01, 123.45),
          LatLong(90.12, 134.56),
          LatLong(101.23, 145.67),
          LatLong(112.34, 156.78),
          LatLong(123.45, 167.89),
          LatLong(134.56, 178.90),
          LatLong(145.67, -170.0),
          LatLong(156.78, -160.0),
          LatLong(167.89, -150.0),
          LatLong(178.90, -140.0),
          LatLong(10.5, 10.5),
          LatLong(20.5, 20.5))

  @Before
  fun setup() {
    client = OkHttpClient.Builder().build()
    repository = ElevationServiceRepository(client)
  }

  @Test
  fun getElevation_returnsData() {
    var result: List<Double>? = null

    repository.getElevation(
        coordinates,
        0,
        onSuccess = {
          result = it
          assertNotNull("Response should not be null", result)
          assertEquals("There should be 3 elevation results", coordinates.size, result?.size)
        },
        onFailure = {})
  }

  @Test
  fun getElevation_returnsDataLong() {
    var result: List<Double>? = null

    repository.getElevation(
        longCoordinates,
        0,
        onSuccess = {
          result = it
          assertNotNull("Response should not be null", result)
          assertEquals("There should be 3 elevation results", longCoordinates.size, result?.size)
        },
        onFailure = {})
  }
}
