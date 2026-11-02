package app.cleanmac

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class CleanerTest {
    private val root: Path = Files.createTempDirectory("cm").toRealPath()
    private val allowed = Files.createDirectory(root.resolve("allowed"))
    private val cleaner = Cleaner(listOf(allowed))

    @Test fun deletesTreeAndReportsSize() {
        val d = Files.createDirectories(allowed.resolve("a/b"))
        Files.write(d.resolve("f"), ByteArray(1234))
        assertEquals(1234, cleaner.delete(allowed.resolve("a")))
        assertFalse(Files.exists(allowed.resolve("a")))
    }

    @Test fun refusesOutsideAndRoot() {
        val other = Files.createDirectory(root.resolve("other"))
        assertFailsWith<UnsafePathException> { cleaner.delete(other) }
        assertFailsWith<UnsafePathException> { cleaner.delete(allowed) }
        assertFailsWith<UnsafePathException> { cleaner.delete(allowed.resolve("../other")) }
    }

    @Test fun doesNotFollowSymlinks() {
        val other = Files.createDirectory(root.resolve("other"))
        Files.write(other.resolve("keep"), ByteArray(10))
        Files.createSymbolicLink(allowed.resolve("link"), other)
        cleaner.delete(allowed.resolve("link"))
        assertTrue(Files.exists(other.resolve("keep")))
        assertFalse(Files.exists(allowed.resolve("link"), java.nio.file.LinkOption.NOFOLLOW_LINKS))
    }

    @Test fun refusesSymlinkedParent() {
        val other = Files.createDirectory(root.resolve("other"))
        Files.write(other.resolve("keep"), ByteArray(10))
        Files.createSymbolicLink(allowed.resolve("link"), other)
        assertFailsWith<UnsafePathException> { cleaner.delete(allowed.resolve("link/keep")) }
        assertTrue(Files.exists(other.resolve("keep")))
    }
}
