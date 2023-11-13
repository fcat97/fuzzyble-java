package media.uqab.fuzzyble.dbCreator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import media.uqab.fuzzyble.dbCreator.utils.AsyncImage
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
    private var matchAllWords by mutableStateOf(false)
    private var searchText by mutableStateOf("")
    private var tableItems by mutableStateOf<List<AnnotatedString>>(emptyList())

    private var isFuzzyble by mutableStateOf(false)

    @Composable
    override fun Content() = ProjectHomeContent()

    private enum class DialogIntent {
        OpenSourceFilePicker,
        OpenSyncFilePicker,
        FileExtensionMismatch
    }

    @Composable
    private fun ProjectHomeContent() {
        val coroutine = rememberCoroutineScope()

        // show dialog based on intent
        Dialogs(dialogIntent) { dialogIntent = it }

        var screenWidth by remember { mutableStateOf(100) }
        val isMobileScreen by remember { derivedStateOf { screenWidth < 1080 } }

        LaunchedEffect(Unit) {
            val db = GetDatabase(srcDb)
            db.getTables().forEach {
                tables.add(it)
            }

            if (tables.isNotEmpty()) {
                onSelectTable(0)
            }
        }

        Scaffold(
            modifier = Modifier.onGloballyPositioned {
                screenWidth = it.size.width
            },
            topBar = { TitleBar() },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        coroutine.launch {
                            event = if (saveChanges()) {
                                Event.Success("Changes Saved")
                            } else {
                                Event.Failure("Failed to save changes")
                            }
                        }
                    },
                ) {
                    AsyncImage("https://openclipart.org/image/800px/237989")
                }
            }
        ) {
            if (isMobileScreen) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProjectControlFields(Modifier.fillMaxWidth())

                    AnimatedVisibility(isFuzzyble) {
                        SearchBar(Modifier.fillMaxWidth())
                    }

                    DataTable(Modifier.fillMaxWidth())
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize().padding(30.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProjectControlFields(Modifier.fillMaxWidth(0.4f))

                    Column {
                        AnimatedVisibility(isFuzzyble) {
                            SearchBar(Modifier.fillMaxWidth())
                        }

                        DataTable(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    @Composable
    private fun TitleBar() {
        Column {
            TopAppBar(
                title = { Text(name) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            ScreenManager.peek().pop()
                        },
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, null)
                    }
                },
            )

            EventBar(modifier = Modifier.fillMaxWidth(), event) { event = null }

            AnimatedVisibility(isOperationRunning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    @Composable
    private fun ProjectControlFields(modifier: Modifier) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),

                label = { Text("Project Name") }
            )

            DatabaseLocation()

            DatabaseColumnSelection()

            TableInfo()
        }
    }

    @Composable
    private fun DatabaseLocation() {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
        val coroutine = rememberCoroutineScope()
        var expandTableDropDown by remember { mutableStateOf(false) }
        var expandColumnDropDown by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DropDownSelector(
                modifier = Modifier.fillMaxWidth(48 / 100f),
                label = "Source DB table",
                items = tables,
                selected = selectedTable,
                onSelect = { coroutine.launch { onSelectTable(it) } },
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
    private fun TableInfo() {
        val coroutine = rememberCoroutineScope()
        var show by remember { mutableStateOf(false) }
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                Icon(
                    imageVector = if (isFuzzyble) Icons.TwoTone.CheckCircle else Icons.TwoTone.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isFuzzyble) Color.Green else Color.LightGray
                )

                Text("Fuzzyble Column")


                Icon(
                    imageVector = if (isPopulated) Icons.TwoTone.CheckCircle else Icons.TwoTone.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isPopulated) Color.Green else Color.LightGray
                )

                Text("Column Populated")

                TextButton(
                    onClick = {
                        coroutine.launch { enableFuzzySearch() }
                    },
                    content = {
                        Text("Enable Fuzzy Search")
                    }
                )
            }
        }
    }

    @Composable
    private fun DataTable(modifier: Modifier) {
        val horizontalScrollState = rememberScrollState()
        val lazyListState = rememberLazyListState()
        var tableRows by remember { mutableStateOf<List<List<AnnotatedString>>>(emptyList()) }

        LaunchedEffect(tableItems) {
            tableRows = if (columns.isEmpty()) emptyList() else tableItems.chunked(columns.size)
        }

        Box(modifier) {
            LazyColumn(Modifier.fillMaxSize().horizontalScroll(horizontalScrollState), state = lazyListState) {
                itemsIndexed(items = tableRows) { rI, row ->
                    Row {
                        row.forEach { item ->
                            Surface(color = if (rI % 2 == 0) Color.LightGray else Color.LightGray.copy(alpha = 0.6f)) {
                                Text(item, modifier = Modifier.width(300.dp))
                            }
                        }
                    }
                }
            }

            HorizontalScrollbar(
                adapter = ScrollbarAdapter(horizontalScrollState),
                modifier = Modifier.align(Alignment.BottomStart)
            )
            VerticalScrollbar(adapter = ScrollbarAdapter(lazyListState), modifier = Modifier.align(Alignment.TopEnd))
        }
    }

    @Composable
    private fun SearchBar(modifier: Modifier) {
        LaunchedEffect(searchText, matchAllWords) {
            searchJob?.cancel()
            searchJob = launch(Dispatchers.IO) {
                getItems(searchText, matchAllWords)
            }
        }

        TextField(
            value = searchText,
            onValueChange = {
                searchText = it
            },
            placeholder = { Text("Search") },
            modifier = modifier,
            leadingIcon = {
                Icon(Icons.TwoTone.Search, null)
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("found ${tableItems.size} items", modifier = Modifier.padding(end = 12.dp))

                    if (searching) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    }

                    Switch(matchAllWords, onCheckedChange = { matchAllWords = it })
                    TextButton(
                        onClick = {
                            matchAllWords = !matchAllWords
                        },
                        content = {
                            Text("Match All")
                        },
                    )
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

    private suspend fun onSelectTable(index: Int) {
        try {
            selectedTable = index
            val db = GetDatabase(srcDb)
            columns.clear()
            db.getColumns(tables[selectedTable]).let {
                columns.addAll(it)
            }

            if (columns.isNotEmpty()) {
                selectedColumn = 0
            }

            getItems(searchText, matchAllWords)
        } catch (ignored: Exception) {
        }
    }

    private suspend fun getItems(
        search: String,
        matchAllWords: Boolean = false
    ) {
        searching = true
        val db = GetDatabase(srcDb)

        val allItems = mutableListOf<AnnotatedString>()
        allItems.addAll(columns.map { getHighlighted(it, emptyList()) })

        try {
            val suggestions = hashMapOf<String, List<String>>()

            val t = tables[selectedTable]
            val c = columns[selectedColumn]

            val match = if (search.isBlank()) {
                db.getFirst100Row(t)
            } else if (search.length < 3) {
                // don't make fuzzy search for these words, it won't give good result
                suggestions[search] = listOf(search)
                db.searchItems(t, c, search)
            } else {
                val fc = ColumnWordLen(t, c)
                val cursor = FuzzyCursor(db)

                search.split(" ").forEach { word ->
                    if (word.length > 2) {
                        cursor.getFuzzyWords(fc, word).let {
                            suggestions[word] = it.toList()
                        }
                    } else {
                        // when searched word is less than 2 char,
                        // fuzzy match provides inaccurate result
                        suggestions[word] = listOf(word)
                    }
                }

                println("suggestions $search --> $suggestions")

                val matchedRow = mutableListOf<String>()

                if (matchAllWords) {
                    // matching rows
                    var m = mutableListOf<String>()

                    for (key in suggestions.keys) {
                        val ws = suggestions[key] ?: emptyList()

                        // if previous match contains any of these words or not
                        // if not, break the loop and return empty list
                        if (m.isNotEmpty()) {
                            m = m.filter { row ->
                                ws.any { row.contains(it) }
                            }.toMutableList()

                            // no match, break the search
                            if (m.isEmpty()) break
                        }

                        // search for each word's suggestions and add to the existing list
                        for (s in ws) {
                            m.addAll(db.searchItems(t, c, s))
                        }

                        // since we want to match all words,
                        // zero result for any word's suggestion will
                        // ultimately return zero result
                        if (m.isEmpty()) {
                            m.clear()
                            break
                        }
                    }

                    matchedRow.addAll(m)
                } else {
                    // include match for any of the suggestions
                    suggestions.values.flatten().forEach { s ->
                        val a = db.searchItems(t, c, s)         // get rows from source db
                        matchedRow.addAll(a)
                    }
                }
                matchedRow
            }

            val items = match.map {             // List<RowData> joined columns of row
                it.split(Database.SEPARATOR)    // List<Row<Column>>
            }.flatten().map {           // List<Column>
                getHighlighted(it, suggestions.values.flatten())
            }

            allItems.addAll(items)
        } catch (ignored: Exception) {
        } finally {
            searching = false
        }

        tableItems = allItems
    }

    private fun getHighlighted(text: String, suggestions: List<String>): AnnotatedString {
        if (suggestions.isEmpty()) return buildAnnotatedString { append(text) }

        return buildAnnotatedString {
            append(text)
            suggestions.forEach { sug ->
                try {
                    val startIndex = text.indexOf(sug)
                    if (startIndex >= 0) {
                        val endIndex = startIndex + sug.length
                        addStyle(style = SpanStyle(background = Color.Yellow), start = startIndex, end = endIndex)
                    }
                } catch (ignored: Exception) {

                }
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

    private suspend fun enableFuzzySearch() {
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
}