package media.uqab.fuzzyble.dbCreator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.twotone.CheckCircle
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material.icons.twotone.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import media.uqab.fuzzyble.dbCreator.database.Database
import media.uqab.fuzzyble.dbCreator.model.Project
import media.uqab.fuzzyble.dbCreator.usecase.CreateFuzzyTable
import media.uqab.fuzzyble.dbCreator.usecase.GetDatabase
import media.uqab.fuzzyble.dbCreator.usecase.SaveProject
import media.uqab.fuzzybleJava.ColumnTrigrams
import media.uqab.fuzzybleJava.ColumnWordLen
import media.uqab.fuzzybleJava.FuzzyCursor
import kotlin.io.path.Path
import kotlin.io.path.extension

class ProjectScreen(project: Project) : Screen {
    private var event by mutableStateOf<Event?>(null)
    private var dialogIntent by mutableStateOf<DialogIntent?>(null)

    private var name by mutableStateOf(project.name)
    private var projectDir by mutableStateOf(project.projectDir)
    private var srcDb by mutableStateOf(project.srcDb)
    private var syncDb by mutableStateOf(project.syncDb)

    private val tables = mutableStateListOf<String>()
    private val columns = mutableStateListOf<String>()
    private var selectedTable by mutableStateOf(-1)
    private var selectedColumn by mutableStateOf(-1)

    private var isOperationRunning by mutableStateOf(false)

    private var searchJob: Job? = null
    private var searching by mutableStateOf(false)
    private var searchText by mutableStateOf("")
    private val suggestions = mutableListOf<String>()
    private val tableItems = mutableStateListOf<AnnotatedString>()

    @Composable
    override fun Content() {
        ProjectHomeContent()
    }

    private enum class DialogIntent {
        OpenSourceFilePicker,
        OpenSyncFilePicker,
        FileExtensionMismatch
    }

    @Composable
    private fun ProjectHomeContent() {
        val coroutine = rememberCoroutineScope()

        Dialogs(dialogIntent) { dialogIntent = it }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                EventBar(modifier = Modifier.fillMaxWidth(), event) { event = null }

                AnimatedVisibility(isOperationRunning) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                TextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(0.8f)
                        .padding(top = 40.dp),

                    label = { Text("Project Name") }
                )

                Spacer(Modifier.height(20.dp))

                DatabaseLocation()

                Spacer(Modifier.height(20.dp))

                DatabaseColumnSelection()

                Spacer(Modifier.height(20.dp))

