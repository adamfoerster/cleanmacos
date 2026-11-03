package app.cleanmac

import java.nio.file.Path

enum class Group(val title: String, val description: String) {
    SystemJunk("System Junk", "Redundant files that clog up device storage and impede optimal performance."),
    Trash("Trash Bins", "Files already deleted that still take up space until the Trash is emptied."),
    Mail("Mail Attachments", "Attachments downloaded by Mail. They can be fetched again from the server."),
}

data class CleanItem(
    val id: String,
    val name: String,
    val path: Path,
    val size: Long,
    val selectedByDefault: Boolean,
    val children: List<CleanItem> = emptyList(),
    /** Set for simulator runtimes, which are deleted via simctl instead of the file system. */
    val runtime: SimRuntime? = null,
) {
    /** The items that are actually deleted: the item itself, or its children when it has any. */
    val leaves: List<CleanItem> get() = if (children.isEmpty()) listOf(this) else children.flatMap { it.leaves }
}

data class SubCategory(
    val id: String,
    val title: String,
    val description: String,
    val group: Group,
    val items: List<CleanItem>,
) {
    val size: Long get() = items.sumOf { it.size }
}

fun formatSize(bytes: Long): String {
    if (bytes < 1000) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var v = bytes.toDouble()
    var i = -1
    while (v >= 1000 && i < units.lastIndex) {
        v /= 1000; i++
    }
    return if (v >= 100 || i == 0) "%.0f %s".format(v, units[i]) else "%.1f %s".format(v, units[i])
}
