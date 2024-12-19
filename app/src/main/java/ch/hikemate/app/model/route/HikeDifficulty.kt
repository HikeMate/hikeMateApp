package ch.hikemate.app.model.route

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import ch.hikemate.app.R
import ch.hikemate.app.ui.theme.hikeDifficultyDifficultColor
import ch.hikemate.app.ui.theme.hikeDifficultyEasyColor
import ch.hikemate.app.ui.theme.hikeDifficultyModerateColor

/**
 * Represents the difficulty level of a hike route.
 *
 * Three possible levels: easy, moderate or difficult.
 *
 * @param nameResourceId The string resource ID of the localizable name of the difficulty level
 * @param color The color associated with the difficulty level
 */
enum class HikeDifficulty(@StringRes val nameResourceId: Int, val color: Color) {
  EASY(R.string.hike_difficulty_easy, hikeDifficultyEasyColor),
  MODERATE(R.string.hike_difficulty_moderate, hikeDifficultyModerateColor),
  DIFFICULT(R.string.hike_difficulty_difficult, hikeDifficultyDifficultColor)
}
