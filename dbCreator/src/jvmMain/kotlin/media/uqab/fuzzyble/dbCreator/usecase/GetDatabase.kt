package media.uqab.fuzzyble.dbCreator.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import media.uqab.fuzzyble.dbCreator.database.Database

object GetDatabase {
    suspend operator fun invoke(path: String): Database? {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                Database(url = path)
            } catch (e: Exception) {
                println(e.message)
                null
            }
        }
    }
}