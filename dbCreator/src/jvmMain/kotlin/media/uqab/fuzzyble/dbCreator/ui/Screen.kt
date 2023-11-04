package media.uqab.fuzzyble.dbCreator.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.*

interface Screen {
    @Composable
    fun Content()
}

class ScreenManager private constructor(val key: String){
    companion object {
        private val managers = hashMapOf<String, ScreenManager>()
        var mostRecentTag = UUID.randomUUID().toString()
            private set

        fun peek(tag: String = mostRecentTag): ScreenManager {
            var sc = managers[tag]
            if (sc == null) {
                sc = ScreenManager(tag)
                managers[tag] = sc
            }

            mostRecentTag = tag
            return sc
        }
    }
    private val stack: Stack<Screen> = Stack()
    private var stackSize by mutableStateOf(0)
    private var initialized = false

    @Composable
    fun Start(startingScreen: @Composable () -> Screen) {
        if (!initialized) {
            push(startingScreen())
            initialized = true
        }

        println("screen changed. stack: $stackSize")
        stack.peek().Content()
    }

    fun push(screen: Screen) {
        stack.push(screen)
        stackSize = stack.size
    }

    fun pop(): Screen {
        val ans = stack.pop()
        stackSize = stack.size
        return ans
    }
}