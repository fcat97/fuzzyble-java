package media.uqab.fuzzyble.dbCreator.usecase

import kotlinx.coroutines.*
import media.uqab.fuzzybleJava.FuzzyColumn
import media.uqab.fuzzybleJava.FuzzyCursor

object EnableFuzzySearch {
    suspend operator fun invoke(
        cursor: FuzzyCursor,
        col: FuzzyColumn,
        force: Boolean = false,
        onProgress: (Float) -> Unit) {
        withContext(Dispatchers.IO) {

            cursor.createFuzzyble(col, force)
            cursor.populate(col, force, onProgress)
        }
    }
}