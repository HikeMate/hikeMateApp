package ch.hikemate.app.model.profile

import ch.hikemate.app.R
import com.google.firebase.Timestamp

/** An enum class representing the hiking level of a user. */
enum class HikingLevel {
  BEGINNER,
  INTERMEDIATE,
  EXPERT;

  fun getDisplayNameId(): Int {
    return when (this) {
      BEGINNER -> R.string.hiking_level_beginner
      INTERMEDIATE -> R.string.hiking_level_intermediate
      EXPERT -> R.string.hiking_level_expert
    }
  }
}

/**
 * A class representing a user profile.
 *
 * @property id The ID of the profile.
 * @property name The name of the profile.
 * @property email The email of the profile.
 * @property hikingLevel The hiking level of the profile.
 */
data class Profile(
    val id: String,
    val name: String,
    val email: String,
    val hikingLevel: HikingLevel,
    val joinedDate: Timestamp,
)
