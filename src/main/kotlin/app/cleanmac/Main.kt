package app.cleanmac

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

private val Accent = Color(0xFFC2189E)
private val Selected = Color(0xFFE9E5F5)
private val Muted = Color(0xFF6B6B7B)

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "CleanMac", state = rememberWindowState(width = 1100.dp, height = 700.dp)) {
        MaterialTheme(colorScheme = lightColorScheme(primary = Accent)) {
            val scope = rememberCoroutineScope()
            val state = remember { CleanState(scope).also { it.scan() } }
            App(state)
        }
    }
}

@Composable
fun App(s: CleanState) {
    var confirm by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = Color.White) {
        Column {
            if (s.permissionDenied) {
                Row(Modifier.fillMaxWidth().background(Color(0xFFFFF3CD)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Some folders (Trash, Mail) need Full Disk Access to be scanned.", Modifier.weight(1f))
                    TextButton(onClick = {
                        ProcessBuilder("open", "x-apple.systempreferences:com.apple.preference.security?Privacy_AllFiles").start()
                    }) { Text("Open Settings") }
                }
            }
            Row(Modifier.weight(1f)) {
                SubPane(s, Modifier.width(320.dp).fillMaxHeight())
                VerticalDivider()
                ItemPane(s, Modifier.weight(1f).fillMaxHeight())
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().background(Color(0xFFF6F5FA)).padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { s.scan() }, enabled = !s.scanning && !s.cleaning) { Text("Rescan") }
                Spacer(Modifier.weight(1f))
                Text("${s.selectedItems.size} Items Selected", fontWeight = FontWeight.Medium)
                Text("  |  ${formatSize(s.selectedBytes)}", color = Muted)
                Spacer(Modifier.width(24.dp))
                Button(
                    onClick = { confirm = true },
                    enabled = s.selectedItems.isNotEmpty() && !s.cleaning && !s.scanning,
                ) { Text(if (s.cleaning) "Cleaning…" else "Clean") }
            }
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Delete ${s.selectedItems.size} items?") },
        text = { Text("${formatSize(s.selectedBytes)} will be permanently deleted. This cannot be undone.") },
        confirmButton = { TextButton(onClick = { confirm = false; s.clean() }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
    )
    s.report?.let {
        AlertDialog(
            onDismissRequest = { s.report = null },
            title = { Text("Done") },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = { s.report = null }) { Text("OK") } },
        )
    }
}

@Composable
private fun Row(selected: Boolean, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) =
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Selected else Color.Transparent)
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )

@Composable
private fun SubPane(s: CleanState, modifier: Modifier) = Column(modifier.padding(top = 24.dp)) {
    Text("System Junk", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 24.dp))
    Text("Redundant files that clog up device storage and impede optimal performance.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(24.dp, 8.dp, 24.dp, 16.dp))
    if (s.scanning) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 24.dp))
    LazyColumn {
        items(s.subs, key = { it.id }) { sub ->
            Row(sub.id == s.selectedSub?.id, { s.selectedSubId = sub.id }) {
                Column(Modifier.weight(1f)) {
                    Text(sub.title, fontWeight = FontWeight.Medium)
                    Text("${sub.items.size} items", fontSize = 12.sp, color = Muted)
                }
                Text(formatSize(sub.size), color = Muted)
            }
        }
    }
}

@Composable
private fun ItemPane(s: CleanState, modifier: Modifier) = Column(modifier.padding(top = 24.dp)) {
    val sub = s.selectedSub
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(sub?.title ?: "Nothing here", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        var sortMenu by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { sortMenu = true }) { Text("Sort by: ${s.sortBy}") }
            DropdownMenu(sortMenu, { sortMenu = false }) {
                SortBy.entries.forEach { DropdownMenuItem(text = { Text(it.name) }, onClick = { s.sortBy = it; sortMenu = false }) }
            }
        }
    }
    if (sub == null) return@Column
    Text(sub.description, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(24.dp, 4.dp, 24.dp, 8.dp))
    Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Select:", fontWeight = FontWeight.Medium)
        TextButton(onClick = { s.select(sub, "smart") }) { Text("Smartly") }
        TextButton(onClick = { s.select(sub, "all") }) { Text("All") }
        TextButton(onClick = { s.select(sub, "none") }) { Text("None") }
    }
    LazyColumn(Modifier.padding(horizontal = 12.dp)) {
        items(s.sorted(sub.items), key = { it.id }) { item ->
            var open by remember(item.id) { mutableStateOf(false) }
            ItemRow(s, item, 0, open, if (item.children.isEmpty()) null else { { open = !open } })
            if (open) s.sorted(item.children).forEach { ItemRow(s, it, 1, false, null) }
        }
    }
}

@Composable
private fun ItemRow(s: CleanState, item: CleanItem, depth: Int, open: Boolean, onExpand: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(start = (depth * 32).dp).clickable { s.toggle(item) }, verticalAlignment = Alignment.CenterVertically) {
        TriStateCheckbox(
            when (s.checkState(item)) { true -> androidx.compose.ui.state.ToggleableState.On; false -> androidx.compose.ui.state.ToggleableState.Off; null -> androidx.compose.ui.state.ToggleableState.Indeterminate },
            { s.toggle(item) },
        )
        Column(Modifier.weight(1f)) {
            Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatSize(item.size), color = Muted)
        if (onExpand != null) TextButton(onClick = onExpand) { Text(if (open) "▾" else "▸") } else Spacer(Modifier.width(12.dp))
    }
}
