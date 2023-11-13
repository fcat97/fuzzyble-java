package media.uqab.fuzzyble.dbCreator.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val primaryColor = Color(0xff83A2FF)
val accentColor = Color(0xffFFD28F)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = MaterialTheme.colors.copy(primary = primaryColor, secondary = accentColor),
        content = content
    )
}