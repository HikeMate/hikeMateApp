package ch.hikemate.app.model.guide

import ch.hikemate.app.R
import ch.hikemate.app.ui.navigation.Route
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class GuideTest {
  @Test
  fun testGuideTopicCreation() {
    val topic =
        GuideTopic(
            titleResId = R.string.guide_topic_find_hikes,
            contentResId = R.string.guide_content_find_hikes,
            actionRoute = Route.MAP)

    assertEquals(R.string.guide_topic_find_hikes, topic.titleResId)
    assertEquals(R.string.guide_content_find_hikes, topic.contentResId)
    assertEquals(Route.MAP, topic.actionRoute)
  }

  @Test
  fun testGuideTopicCreationWithoutActionRoute() {
    val topic =
        GuideTopic(
            titleResId = R.string.guide_section_basic_gear,
            contentResId = R.string.guide_content_basic_gear)

    assertEquals(R.string.guide_section_basic_gear, topic.titleResId)
    assertEquals(R.string.guide_content_basic_gear, topic.contentResId)
    assertNull(topic.actionRoute)
  }
}
