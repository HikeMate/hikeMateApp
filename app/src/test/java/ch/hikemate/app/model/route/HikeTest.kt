package ch.hikemate.app.model.route

import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test

class HikeTest {

  private val hikes =
      listOf(
          createHike(id = "1", isSaved = false),
          createHike(id = "2", isSaved = true),
          createHike(id = "3", isSaved = false),
          createHike(id = "4", isSaved = true),
          createHike(id = "5", isSaved = false))

  private fun createHike(
      id: String = "default",
      isSaved: Boolean = false,
      plannedDate: Timestamp? = null,
      name: String? = null,
      description: String? = null,
      bounds: Bounds? = null,
      waypoints: List<LatLong>? = null,
      elevation: List<Double>? = null,
      distance: Double? = null,
      estimatedTime: Double? = null,
      elevationGain: Double? = null,
      difficulty: HikeDifficulty? = null
  ): Hike {
    return Hike(
        id = id,
        isSaved = isSaved,
        plannedDate = plannedDate,
        name = name,
        description =
            if (description == null) DeferredData.NotRequested
            else DeferredData.Obtained(description),
        bounds = if (bounds == null) DeferredData.NotRequested else DeferredData.Obtained(bounds),
        waypoints =
            if (waypoints == null) DeferredData.NotRequested else DeferredData.Obtained(waypoints),
        elevation =
            if (elevation == null) DeferredData.NotRequested else DeferredData.Obtained(elevation),
        distance =
            if (distance == null) DeferredData.NotRequested else DeferredData.Obtained(distance),
        estimatedTime =
            if (estimatedTime == null) DeferredData.NotRequested
            else DeferredData.Obtained(estimatedTime),
        elevationGain =
            if (elevationGain == null) DeferredData.NotRequested
            else DeferredData.Obtained(elevationGain),
        difficulty =
            if (difficulty == null) DeferredData.NotRequested
            else DeferredData.Obtained(difficulty))
  }

  @Test
  fun toSavedHikeWorksAsExpected() {
    for (hike in hikes) {
      val saved = hike.toSavedHike()
      assertEquals(hike.id, saved.id)
      assertEquals(hike.name, saved.name)
      assertEquals(hike.plannedDate, saved.date)
    }
  }

  @Test
  fun getColorIsDeterministic() {
    for (hike in hikes) {
      // Compute the hike's color once
      val color = hike.getColor()
      for (i in 0..100) {
        // All consecutive computations of the color should yield the same result
        assertEquals(color, hike.getColor())
      }
    }
  }

  @Test
  fun hasOsmDataWorksAsExcepted() {
    val description = "Hike description"
    val bounds = Bounds(0.0, 0.0, 0.0, 0.0)
    val waypoints = emptyList<LatLong>()

    val noDescription = createHike(bounds = bounds, waypoints = waypoints)
    val noBounds = createHike(description = description, waypoints = waypoints)
    val noWaypoints = createHike(description = description, bounds = bounds)
    val complete = createHike(description = description, bounds = bounds, waypoints = waypoints)

    assertFalse(noDescription.hasOsmData())
    assertFalse(noBounds.hasOsmData())
    assertFalse(noWaypoints.hasOsmData())
    assertTrue(complete.hasOsmData())
  }

  @Test
  fun isFullyLoadedWorksAsExpected() {
    val description = "Hike description"
    val bounds = Bounds(0.0, 0.0, 0.0, 0.0)
    val waypoints = emptyList<LatLong>()
    val elevation = emptyList<Double>()
    val distance = 1.0
    val estimatedTime = 2.0
    val elevationGain = 3.0
    val difficulty = HikeDifficulty.MODERATE

    val incomplete1 = createHike()
    val incomplete2 = createHike(waypoints = waypoints, distance = distance)
    val complete =
        createHike(
            description = description,
            bounds = bounds,
            waypoints = waypoints,
            elevation = elevation,
            distance = distance,
            estimatedTime = estimatedTime,
            elevationGain = elevationGain,
            difficulty = difficulty)

    assertFalse(incomplete1.isFullyLoaded())
    assertFalse(incomplete2.isFullyLoaded())
    assertTrue(complete.isFullyLoaded())
  }

  @Test
  fun withDetailsOrThrowWorksAsExpected() {
    val description = "Description"
    val bounds = Bounds(0.0, 0.0, 0.0, 0.0)
    val waypoints = emptyList<LatLong>()
    val elevation = emptyList<Double>()
    val distance = 1.0
    val estimatedTime = 2.0
    val elevationGain = 3.0
    val difficulty = HikeDifficulty.MODERATE

    val deltaForDoubleComparison = 0.0

    val incomplete1 = createHike()
    val incomplete2 = createHike(waypoints = waypoints, distance = distance)
    val complete =
        createHike(
            description = description,
            bounds = bounds,
            waypoints = waypoints,
            elevation = elevation,
            distance = distance,
            estimatedTime = estimatedTime,
            elevationGain = elevationGain,
            difficulty = difficulty)

    assertThrows(IllegalStateException::class.java) { incomplete1.withDetailsOrThrow() }
    assertThrows(IllegalStateException::class.java) { incomplete2.withDetailsOrThrow() }
    val detailed = complete.withDetailsOrThrow()
    assertEquals(complete.id, detailed.id)
    assertEquals(complete.getColor(), detailed.color)
    assertEquals(complete.isSaved, detailed.isSaved)
    assertEquals(complete.plannedDate, detailed.plannedDate)
    assertEquals(complete.name, detailed.name)
    assertEquals(description, detailed.description)
    assertEquals(bounds, detailed.bounds)
    assertEquals(waypoints, detailed.waypoints)
    assertEquals(elevation, detailed.elevation)
    assertEquals(distance, detailed.distance, deltaForDoubleComparison)
    assertEquals(estimatedTime, detailed.estimatedTime, deltaForDoubleComparison)
    assertEquals(elevationGain, detailed.elevationGain, deltaForDoubleComparison)
    assertEquals(difficulty, detailed.difficulty)
  }
}
