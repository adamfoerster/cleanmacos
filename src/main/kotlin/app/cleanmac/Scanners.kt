package app.cleanmac

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class ScanResult(val subCategories: List<SubCategory>, val permissionDenied: Boolean)

class Scanner(private val home: Path = Path.of(System.getProperty("user.home"))) {
    private val lib = home.resolve("Library")

    /** Roots the Cleaner is allowed to delete from. */
    val allowedRoots: List<Path> = listOf(
        lib.resolve("Developer/Xcode"),
        lib.resolve("Developer/CoreSimulator/Caches"),
        lib.resolve("Caches"),
        lib.resolve("Logs"),
        lib.resolve("Containers/com.apple.mail/Data/Library/Mail Downloads"),
        home.resolve(".Trash"),
    )

    private var denied = false

    private fun children(dir: Path, selected: Boolean = true, idPrefix: String = dir.toString()): List<CleanItem> {
        if (!dir.isDirectory()) return emptyList()
        return try {
            Files.newDirectoryStream(dir).use { s ->
                s.map { CleanItem("$idPrefix/${it.name}", it.name, it, 0, selected) }
            }
        } catch (_: java.nio.file.AccessDeniedException) {
            denied = true; emptyList()
        } catch (_: java.io.IOException) {
            emptyList()
        }
    }

    private suspend fun sized(items: List<CleanItem>): List<CleanItem> = coroutineScope {
        items.map { async(Dispatchers.IO) { it.copy(size = sizeOf(it.path)) } }
            .map { it.await() }
            .filter { it.size > 0 }
    }

    suspend fun scan(): ScanResult = withContext(Dispatchers.IO) {
        denied = false
        val xcode = lib.resolve("Developer/Xcode")
        // Heavy or irreplaceable items are not pre-selected.
        val xcodeItems = buildList {
            add(CleanItem("xc-derived", "Derived Data", xcode.resolve("DerivedData"), 0, true))
            add(CleanItem("xc-devsupport", "iOS Device Support", xcode.resolve("iOS DeviceSupport"), 0, false))
            add(CleanItem("xc-watch", "watchOS Device Support", xcode.resolve("watchOS DeviceSupport"), 0, false))
            add(CleanItem("xc-archives", "Archives", xcode.resolve("Archives"), 0, false))
            add(CleanItem("xc-products", "Products", xcode.resolve("Products"), 0, true))
            add(CleanItem("xc-simcache", "Simulator Caches", lib.resolve("Developer/CoreSimulator/Caches"), 0, true))
        }.filter { it.path.exists() }

        val subs = listOf(
            SubCategory("xcode", "Xcode Junk",
                "Xcode generates a significant amount of intermediate build information and project index.",
                Group.SystemJunk, sized(xcodeItems)),
            SubCategory("usercache", "User Cache Files",
                "Temporary files apps keep to load faster. They are recreated when needed.",
                Group.SystemJunk, sized(children(lib.resolve("Caches")))),
            SubCategory("userlogs", "User Log Files",
                "Logs written by apps and services for your user.",
                Group.SystemJunk, sized(children(lib.resolve("Logs")))),
            SubCategory("trash", "Trash", Group.Trash.description,
                Group.Trash, sized(children(home.resolve(".Trash"), selected = true))),
            SubCategory("mail", "Mail Downloads", Group.Mail.description,
                Group.Mail, sized(children(lib.resolve("Containers/com.apple.mail/Data/Library/Mail Downloads"), selected = false))),
        ).map { it.copy(items = it.items.sortedByDescending { i -> i.size }) }

        ScanResult(subs, denied)
    }
}
