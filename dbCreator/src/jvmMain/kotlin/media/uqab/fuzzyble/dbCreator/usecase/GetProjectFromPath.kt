package media.uqab.fuzzyble.dbCreator.usecase

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import media.uqab.fuzzyble.dbCreator.model.Project
import java.nio.charset.Charset
import java.nio.file.Files
import kotlin.io.path.Path

object GetProjectFromPath {
    suspend operator fun invoke(projectPath: String): Project? {
        return withContext(Dispatchers.IO) {
            val path = Path(projectPath)
            if (!Files.exists(path)) return@withContext null
            if (Files.isRegularFile(path)) return@withContext null

            val pFile = Path(projectPath, "project_info.json")
            if(!Files.exists(pFile)) return@withContext null
            
            val content = Files.readAllBytes(pFile).toString(Charset.defaultCharset())
            val data = try {
                Gson().fromJson(content, Project::class.java)
            } catch (ignored: Exception) {
                null
            }

            return@withContext data
        }
    }
}