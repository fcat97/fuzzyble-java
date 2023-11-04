package media.uqab.fuzzyble.dbCreator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

sealed class Event(val msg: String) {
    class Success(msg: String) : Event(msg)
    class Failure(msg: String) : Event(msg)
}

@Composable
fun EventBar(modifier: Modifier, event: Event?, onConsumed: () -> Unit) {
    LaunchedEffect(event) {
        if (event == null) return@LaunchedEffect
        delay(2000)
        onConsumed()
    }

    AnimatedVisibility(
        event != null,
        modifier = modifier.background(color = if (event is Event.Failure) Color.Red else Color.Blue),
    ) {
        Row(horizontalArrangement = Arrangement.Center) {
            Text(event?.msg ?: "", color = Color.White)
        }
    }
}