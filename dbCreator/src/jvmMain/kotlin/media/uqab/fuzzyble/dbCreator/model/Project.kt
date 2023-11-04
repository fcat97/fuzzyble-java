package media.uqab.fuzzyble.dbCreator.model

data class Project(
    var name: String = "Untitled Project",
    var lastModified: Long = System.currentTimeMillis(),
    var projectDir: String = "",
    var srcDb: String = "",
    var syncDb: String = "",
)