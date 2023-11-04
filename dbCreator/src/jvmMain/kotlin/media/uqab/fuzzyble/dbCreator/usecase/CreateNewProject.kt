package media.uqab.fuzzyble.dbCreator.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import media.uqab.fuzzyble.dbCreator.model.Project

object CreateNewProject {
    suspend operator fun invoke(projectDir: String?): Project? {
        return withContext(Dispatchers.IO) {
            if (projectDir == null) return@withContext null

            val existing = GetProjectFromPath(projectDir)
            if (existing != null) return@withContext existing

            val newProject = Project(projectDir = projectDir)
            if (SaveProject(newProject)) {
                return@withContext newProject
            } else {
                return@withContext null
            }
        }
    }
}