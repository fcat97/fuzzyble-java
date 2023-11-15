package media.uqab.fuzzyble.dbCreator.usecase

import kotlinx.coroutines.*
import media.uqab.fuzzybleJava.FuzzyColumn
import media.uqab.fuzzybleJava.FuzzyCursor

object EnableFuzzySearch {
    suspend operator fun invoke(
        data: List<String>,
        cursor: FuzzyCursor,
        col: FuzzyColumn,
        force: Boolean = false,
        onProgress: (Float) -> Unit) {
        withContext(Dispatchers.IO) {
            var progress = 0f
            val step = 1f / data.size

            cursor.createFuzzyble(col, force)
            if (force) {
                cursor.markPopulated(col, false)
            }

            if (cursor.isPopulated(col)) return@withContext
            else {
                data.chunked(10).map { chunk ->
                    chunk.map {
                        async {
                            cursor.addToFuzzySearch(col, it)
                            progress += step
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }.awaitAll()
                }

                cursor.markPopulated(col, true)
            }

        }
    }
}