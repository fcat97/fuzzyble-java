package media.uqab.fuzzyble.dbCreator.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import media.uqab.fuzzybleJava.FuzzyColumn
import media.uqab.fuzzybleJava.FuzzyCursor

object CreateFuzzyTable {
    suspend operator fun invoke(cursor: FuzzyCursor, col: FuzzyColumn, force: Boolean = false) {
        withContext(Dispatchers.IO) {
            if (!cursor.isFuzzyble(col)) {
                cursor.createFuzzyble(col, force)
            }

            cursor.populate(col, force)
        }
    }
}