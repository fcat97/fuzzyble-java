package media.uqab.fuzzyble.dbCreator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
import kotlinx.coroutines.*
import media.uqab.fuzzyble.dbCreator.database.Database
import media.uqab.fuzzyble.dbCreator.model.Project
import media.uqab.fuzzyble.dbCreator.usecase.EnableFuzzySearch
import media.uqab.fuzzyble.dbCreator.usecase.GetDatabase
import media.uqab.fuzzyble.dbCreator.usecase.SaveProject
import media.uqab.fuzzyble.dbCreator.utils.AsyncImage
import media.uqab.fuzzybleJava.*
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ProjectScreen(project: Project) : Screen {
    private var event by mutableStateOf<Event?>(null)
    private var dialogIntent by mutableStateOf<DialogIntent?>(null)
    private var isMobileScreen by mutableStateOf(false)

    private var name by mutableStateOf(project.name)
    private var projectDir by mutableStateOf(project.projectDir)
    private var srcDb by mutableStateOf(project.srcDb)
    private var syncDb by mutableStateOf(project.syncDb)

    private val tables = mutableStateListOf<String>()
    private val columns = mutableStateListOf<String>()
    private var selectedTable by mutableStateOf(-1)
    private var selectedColumn by mutableStateOf(-1)

    private var isOperationRunning by mutableStateOf(false)
    private var operationProgress by mutableStateOf(0f)
    private var reloadTableStatus by mutableStateOf(0)

    private var searchJob: Job? = null
    private var searching by mutableStateOf(false)
    private var matchAllWords by mutableStateOf(false)
    private var searchText by mutableStateOf("")
    private var tableRows = mutableStateListOf<List<AnnotatedString>>()

    private var isFuzzyble by mutableStateOf(false)
    private val fuzzyMethods = listOf(Methods.Trigram2, Methods.Trigram, Methods.WordLen)
    private var selectedMethod by mutableStateOf(Methods.Trigram2)

    private enum class DialogIntent {
        OpenSourceFilePicker,
        OpenSyncFilePicker,
        FileExtensionMismatch
    }

    private enum class Methods(val label: String) {
        Trigram2("Trigram (optimized, slow)"),
        Trigram("Trigram (fast)"),
        WordLen("WordLen")
    }

    @Composable
    override fun Content() {
        val coroutine = rememberCoroutineScope()

        DisposableEffect(Unit) {
            onDispose {
                immutableDb?.close()
                mutableDb?.close()
            }
        }

        // show dialog based on intent
        Dialogs(dialogIntent) { dialogIntent = it }

        var screenWidth by remember { mutableStateOf(100) }
        val mobileScreen by remember { derivedStateOf { screenWidth < 1080 } }
        LaunchedEffect(mobileScreen) {
            isMobileScreen = mobileScreen
        }


        LaunchedEffect(Unit) {
            openSrcDatabase(srcDb)
            openSyncDatabase(syncDb)
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
                    AsyncImage(Icons.save)
                }
            }
        ) {
            if (isMobileScreen) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
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
                        AsyncImage(Icons.back)
                    }
                },
                actions = {
                    AnimatedVisibility(isOperationRunning) {
                        Text(String.format("%.2f", 100 * operationProgress) + "%")
                    }

                    AnimatedVisibility(isOperationRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            progress = operationProgress,
                            color = MaterialTheme.colors.secondary
                        )
                    }
                }
            )

            EventBar(modifier = Modifier.fillMaxWidth(), event) { event = null }

            AnimatedVisibility(isOperationRunning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colors.secondary)
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
        val coroutine = rememberCoroutineScope()

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TextField(
                value = srcDb,
                onValueChange = { },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Source Database")
                },
                trailingIcon = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {
                                coroutine.launch {
                                    openSrcDatabase(srcDb)
                                }
                            },
                            content = {
                                AsyncImage(Icons.reload)
                            }
                        )

                        IconButton(onClick = { dialogIntent = DialogIntent.OpenSourceFilePicker }) {
                            AsyncImage(Icons.upload)
                        }
                    }
                }
            )

            TextField(
                value = syncDb,
                onValueChange = { syncDb = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Destination Database")
                },
                trailingIcon = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {
                                coroutine.launch {
                                    openSyncDatabase(syncDb)
                                }
                            },
                            content = {
                                AsyncImage(Icons.reload)
                            }
                        )

                        IconButton(onClick = { dialogIntent = DialogIntent.OpenSyncFilePicker }) {
                            AsyncImage(Icons.upload)
                        }
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
                onSelect = { onSelectColumn(it) },
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
        var showMethodDropDown by remember { mutableStateOf(false) }

        LaunchedEffect(reloadTableStatus, selectedMethod) {
            show = mutableDb != null

            try {
                if (show) {
                    val cursor = getCursor()

                    isFuzzyble = cursor.isFuzzyble(fuzzyColumn)
                    isPopulated = cursor.isPopulated(fuzzyColumn)
                }
            } catch (ignore: Exception) {

            }
        }

        AnimatedVisibility(show) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DropDownSelector(
                    modifier = Modifier.fillMaxWidth(),
                    label = "Select Fuzzy Method",
                    items = fuzzyMethods.map { it.label },
                    selected = when (selectedMethod) {
                        Methods.Trigram2 -> 0
                        Methods.Trigram -> 1
                        Methods.WordLen -> 2
                    },
                    onSelect = {
                        selectedMethod = when (it) {
                            0 -> Methods.Trigram2
                            1 -> Methods.Trigram
                            else -> Methods.WordLen
                        }
                    },
                    expanded = showMethodDropDown,
                    onDismiss = { showMethodDropDown = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AsyncImage(if (isFuzzyble) Icons.checkMark else Icons.warning) {
                        Image(it, null)
                    }

                    Text("Fuzzyble Column")

                    AsyncImage(if (isPopulated) Icons.checkMark else Icons.warning) {
                        Image(it, null)
                    }

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
    }

    @Composable
    private fun DataTable(modifier: Modifier) {
        val horizontalScrollState = rememberScrollState()
        val lazyListState = rememberLazyListState()

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
                AsyncImage(Icons.search)
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("found ${tableRows.size - 1} items", modifier = Modifier.padding(end = 12.dp))

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
                            AsyncImage(Icons.rightArrow) {
                                Icon(
                                    it,
                                    null,
                                    modifier = Modifier.rotate(if (expanded) 90f else 0f).size(24.dp)
                                )
                            }
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
        val coroutine = rememberCoroutineScope()

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
                openFilePicker {
                    srcDb = it
                    coroutine.launch {
                        openSrcDatabase(it)
                    }
                }
            }

            DialogIntent.OpenSyncFilePicker -> {
                openFilePicker {
                    syncDb = it
                    coroutine.launch {
                        openSyncDatabase(it)
                    }
                }
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
            columns.clear()
            getImmutableDb().getColumns(tables[selectedTable]).let {
                columns.addAll(it)
            }

            onSelectColumn(0)

            getItems(searchText, matchAllWords)
        } catch (ignored: Exception) {
        }
    }

    private fun onSelectColumn(index: Int) {
        if (columns.isNotEmpty()) {
            selectedColumn = index
            reloadTableStatus += 1
        }
    }

    private suspend fun getItems(
        search: String,
        matchAllWords: Boolean = false
    ) {
        searching = true
        val db = getImmutableDb()

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
                val cursor = getCursor()

                search.split(" ").forEach { word ->
                    if (word.length > 2) {
                        cursor.getFuzzyWords(fuzzyColumn, word).let {
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

        tableRows.clear()
        if (columns.isNotEmpty()) {
            tableRows.addAll(allItems.chunked(columns.size))
        }
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

    private suspend fun openSrcDatabase(path: String) {
        if (path.isNotBlank()) {
            immutableDb?.close()
            immutableDb = null

            tables.clear()
            getImmutableDb().getTables().forEach {
                tables.add(it)
            }
        }

        if (tables.isNotEmpty()) {
            onSelectTable(0)
        }
    }

    private suspend fun openSyncDatabase(path: String) {
        if (path.isNotBlank()) {
            mutableDb?.close()
            mutableDb = null

            getMutableDb()
        }
    }

    private suspend fun saveChanges(): Boolean {
        return withContext(Dispatchers.IO) {
            val update = Project(
                name = name,
                lastModified = System.currentTimeMillis(),
                projectDir,
                srcDb,
                syncDb
            )

            SaveProject(update)
        }
    }

    private suspend fun enableFuzzySearch() {
        try {
            event = Event.Success("Enabling Fuzzy Search")
            isOperationRunning = true
            val cur = getCursor()
            EnableFuzzySearch(cur, fuzzyColumn, true) {
                operationProgress = it
            }
            event = Event.Success("Fuzzy Search Enabled")

            reloadTableStatus += 1
        } catch (ignore: Exception) {
        } finally {
            isOperationRunning = false
        }

    }

    // fuzzy db -------------------------------------------
    private var immutableDb: Database? = null
    private suspend fun getImmutableDb(): Database {
        if (immutableDb == null) {
            immutableDb = GetDatabase(srcDb)
        }
        return immutableDb!!
    }

    private var mutableDb: Database? = null
    private suspend fun getMutableDb(): Database {
        if (mutableDb == null) {
            mutableDb = GetDatabase(syncDb)
        }
        return mutableDb!!
    }

    private val strategy: Strategy
        get() = when(selectedMethod) {
            Methods.Trigram2 -> Trigram2()
            Methods.Trigram -> Trigram()
            Methods.WordLen -> WordLen()
        }
    private val fuzzyColumn: FuzzyColumn get() = FuzzyColumn(tables[selectedTable], columns[selectedColumn])
    private suspend fun getCursor(): FuzzyCursor {
        return FuzzyCursor(getImmutableDb(), getMutableDb(), strategy)
    }
}