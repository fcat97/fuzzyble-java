package media.uqab.fuzzyble.dbCreator.usecase

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import media.uqab.fuzzyble.dbCreator.model.Project
import java.util.prefs.Preferences

object SaveRecentProject {
    suspend operator fun invoke(project: Project) {
        val projectDir = mutableListOf<String>()
        projectDir.add(project.projectDir)
        projectDir.addAll(GetRecentProjects().map { it.projectDir })

        val type = object : TypeToken<List<String>>() {}.type
        val data = Gson().toJson(projectDir.take(5), type)

        Preferences.userRoot().node("fuzzyble.dbCreator")
            .put("recent-project", data)
    }
}