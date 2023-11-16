package media.uqab.fuzzyble.dbCreator

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import media.uqab.fuzzyble.dbCreator.ui.AppTheme
import media.uqab.fuzzyble.dbCreator.ui.HomeScreen
import media.uqab.fuzzyble.dbCreator.ui.ScreenManager

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Fuzzyble Database Creator"
    ) {
        AppTheme {
            ScreenManager.peek().Start {
                HomeScreen()
            }
        }
    }
}

object Const {
    const val APP_NAME = "DbCreator"
}
