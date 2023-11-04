package media.uqab.fuzzyble.dbCreator.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import kotlinx.coroutines.launch
import media.uqab.fuzzyble.dbCreator.usecase.CreateNewProject
import media.uqab.fuzzyble.dbCreator.usecase.GetProjectFromPath


class HomeScreen : Screen {
    private var dialogIntent by mutableStateOf<DialogIntent?>(null)

    @Composable
    override fun Content() {
        HomeScreenContent()
    }

    private enum class DialogIntent {
        OPEN_EXISTING_PROJECT,
        CREATE_NEW_PROJECT,
        NO_PROJECT,
        FAILED_TO_CREATE_NEW
    }

    @Preview
    @Composable
    private fun HomeScreenContent() {
        Dialogs(dialogIntent) { dialogIntent = it }

        // main contents
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Welcome!",
                modifier = Modifier.padding(top = 40.dp)
                    .align(Alignment.TopCenter),
                fontSize = 40.sp,
            )

            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Column(
                    modifier = Modifier.size(150.dp)
                        .clip(RoundedCornerShape(15))
                        .background(color = Color.LightGray)
                        .clickable { dialogIntent = DialogIntent.OPEN_EXISTING_PROJECT },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                ) {
                    Icon(Icons.Default.Edit, null)

                    Text("Open Existing")
                }

                Column(
                    modifier = Modifier.size(150.dp)
                        .clip(RoundedCornerShape(15))
                        .background(color = Color.LightGray)
                        .clickable { dialogIntent = DialogIntent.CREATE_NEW_PROJECT },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                ) {
                    Icon(Icons.Default.Add, null)

                    Text("New Project")
                }
            }

        }
    }

    @Composable
    private fun Dialogs(dialogIntent: DialogIntent?, onChangeIntent: (DialogIntent?) -> Unit) {
        val coroutine = rememberCoroutineScope()

        fun openExistingProject(projectDir: String?) {
            coroutine.launch {
                if (projectDir == null) return@launch

                val project = GetProjectFromPath(projectDir)
                if (project == null) {
                    onChangeIntent(DialogIntent.NO_PROJECT)
                } else {
                    ScreenManager.peek().push(ProjectScreen(project))
                }
            }
        }
        fun createNewProject(projectDir: String?) {
            coroutine.launch {
                if (projectDir == null) return@launch

                val project = CreateNewProject(projectDir)
                if (project == null) {
                    onChangeIntent(DialogIntent.FAILED_TO_CREATE_NEW)
                } else {
                    ScreenManager.peek().push(ProjectScreen(project))
                }
            }
        }

        when(dialogIntent) {
            DialogIntent.OPEN_EXISTING_PROJECT -> {
                DirectoryPicker(true, System.getProperty("user.home")) {
                    onChangeIntent(null)
                    openExistingProject(it)
                }
            }
            DialogIntent.CREATE_NEW_PROJECT -> {
                DirectoryPicker(true, System.getProperty("user.home")) {
                    onChangeIntent(null)
                    createNewProject(it)
                }
            }
            DialogIntent.NO_PROJECT -> {
                ProjectNotExist(
                    onCreateNew = {
                        onChangeIntent(DialogIntent.CREATE_NEW_PROJECT)
                    },
                    onDismiss = {
                        onChangeIntent(null)
                    }
                )
            }
            else -> {}
        }
    }

    @Composable
    private fun ProjectNotExist(
        onCreateNew: () -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("Wrong Directory", color = Color.Red)
            },
            text = {
                Text(
                    "This directory doesn't not contain required files to open the project. " +
                            "You may have selected wrong directory or the files may be corrupted!"
                )
            },
            buttons = {
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) {
                        Text("Dismiss", color = Color.DarkGray)
                    }

                    TextButton(onClick = onCreateNew) {
                        Text(text = "Create New", color = Color.Black)
                    }
                }
            }
        )
    }
}