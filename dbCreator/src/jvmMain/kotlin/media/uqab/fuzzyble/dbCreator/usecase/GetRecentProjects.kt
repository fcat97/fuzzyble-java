package media.uqab.fuzzyble.dbCreator.usecase

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import media.uqab.fuzzyble.dbCreator.model.Project
import java.util.prefs.Preferences

object GetRecentProjects {
    suspend operator fun invoke(): List<Project> {
        val data = Preferences.userRoot().node("fuzzyble.dbCreator")
            .get("recent-project", "[]")

        val type = object : TypeToken<List<String>>() {}.type
        val recentPath = Gson().fromJson<List<String>>(data, type).distinct()
        return recentPath.mapNotNull {
            GetProjectFromPath(it)
        }
    }
}