                TableDetails()
            }

            Button(
                onClick = {
                    coroutine.launch {
                        event = if (saveChanges()) {
                            Event.Success("Changes Saved")
                        } else {
                            Event.Failure("Failed to save changes")
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Text("Save Changes")
            }
        }
    }

    @Composable
    private fun DatabaseLocation() {
        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextField(
                value = srcDb,
                onValueChange = { srcDb = it },
                modifier = Modifier.fillMaxWidth(48 / 100f),
                label = {
                    Text("Source Database")
                },
                trailingIcon = {
                    IconButton(onClick = { dialogIntent = DialogIntent.OpenSourceFilePicker }) {
                        Icon(Icons.Default.Edit, null)
                    }
                }
            )

            TextField(
                value = syncDb,
                onValueChange = { syncDb = it },
                modifier = Modifier.fillMaxWidth(48 / 52f),
                label = {
                    Text("Destination Database")
                },
                trailingIcon = {
                    IconButton(onClick = { dialogIntent = DialogIntent.OpenSyncFilePicker }) {
                        Icon(Icons.Default.Edit, null)
                    }
                }
            )
        }
    }

    @Composable
    private fun DatabaseColumnSelection() {
        var expandTableDropDown by remember { mutableStateOf(false) }
        var expandColumnDropDown by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val db = GetDatabase(srcDb)
            db.getTables().forEach {
                tables.add(it)
            }
            if (tables.isNotEmpty()) selectedTable = 0
        }

        LaunchedEffect(selectedTable) {
            try {
                // check if selection is correct or not
                tables[selectedTable]

                val db = GetDatabase(srcDb)
                columns.clear()
                db.getColumns(tables[selectedTable]).forEach {
                    columns.add(it)
                }
                if (columns.isNotEmpty()) selectedColumn = 0
            } catch (ignored: Exception) {
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DropDownSelector(
                modifier = Modifier.fillMaxWidth(48 / 100f),
                label = "Source DB table",
                items = tables,
                selected = selectedTable,
                onSelect = { selectedTable = it },
                expanded = expandTableDropDown,
                onDismiss = { expandTableDropDown = it }
            )

            DropDownSelector(
                modifier = Modifier.fillMaxWidth(48 / 52f),
                label = "Source DB column",
                items = columns,
                selected = selectedColumn,
                onSelect = { selectedColumn = it },
                expanded = expandColumnDropDown,
                onDismiss = { expandColumnDropDown = it }
            )
        }
    }

    @Composable
    private fun TableDetails() {
        val coroutine = rememberCoroutineScope()
        var show by remember { mutableStateOf(false) }
        var isFuzzyble by remember { mutableStateOf(false) }
        var isPopulated by remember { mutableStateOf(false) }

        LaunchedEffect(srcDb, syncDb, selectedTable, selectedColumn) {
            show = try {
                if (srcDb.isBlank()) false
                else if (syncDb.isBlank()) false
                else if (tables[selectedTable].isBlank()) false
                else if (columns[selectedColumn].isBlank()) false
                else true
            } catch (ignored: Exception) {
                false
            }

            try {
                val db = GetDatabase(srcDb)
                val t = tables[selectedTable]
                val c = columns[selectedColumn]
                val columnWordLen = ColumnWordLen(t, c)
                val cursor = FuzzyCursor(db)

                isFuzzyble = cursor.isFuzzyble(columnWordLen)
                isPopulated = cursor.isPopulated(columnWordLen)
            } catch (ignore: Exception) {

            }
        }

        AnimatedVisibility(show) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isFuzzyble) {
                            Icon(Icons.TwoTone.CheckCircle, null, modifier = Modifier.size(24.dp), tint = Color.Green)
                        } else {
                            Icon(Icons.TwoTone.Warning, null, modifier = Modifier.size(24.dp), tint = Color.LightGray)
                        }

                        Text("Fuzzyble Column")
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPopulated) {
                            Icon(Icons.TwoTone.CheckCircle, null, modifier = Modifier.size(24.dp), tint = Color.Green)
                        } else {
                            Icon(Icons.TwoTone.Warning, null, modifier = Modifier.size(24.dp), tint = Color.LightGray)
                        }

                        Text("Column Populated")
                    }

                    TextButton(
                        onClick = {
                            coroutine.launch {
                                try {
                                    event = Event.Success("Enabling Fuzzy Search")
                                    isOperationRunning = true
                                    val cur = FuzzyCursor(GetDatabase(srcDb))
                                    val t = tables[selectedTable]
                                    val c = columns[selectedColumn]
                                    val column = ColumnWordLen(t, c)
                                    CreateFuzzyTable(cur, column, true)
                                    event = Event.Success("Fuzzy Search Enabled")
                                } catch (ignore: Exception) {
                                } finally {
                                    isOperationRunning = false
                                }

                            }
                        },
                    ) {
                        Text("Enable Fuzzy Search")
                    }
                }

                AnimatedVisibility(isFuzzyble) {
                    SearchBar(Modifier.fillMaxWidth(0.8f))
                }

                DataTable(Modifier.fillMaxWidth(0.8f))
            }
        }
    }

    @Composable
    private fun DataTable(modifier: Modifier) {
        val horizontalScrollState = rememberScrollState()
        val lazyListState = rememberLazyListState()

        LaunchedEffect(selectedTable) {
            searchJob?.cancel()
            searchJob = launch(Dispatchers.IO) {
                tableItems.clear()
                tableItems.addAll(getItems(""))
            }
        }

        Box(Modifier.fillMaxWidth(0.8f)) {
            LazyColumn(modifier, state = lazyListState) {
                itemsIndexed(items = tableItems.chunked(columns.size)) { rI, row ->
                    Row(Modifier.horizontalScroll(horizontalScrollState)) {
                        row.forEach { item ->
                            Surface(color = if (rI % 2 == 0) Color.LightGray else Color.LightGray.copy(alpha = 0.6f)) {
                                Text(item, modifier = Modifier.width(300.dp))
                            }
                        }
                    }
                }
            }

            HorizontalScrollbar(adapter = ScrollbarAdapter(horizontalScrollState), modifier = Modifier.align(Alignment.BottomStart))
            VerticalScrollbar(adapter = ScrollbarAdapter(lazyListState), modifier = Modifier.align(Alignment.TopEnd))
        }
    }

    @Composable
    private fun SearchBar(modifier: Modifier) {
        LaunchedEffect(searchText) {
            searchJob?.cancel()
            searchJob = launch(Dispatchers.IO) {
                tableItems.clear()
                tableItems.addAll(getItems(searchText))
            }
        }

        TextField(
            value = searchText,
            onValueChange = {
                searchText = it
            },
            placeholder = { Text("Search") },
            modifier = modifier,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searching) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    }
                    Icon(Icons.TwoTone.Search, null)
                }
            }
        )
    }

    @Composable
    private fun DropDownSelector(
        items: List<String>,
        modifier: Modifier,
        label: String,
        selected: Int,
        onSelect: (Int) -> Unit,
        expanded: Boolean,
        onDismiss: (Boolean) -> Unit
    ) {
        var mTextFieldSize by remember { mutableStateOf(Size.Zero) }

        val icon = if (expanded) Icons.Filled.KeyboardArrowUp
        else Icons.Filled.KeyboardArrowDown

        Column(modifier) {
            TextField(
                value = try {
                    items[selected]
                } catch (ignored: Exception) {
                    ""
                },
                onValueChange = { onSelect(items.indexOf(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        mTextFieldSize = coordinates.size.toSize()
                    },
                label = { Text(label) },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            onDismiss(!expanded)
                        },
                        content = {
                            Icon(icon, "expand")
                        }
                    )
                }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onDismiss(false) },
                modifier = Modifier.width(with(LocalDensity.current) { mTextFieldSize.width.toDp() })
            ) {
                items.forEach { column ->
                    DropdownMenuItem(onClick = {
                        onSelect(items.indexOf(column))
                        onDismiss(false)
                    }) {
                        Text(text = column)
                    }
                }
            }
        }
    }

    @Composable
    private fun Dialogs(dialogIntent: DialogIntent?, onChangeIntent: (DialogIntent?) -> Unit) {
        @Composable
        fun openFilePicker(onPick: (String) -> Unit) {
            FilePicker(true, projectDir) {
                onChangeIntent(null)
                it?.let { mpFile ->
                    if (Path(mpFile.path).extension == "db") {
                        onPick(mpFile.path)
                    } else {
                        onChangeIntent(DialogIntent.FileExtensionMismatch)
                    }
                }
            }
        }

        when (dialogIntent) {
            DialogIntent.OpenSourceFilePicker -> {
                openFilePicker { srcDb = it }
            }

            DialogIntent.OpenSyncFilePicker -> {
                openFilePicker { syncDb = it }
            }

            DialogIntent.FileExtensionMismatch -> {
                AlertDialog(
                    onDismissRequest = {
                        onChangeIntent(null)
                    },
                    buttons = {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = {
                                    onChangeIntent(null)
                                },
                            ) {
                                Text("Okay")
                            }
                        }
                    },
                    title = {
                        Text("Wrong file type")
                    },
                    text = {
                        Text("Please select a sqlite database with '.db' extension")
                    },
                )
            }

            else -> {}
        }
    }

    private suspend fun getItems(search: String): List<AnnotatedString> {
        searching = true
        val db = GetDatabase(srcDb)

        val allItems = mutableListOf<AnnotatedString>()
        allItems.addAll(columns.map { getHighlighted(it, emptyList()) })
        println("getItems called")
        try {
            val t = tables[selectedTable]
            val items = if (search.isBlank() || search.length < 3) {
                db.getFirst100Row(t)
            } else {
                val c = columns[selectedColumn]
                val fc = ColumnTrigrams(t, c)
                val suggestion = FuzzyCursor(db).getFuzzyWords(fc, search)
                suggestions.clear()
                suggestions.addAll(suggestion)
                println("suggestions $search --> $suggestions")

                val match = mutableListOf<String>()
                suggestions.forEach { s ->
                    val a = db.searchItems(t, c, s)
                    match.addAll(a)
                }
                match
            }.map { it.split(Database.SEPARATOR) }
                .flatten()
                .map { getHighlighted(it, suggestions) }

            allItems.addAll(items)
        } catch (ignored: Exception) {
        } finally {
            searching = false
        }


        return allItems
    }

    private fun getHighlighted(text: String, suggestions: List<String>): AnnotatedString {
        if (suggestions.isEmpty()) return buildAnnotatedString { append(text) }

        return buildAnnotatedString {
            append(text)
            suggestions.forEach {  sug ->
                try {
                    val startIndex = text.indexOf(sug)
                    if (startIndex >= 0) {
                        val endIndex = startIndex + sug.length
                        println("$sug $startIndex $endIndex")
                        addStyle(style = SpanStyle(background = Color.Yellow), start = startIndex, end = endIndex)
                    }
                } catch (e: Exception) { }
            }
        }
    }

    private suspend fun saveChanges(): Boolean {
        return withContext(Dispatchers.IO) {
            val update = Project(
                name = name,
                lastModified = System.currentTimeMillis(),
                projectDir, srcDb, syncDb
            )

            SaveProject(update)
        }
    }
}