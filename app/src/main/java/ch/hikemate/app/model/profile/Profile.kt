package ch.hikemate.app.model.profile

import com.google.firebase.Timestamp

/** An enum class representing the hiking level of a user. */
enum class HikingLevel {
  BEGINNER,
  INTERMEDIATE,
  EXPERT
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
