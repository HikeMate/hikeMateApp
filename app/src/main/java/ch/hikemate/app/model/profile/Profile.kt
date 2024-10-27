package ch.hikemate.app.model.profile

import com.google.firebase.Timestamp

/**
 * A class representing a fitness level.
 *
 * @property level The level of the fitness level.
 * @property description The description of the fitness level.
 */
data class FitnessLevel(
    val level: Int,
    val description: String,
)

/**
 * A class representing a user profile.
 *
 * @property id The ID of the profile.
 * @property name The name of the profile.
 * @property email The email of the profile.
 * @property fitnessLevel The fitness level of the profile.
 */
data class Profile(
    val id: String,
    val name: String,
    val email: String,
    val fitnessLevel: FitnessLevel,
    val joinedDate: Timestamp,
)
