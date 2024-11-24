package ch.hikemate.app.model.extensions

import org.osmdroid.util.BoundingBox

/** Return true if the bounding box crosses the date line */
fun BoundingBox.crossesDateLine(): Boolean {
  return lonEast < lonWest
}

/** Split the bounding box by the date line */
fun BoundingBox.splitByDateLine(): Pair<BoundingBox, BoundingBox> {
  return if (crossesDateLine()) {
    Pair(
        BoundingBox(latNorth, 180.0, latSouth, lonWest),
        BoundingBox(latNorth, lonEast, latSouth, -180.0))
  } else {
    Pair(this, this)
  }
}
