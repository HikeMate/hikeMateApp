package ch.hikemate.app.model.profile

import android.content.Context
import ch.hikemate.app.R
import com.google.firebase.Timestamp

/** An enum class representing the hiking level of a user. */
enum class HikingLevel {
  BEGINNER,
  INTERMEDIATE,
  EXPERT;

  /**
   * Returns the display string of the hiking level.
   *
   * @param context The context to get the string from.
   */
  fun getDisplayString(context: Context): String {
    return when (this) {
      BEGINNER -> context.getString(R.string.hiking_level_beginner)
      INTERMEDIATE -> context.getString(R.string.hiking_level_intermediate)
      EXPERT -> context.getString(R.string.hiking_level_expert)
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
