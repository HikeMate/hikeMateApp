package ch.hikemate.app.model.guide

import ch.hikemate.app.R
import ch.hikemate.app.ui.navigation.Route

/**
 * Represents a topic in the app guide. The actionRoute is the route the user will be navigated to
 * when clicking on the topic's button.
 */
data class GuideTopic(
    val titleResId: Int,
    val contentResId: Int,
    val actionRoute: String? = null,
)

/** The object contains the topics for HikeMate's guide and the hiking's guide topics. */
object Guide {

  val APP_GUIDE_TOPICS =
      listOf(
          GuideTopic(
              titleResId = R.string.guide_topic_find_hikes,
              contentResId = R.string.guide_content_find_hikes,
              actionRoute = Route.MAP,
          ),
          GuideTopic(
              titleResId = R.string.guide_topic_save_hikes,
              contentResId = R.string.guide_content_save_hikes,
              actionRoute = Route.SAVED_HIKES,
          ),
          GuideTopic(
              titleResId = R.string.guide_topic_profile,
              contentResId = R.string.guide_content_profile,
              actionRoute = Route.PROFILE,
          ),
          GuideTopic(
              titleResId = R.string.guide_section_difficulty_levels,
              contentResId = R.string.guide_content_difficulty_levels),
          GuideTopic(
              titleResId = R.string.guide_topic_track_location,
              contentResId = R.string.guide_content_track_location,
              actionRoute = Route.MAP))

  val HIKING_GUIDE_TOPICS =
      listOf(
          GuideTopic(
              titleResId = R.string.guide_section_basic_gear,
              contentResId = R.string.guide_content_basic_gear),
          GuideTopic(
              titleResId = R.string.guide_section_weather,
              contentResId = R.string.guide_content_weather),
          GuideTopic(
              titleResId = R.string.guide_section_safety_basics,
              contentResId = R.string.guide_content_safety_basics),
          GuideTopic(
              titleResId = R.string.guide_section_hiking_form,
              contentResId = R.string.guide_content_hiking_form),
          GuideTopic(
              titleResId = R.string.guide_section_elevation,
              contentResId = R.string.guide_content_elevation))
}
