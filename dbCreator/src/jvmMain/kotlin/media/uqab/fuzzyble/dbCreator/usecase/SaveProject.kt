package media.uqab.fuzzyble.dbCreator.usecase

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import media.uqab.fuzzyble.dbCreator.model.Project
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.exists

object SaveProject {
    suspend operator fun invoke(project: Project): Boolean {
        return withContext(Dispatchers.IO) {
            val path = Path(project.projectDir)
            if (!path.exists()) return@withContext false

            val json = Gson().toJson(project)
            val saveFilePath = Path(project.projectDir, "project_info.json")
            try { Files.delete(saveFilePath) } catch (ignored: Exception) { }
            Files.write(saveFilePath, json.toByteArray())

            return@withContext saveFilePath.exists()
        }
    }
}