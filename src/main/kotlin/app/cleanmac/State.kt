package app.cleanmac

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortBy { Size, Name }

class CleanState(private val scope: CoroutineScope, private val scanner: Scanner = Scanner()) {
    private val cleaner = Cleaner(scanner.allowedRoots)

    var subs by mutableStateOf<List<SubCategory>>(emptyList())
    var scanning by mutableStateOf(false)
    var cleaning by mutableStateOf(false)
    var permissionDenied by mutableStateOf(false)
    var selectedSubId by mutableStateOf<String?>(null)
    var sortBy by mutableStateOf(SortBy.Size)
    var report by mutableStateOf<String?>(null)
    val checked = mutableStateMapOf<String, Boolean>()

    val selectedSub get() = subs.firstOrNull { it.id == selectedSubId } ?: subs.firstOrNull()
    private val allItems get() = subs.flatMap { it.items }.flatMap { it.leaves }
    val selectedItems get() = allItems.filter { checked[it.id] == true }
    val selectedBytes get() = selectedItems.sumOf { it.size }

    fun sorted(items: List<CleanItem>) =
        if (sortBy == SortBy.Size) items.sortedByDescending { it.size } else items.sortedBy { it.name.lowercase() }

    /** null = partially selected. */
    fun checkState(item: CleanItem): Boolean? {
        val states = item.leaves.map { checked[it.id] == true }
        return if (states.all { it }) true else if (states.none { it }) false else null
    }

    fun toggle(item: CleanItem) {
        val on = checkState(item) != true
        item.leaves.forEach { checked[it.id] = on }
    }

    fun scan() {
        if (scanning) return
        scanning = true
        scope.launch {
            val r = scanner.scan()
            subs = r.subCategories
            permissionDenied = r.permissionDenied
            checked.clear()
            r.subCategories.flatMap { it.items }.flatMap { it.leaves }.forEach { checked[it.id] = it.selectedByDefault }
            scanning = false
        }
    }

    fun select(sub: SubCategory, mode: String) {
        sub.items.flatMap { it.leaves }.forEach { checked[it.id] = mode == "all" || (mode == "smart" && it.selectedByDefault) }
    }

    fun clean() {
        val items = selectedItems
        if (items.isEmpty() || cleaning) return
        cleaning = true
        scope.launch {
            var freed = 0L
            val errors = mutableListOf<String>()
            withContext(Dispatchers.IO) {
                items.forEach {
                    try {
                        freed += cleaner.delete(it.path)
                    } catch (e: Exception) {
                        errors += "${it.name}: ${e.message}"
                    }
                }
            }
            report = "Freed ${formatSize(freed)}." +
                if (errors.isEmpty()) "" else "\n\n${errors.size} item(s) failed:\n" + errors.take(8).joinToString("\n")
            cleaning = false
            scanning = false
            scan()
        }
    }
}
