package app.cleanmac

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.*

class FakeRuntimes(val rts: List<SimRuntime> = emptyList()) : RuntimeManager {
    val deleted = mutableListOf<String>()
    override fun list() = rts
    override fun delete(runtime: SimRuntime): Long { deleted += runtime.id; return runtime.size }
}

class ScannerTest {
    private val home = Files.createTempDirectory("cm-home").toRealPath()

    private fun device(name: String, bytes: Int) {
        val d = Files.createDirectories(home.resolve("Library/Developer/Xcode/iOS DeviceSupport/$name"))
        Files.write(d.resolve("f"), ByteArray(bytes))
    }

    @Test fun deviceSupportIsSplitPerDevice() = runBlocking {
        device("iPhone A 26.6.2", 300)
        device("iPhone B 26.6.1", 100)
        val xcode = Scanner(home, FakeRuntimes()).scan().subCategories.first { it.id == "xcode" }
        val ds = xcode.items.first { it.id == "xc-devsupport" }
        assertEquals(listOf("iPhone A 26.6.2", "iPhone B 26.6.1"), ds.children.map { it.name })
        assertEquals(400, ds.size)
        assertEquals(2, ds.leaves.size)
    }

    @Test fun togglingParentAndChildren() = runBlocking {
        device("A", 300); device("B", 100)
        val s = CleanState(CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined), Scanner(home, FakeRuntimes()))
        s.subs = Scanner(home, FakeRuntimes()).scan().subCategories
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

    @Test fun parsesOnlyDeletableRuntimes() {
        val json = """{"A":{"identifier":"A","deletable":true,"version":"27.0","build":"24A","sizeBytes":5,"runtimeIdentifier":"com.apple.CoreSimulator.SimRuntime.iOS-27-0"},
            "B":{"identifier":"B","deletable":false,"version":"1.0","build":"x","sizeBytes":9,"runtimeIdentifier":"com.apple.CoreSimulator.SimRuntime.iOS-1-0"}}"""
        assertEquals(listOf(SimRuntime("A", "iOS 27.0", "24A", 5)), parseRuntimes(json))
    }

    @Test fun runtimesAreSelectableAndDeletedViaManager() = runBlocking {
        val a = SimRuntime("A", "iOS 27.0", "24A", 500)
        val b = SimRuntime("B", "iOS 26.5", "23F", 300)
        val fake = FakeRuntimes(listOf(a, b))
        val scanner = Scanner(home, fake)
        val s = CleanState(CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined), scanner, fake)
        s.subs = scanner.scan().subCategories
        val rt = s.subs.first { it.id == "xcode" }.items.first { it.id == "xc-runtimes" }
        assertEquals(800, rt.size)
        assertTrue(rt.leaves.none { s.checked[it.id] == true })
        s.checked[rt.children[1].id] = true
        s.clean()
        kotlinx.coroutines.delay(300)
        assertEquals(listOf("B"), fake.deleted)
    }
}
