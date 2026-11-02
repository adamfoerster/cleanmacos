package app.cleanmac

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.*

class ScannerTest {
    private val home = Files.createTempDirectory("cm-home").toRealPath()

    private fun device(name: String, bytes: Int) {
        val d = Files.createDirectories(home.resolve("Library/Developer/Xcode/iOS DeviceSupport/$name"))
        Files.write(d.resolve("f"), ByteArray(bytes))
    }

    @Test fun deviceSupportIsSplitPerDevice() = runBlocking {
        device("iPhone A 26.6.2", 300)
        device("iPhone B 26.6.1", 100)
        val xcode = Scanner(home).scan().subCategories.first { it.id == "xcode" }
        val ds = xcode.items.first { it.id == "xc-devsupport" }
        assertEquals(listOf("iPhone A 26.6.2", "iPhone B 26.6.1"), ds.children.map { it.name })
        assertEquals(400, ds.size)
        assertEquals(2, ds.leaves.size)
    }

    @Test fun togglingParentAndChildren() = runBlocking {
        device("A", 300); device("B", 100)
        val s = CleanState(CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined), Scanner(home))
        s.subs = Scanner(home).scan().subCategories
        val ds = s.subs.first { it.id == "xcode" }.items.first { it.id == "xc-devsupport" }
        ds.leaves.forEach { s.checked[it.id] = false }
        s.checked[ds.children[0].id] = true
        assertNull(s.checkState(ds))
        assertEquals(300, s.selectedBytes)
        s.toggle(ds)
        assertEquals(true, s.checkState(ds))
        s.toggle(ds)
        assertEquals(false, s.checkState(ds))
        assertEquals(0, s.selectedBytes)
    }
}
