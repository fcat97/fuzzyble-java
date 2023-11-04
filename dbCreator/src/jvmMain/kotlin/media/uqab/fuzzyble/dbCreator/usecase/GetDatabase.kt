package media.uqab.fuzzyble.dbCreator.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import media.uqab.fuzzyble.dbCreator.database.Database

object GetDatabase {
    private var mDatabase: Database? = null

    suspend operator fun invoke(dbName: String): Database {
        return withContext(Dispatchers.IO) {
            if (mDatabase == null) {
                mDatabase = Database().apply {
                    openDatabase(dbName)
                }
            }

            return@withContext mDatabase!!
        }
    }
}