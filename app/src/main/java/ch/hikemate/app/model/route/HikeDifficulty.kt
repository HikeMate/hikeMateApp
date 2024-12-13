package ch.hikemate.app.model.route

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import ch.hikemate.app.R

/**
 * Represents the difficulty level of a hike route.
 *
 * Three possible levels: easy, moderate or difficult.
 *
 * @param nameResourceId The string resource ID of the localizable name of the difficulty level
 * @param colorResourceId The color resource ID of the color associated with the difficulty level
 */
enum class HikeDifficulty(@StringRes val nameResourceId: Int, @ColorRes val colorResourceId: Int) {
  EASY(R.string.hike_difficulty_easy, R.color.hike_difficulty_easy),
  MODERATE(R.string.hike_difficulty_moderate, R.color.hike_difficulty_moderate),
  DIFFICULT(R.string.hike_difficulty_difficult, R.color.hike_difficulty_difficult)
}
