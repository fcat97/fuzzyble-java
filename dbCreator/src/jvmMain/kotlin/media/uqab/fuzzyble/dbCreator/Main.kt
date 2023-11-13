package media.uqab.fuzzyble.dbCreator

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ApplicationScope
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
