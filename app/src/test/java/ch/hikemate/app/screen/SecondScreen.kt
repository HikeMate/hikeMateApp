package ch.hikemate.app.screen

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import ch.hikemate.app.resources.C
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class SecondScreen(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SecondScreen>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(C.Tag.second_screen_container) }) {

  val simpleText: KNode = child { hasTestTag(C.Tag.greeting_robo) }
}
