package ch.hikemate.app.model.guide

import ch.hikemate.app.ui.navigation.NavigationActions

object Guide{
    data class GuideTopic(
        val titleResId: Int,
        val contentResId: Int,
        val actionRoute: String? = null,
        val iconResId: Int? = null,
        val interactiveGuide: List<GuideStep>? = null
    )

    data class GuideStep(
        val title: Int,
        val description: Int,
        val action: ((NavigationActions) -> Unit)? = null
    )
}
