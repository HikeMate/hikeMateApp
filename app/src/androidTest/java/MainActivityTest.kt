package ch.hikemate.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.Rule
import org.junit.Test

class MainActivityTest : TestCase() {
  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun mainActivityContainsNavHost() {
    // Check that the NavHost is displayed

    composeTestRule.onNodeWithTag(TEST_TAG_NAV_HOST).assertIsDisplayed()
  }
}
