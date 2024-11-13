package ch.hikemate.app.model.route.saved

import com.google.firebase.Timestamp

/**
 * Represents a hike that was saved by the user to easily find it again later. The whole hike does
 * not need to be stored, only its id, name for good measure and a date at which the user plans to
 * go there, if any.
 */
data class SavedHike(
    /**
     * The unique ID of the hike. Allows to retrieve additional information about the hike from the
     * API in case the user wants more details.
     */
    val id: String = "",

    /**
     * The human-friendly and readable name of the hike. Can be used, for example, to be displayed
     * in a list of saved hikes without querying the API to get all the names.
     */
    val name: String = "",

    /**
     * The date at which the user plans to go on the hike. Can be null if the user has not set a
     * date yet or does not plan to add one.
     */
    val date: Timestamp? = null
)

