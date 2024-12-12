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

  @Test
  fun segmentsWorksAsExpected() {
    // Case 1: Empty waypoints list

    val baseHike =
        createHike(
                description = "description",
                bounds = Bounds(0.0, 0.0, 0.0, 0.0),
                waypoints = emptyList(),
                elevation = emptyList(),
                distance = 0.0,
                estimatedTime = 0.0,
                elevationGain = 0.0,
                difficulty = HikeDifficulty.EASY)
            .withDetailsOrThrow()
    val emptyHike = baseHike

    assertTrue(emptyHike.segments.isEmpty())

    // Case 2: Single waypoint - should also result in empty segments
    val singleWaypointHike =
        createHike(
                description = "description",
                bounds = Bounds(0.0, 0.0, 0.0, 0.0),
                waypoints = listOf(LatLong(0.0, 0.0)),
                elevation = listOf(0.0),
                distance = 0.0,
                estimatedTime = 0.0,
                elevationGain = 0.0,
                difficulty = HikeDifficulty.EASY)
            .withDetailsOrThrow()

    assertTrue(singleWaypointHike.segments.isEmpty())

    // Case 3: Two waypoints - should create one segment
    val p1 = LatLong(0.0, 0.0)
    val p2 = LatLong(1.0, 1.0)
    val twoPointHike = baseHike.copy(waypoints = listOf(p1, p2))

    assertEquals(1, twoPointHike.segments.size)
    assertEquals(p1, twoPointHike.segments[0].start)
    assertEquals(p2, twoPointHike.segments[0].end)
    assertEquals(p1.distanceTo(p2), twoPointHike.segments[0].length, 0.0001)

    // Case 4: Multiple waypoints
    val points = listOf(LatLong(0.0, 0.0), LatLong(1.0, 1.0), LatLong(2.0, 2.0), LatLong(3.0, 3.0))
    val multiPointHike = baseHike.copy(waypoints = points)

    assertEquals(3, multiPointHike.segments.size)

    // Verify each segment
    points.zipWithNext().forEachIndexed { index, (start, end) ->
      assertEquals(start, multiPointHike.segments[index].start)
      assertEquals(end, multiPointHike.segments[index].end)
      assertEquals(start.distanceTo(end), multiPointHike.segments[index].length, 0.0001)
    }

    // Case 5: Verify segment lengths are correct with known distances
    val knownPoints =
        listOf(
            LatLong(0.0, 0.0),
            LatLong(0.0, 1.0), // 1 degree longitude at equator
            LatLong(1.0, 1.0) // 1 degree latitude
            )
    val knownDistanceHike = baseHike.copy(waypoints = knownPoints)

    assertEquals(2, knownDistanceHike.segments.size)
    assertTrue(knownDistanceHike.segments[0].length > 0)
    assertTrue(knownDistanceHike.segments[1].length > 0)
  }
}
