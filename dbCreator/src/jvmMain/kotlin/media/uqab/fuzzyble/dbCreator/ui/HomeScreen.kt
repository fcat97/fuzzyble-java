package media.uqab.fuzzyble.dbCreator.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import kotlinx.coroutines.launch
import media.uqab.fuzzyble.dbCreator.model.Project
import media.uqab.fuzzyble.dbCreator.usecase.CreateNewProject
import media.uqab.fuzzyble.dbCreator.usecase.GetProjectFromPath
import media.uqab.fuzzyble.dbCreator.usecase.GetRecentProjects
import media.uqab.fuzzyble.dbCreator.usecase.SaveRecentProject
import media.uqab.fuzzyble.dbCreator.utils.AsyncImage


class HomeScreen : Screen {
    private var dialogIntent by mutableStateOf<DialogIntent?>(null)
    private var recentProjects by mutableStateOf<List<Project>>(emptyList())

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
        LaunchedEffect(Unit) {
            recentProjects = GetRecentProjects()
        }

        Dialogs(dialogIntent) { dialogIntent = it }

        // main contents
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Welcome!",
                modifier = Modifier.padding(top = 40.dp)
                    .align(Alignment.CenterHorizontally),
                fontSize = 40.sp,
            )

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // open existing
                Column(
                    modifier = Modifier.size(150.dp)
                        .clip(RoundedCornerShape(15))
                        .background(color = Color.LightGray)
                        .clickable { dialogIntent = DialogIntent.OPEN_EXISTING_PROJECT },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                ) {
                    AsyncImage(Icons.open)

                    Text("Open Existing")
                }

                // new project
                Column(
                    modifier = Modifier.size(150.dp)
                        .clip(RoundedCornerShape(15))
                        .background(color = Color.LightGray)
                        .clickable { dialogIntent = DialogIntent.CREATE_NEW_PROJECT },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                ) {
                    AsyncImage(Icons.add)

                    Text("New Project")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Recent Projects")
            Divider(modifier = Modifier.fillMaxWidth(0.5f).height(1.dp))
            RecentProjects(Modifier.fillMaxWidth(0.5f))
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
                    openProject(project)
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
                    openProject(project)
                }
            }
        }

        when (dialogIntent) {
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

    @Composable
    private fun RecentProjects(modifier: Modifier) {
        val coroutine = rememberCoroutineScope()

        LazyColumn(modifier) {
            items(items = recentProjects) { project ->
                Row(
                    Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(25))
                        .clickable {
                            coroutine.launch {
                                openProject(project)
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(project.name)
                        Text(project.projectDir)
                    }

                    AsyncImage(Icons.rightArrow)
                }
            }
        }
    }

    private suspend fun openProject(project: Project) {
        SaveRecentProject(project)
        ScreenManager.peek().push(ProjectScreen(project))
    }
}