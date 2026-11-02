package app.cleanmac

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/** Total size in bytes of a file or directory tree. Symlinks are not followed; unreadable entries count as 0. */
fun sizeOf(path: Path): Long {
    var total = 0L
    try {
        Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (attrs.isRegularFile) total += attrs.size()
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path, exc: IOException) = FileVisitResult.CONTINUE
            override fun postVisitDirectory(dir: Path, exc: IOException?) = FileVisitResult.CONTINUE
        })
    } catch (_: IOException) {
    }
    return total
}

class UnsafePathException(message: String) : IllegalArgumentException(message)

/** Deletes only paths strictly inside one of [allowedRoots]. */
class Cleaner(private val allowedRoots: List<Path>) {
    private val roots = allowedRoots.map { it.toAbsolutePath().normalize() }
    private val realRoots = roots.mapNotNull { runCatching { it.toRealPath() }.getOrNull() }

    fun checkSafe(path: Path) {
        val p = path.toAbsolutePath().normalize()
        if (roots.none { p != it && p.startsWith(it) }) throw UnsafePathException("Outside allowed roots: $p")
        // A symlinked parent could redirect the delete elsewhere: resolve the parent and re-check.
        val realParent = p.parent?.toRealPath() ?: throw UnsafePathException("No parent: $p")
        if (realRoots.none { realParent.startsWith(it) }) throw UnsafePathException("Parent escapes allowed roots: $p")
    }

    /** Returns bytes freed. Never follows symlinks. */
    fun delete(path: Path): Long {
        checkSafe(path)
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return 0
        val size = sizeOf(path)
        Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.deleteIfExists(file)
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                Files.deleteIfExists(dir)
                return FileVisitResult.CONTINUE
            }
        })
        return size
    }
}
