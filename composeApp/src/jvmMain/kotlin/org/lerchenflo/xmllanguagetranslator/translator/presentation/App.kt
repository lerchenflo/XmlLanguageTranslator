package org.lerchenflo.xmllanguagetranslator.translator.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.lerchenflo.xmllanguagetranslator.sharedui.ButtonTooltip
import org.lerchenflo.xmllanguagetranslator.translator.data.FilePicker
import org.lerchenflo.xmllanguagetranslator.translator.data.WorkspaceStore
import org.lerchenflo.xmllanguagetranslator.translator.data.XmlUtils
import org.lerchenflo.xmllanguagetranslator.translator.data.parsed
import org.lerchenflo.xmllanguagetranslator.translator.domain.ProjectFile
import org.lerchenflo.xmllanguagetranslator.translator.domain.Workspace
import org.lerchenflo.xmllanguagetranslator.translator.domain.XmlNode

private fun buildMasterNodes(files: List<ProjectFile>): List<XmlNode> {
    if (files.isEmpty()) return emptyList()
    val master = files[0].nodes.toMutableList()
    val existingKeys = master.filterIsInstance<XmlNode.StringEntry>().map { it.name }.toSet()

    // Find missing keys from other files
    files.drop(1).forEach { file ->
        file.nodes.filterIsInstance<XmlNode.StringEntry>().forEach { entry ->
            if (entry.name !in existingKeys && master.none { it is XmlNode.StringEntry && it.name == entry.name }) {
                // Append missing key
                master.add(XmlNode.Whitespace("\n    "))
                master.add(entry)
            }
        }
    }
    return master.toList()
}

private fun buildEmptyKeys(files: List<ProjectFile>, master: List<XmlNode>): Set<String> =
    master.filterIsInstance<XmlNode.StringEntry>()
        .filter { node ->
            files.any { file ->
                val entry = file.nodes.filterIsInstance<XmlNode.StringEntry>().find { it.name == node.name }
                val value = entry?.value
                value == null || value.isEmpty()
            }
        }
        .map { it.name }
        .toSet()

private fun matchesSearch(node: XmlNode, files: List<ProjectFile>, query: String): Boolean {
    if (query.isBlank()) return true
    return when (node) {
        is XmlNode.StringEntry -> node.name.contains(query, ignoreCase = true) ||
            files.any { file ->
                file.nodes.filterIsInstance<XmlNode.StringEntry>()
                    .find { it.name == node.name }
                    ?.value?.contains(query, ignoreCase = true) == true
            }
        is XmlNode.Comment -> node.content.contains(query, ignoreCase = true)
        else -> true
    }
}

// Removes the workspace at index, keeping at least one workspace open at all times -
// closing the last remaining one resets it to a fresh empty "Workspace 1" instead.
private fun closeWorkspace(
    index: Int,
    workspaces: List<Workspace>,
    activeIndex: Int,
    setWorkspaces: (List<Workspace>) -> Unit,
    setActiveIndex: (Int) -> Unit
) {
    if (workspaces.size <= 1) {
        setWorkspaces(listOf(Workspace(id = java.util.UUID.randomUUID().toString(), name = "Workspace 1", isLoaded = true)))
        setActiveIndex(0)
        return
    }

    val newWorkspaces = workspaces.filterIndexed { i, _ -> i != index }
    setWorkspaces(newWorkspaces)

    val newActiveIndex = when {
        index < activeIndex -> activeIndex - 1
        index == activeIndex -> (index - 1).coerceAtLeast(0)
        else -> activeIndex
    }
    setActiveIndex(newActiveIndex.coerceIn(0, newWorkspaces.size - 1))
}

// Moves focus from a value field to the same file's field on the next (direction = 1) or
// previous (direction = -1) visible entry row, so Tab/Shift+Tab walk down/up a column
// instead of sideways across files. Silently does nothing if the target row isn't currently
// composed (e.g. scrolled out of the LazyColumn's view) - its FocusRequester isn't attached yet.
private fun focusAdjacentValueField(
    nodesDisplay: List<XmlNode>,
    currentDisplayIndex: Int,
    fileIndex: Int,
    direction: Int,
    focusRequesters: MutableMap<Pair<Int, String>, FocusRequester>
) {
    val stringEntryIndices = nodesDisplay.indices.filter { nodesDisplay[it] is XmlNode.StringEntry }
    val posInStringRows = stringEntryIndices.indexOf(currentDisplayIndex)
    if (posInStringRows == -1) return

    val targetDisplayIndex = stringEntryIndices.getOrNull(posInStringRows + direction) ?: return
    val targetName = (nodesDisplay[targetDisplayIndex] as XmlNode.StringEntry).name
    val requester = focusRequesters.getOrPut(fileIndex to targetName) { FocusRequester() }
    runCatching { requester.requestFocus() }
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        var workspaces by remember { mutableStateOf(listOf<Workspace>()) }
        var activeIndex by remember { mutableIntStateOf(0) }
        var showCreateDialog by remember { mutableStateOf(false) }
        var renameTargetIndex by remember { mutableStateOf<Int?>(null) }
        var closeConfirmIndex by remember { mutableStateOf<Int?>(null) }
        var showClearConfirm by remember { mutableStateOf(false) }
        var showWorkspaceMenu by remember { mutableStateOf(false) }

        val files = workspaces.getOrNull(activeIndex)?.files ?: emptyList()

        fun setFiles(newFiles: List<ProjectFile>) {
            workspaces = workspaces.mapIndexed { i, ws ->
                if (i == activeIndex) ws.copy(files = newFiles, isLoaded = true) else ws
            }
        }

        // Reopen the previous session's workspaces (migrating the old single-file-list format
        // if needed), then keep the persisted layout - names, files, descriptions - in sync.
        LaunchedEffect(Unit) {
            val (loaded, activeId) = WorkspaceStore.load()
            val index = loaded.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
            workspaces = loaded.mapIndexed { i, ws -> if (i == index) ws.parsed() else ws }
            activeIndex = index
        }
        LaunchedEffect(workspaces, activeIndex) {
            if (workspaces.isNotEmpty()) {
                WorkspaceStore.save(workspaces, workspaces.getOrNull(activeIndex)?.id)
            }
        }

        var showOnlyEmpty by remember { mutableStateOf(false) }
        // Keys considered "empty" at the moment the filter was (re-)applied. Kept frozen
        // while the filter stays on, so a row doesn't vanish the instant you fill it in -
        // it only drops out when you toggle the filter off and on again (or is deleted).
        var frozenEmptyKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
        var searchQuery by remember { mutableStateOf("") }

        // Master Structure Logic
        val masterNodes = remember(files) { buildMasterNodes(files) }

        fun updateMasterNodes(newNodes: List<XmlNode>) {
             if (files.isNotEmpty()) {
                 val newFiles = files.toMutableList()
                 newFiles[0] = newFiles[0].copy(nodes = newNodes)
                 setFiles(newFiles.toList())
             }
        }

        fun computeEmptyKeys(): Set<String> = buildEmptyKeys(files, masterNodes)

        fun switchTo(index: Int) {
            val target = workspaces.getOrNull(index) ?: return
            if (!target.isLoaded) {
                workspaces = workspaces.toMutableList().also { it[index] = target.parsed() }
            }
            activeIndex = index
            if (showOnlyEmpty) {
                val switchedFiles = workspaces[index].files
                frozenEmptyKeys = buildEmptyKeys(switchedFiles, buildMasterNodes(switchedFiles))
            }
        }

        // Value-field focus requesters, keyed by (file column, entry key) so Tab/Shift+Tab
        // can jump straight to the same column's next/previous row.
        val valueFieldFocusRequesters = remember { mutableMapOf<Pair<Int, String>, FocusRequester>() }

        if (showCreateDialog) {
            WorkspaceNameDialog(
                title = "New workspace",
                initialName = "",
                onConfirm = { name ->
                    val newWorkspace = Workspace(
                        id = java.util.UUID.randomUUID().toString(),
                        name = name,
                        isLoaded = true
                    )
                    workspaces = workspaces + newWorkspace
                    activeIndex = workspaces.size - 1
                    showCreateDialog = false
                },
                onDismiss = { showCreateDialog = false }
            )
        }

        renameTargetIndex?.let { index ->
            workspaces.getOrNull(index)?.let { workspace ->
                WorkspaceNameDialog(
                    title = "Rename workspace",
                    initialName = workspace.name,
                    onConfirm = { name ->
                        workspaces = workspaces.mapIndexed { i, ws -> if (i == index) ws.copy(name = name) else ws }
                        renameTargetIndex = null
                    },
                    onDismiss = { renameTargetIndex = null }
                )
            }
        }

        closeConfirmIndex?.let { index ->
            workspaces.getOrNull(index)?.let { workspace ->
                AlertDialog(
                    onDismissRequest = { closeConfirmIndex = null },
                    title = { Text("Close ${workspace.name}?") },
                    text = {
                        Text(
                            if (workspace.files.isEmpty()) "This workspace will be closed."
                            else "Unsaved changes in this workspace will be lost."
                        )
                    },
                    confirmButton = {
                        ButtonTooltip("Close this workspace and discard its unsaved changes") {
                            TextButton(onClick = {
                                closeWorkspace(index, workspaces, activeIndex,
                                    setWorkspaces = { workspaces = it },
                                    setActiveIndex = { activeIndex = it })
                                closeConfirmIndex = null
                            }) { Text("Close") }
                        }
                    },
                    dismissButton = {
                        ButtonTooltip("Keep this workspace open") {
                            TextButton(onClick = { closeConfirmIndex = null }) { Text("Cancel") }
                        }
                    }
                )
            }
        }

        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("Clear all files?") },
                text = { Text("Unsaved changes in this workspace will be lost.") },
                confirmButton = {
                    ButtonTooltip("Remove all files from this workspace") {
                        TextButton(onClick = {
                            setFiles(emptyList())
                            frozenEmptyKeys = emptySet()
                            searchQuery = ""
                            showClearConfirm = false
                        }) { Text("Clear") }
                    }
                },
                dismissButton = {
                    ButtonTooltip("Keep the current files") {
                        TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
                    }
                }
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            WorkspaceTabs(
                workspaces = workspaces,
                activeIndex = activeIndex,
                onSelect = { index -> switchTo(index) },
                onCreate = { showCreateDialog = true },
                onClose = { index -> closeConfirmIndex = index },
                onRename = { index -> renameTargetIndex = index }
            )

            HorizontalDivider()

            // Top Bar: Actions
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ButtonTooltip("Reload all files from disk (discards unsaved edits)") {
                    IconButton(onClick = {
                        val newFiles = files.map { it.copy(nodes = XmlUtils.parseXml(it.file)) }
                        setFiles(newFiles)
                        if (showOnlyEmpty) {
                            // Re-sync the frozen filter snapshot against the freshly-reloaded
                            // data, otherwise the visible rows stay pinned to the pre-reload set.
                            frozenEmptyKeys = buildEmptyKeys(newFiles, buildMasterNodes(newFiles))
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload from File")
                    }
                }

                Spacer(Modifier.width(8.dp))

                ButtonTooltip("Add a language file to translate") {
                    Button(onClick = {
                        val file = FilePicker.pickFile()
                        if (file != null) {
                            setFiles(files + ProjectFile(
                                file = file,
                                nodes = XmlUtils.parseXml(file)
                            ))
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add File")
                        Spacer(Modifier.width(8.dp))
                        Text("Add File")
                    }
                }

                Spacer(Modifier.width(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.width(300.dp),
                    singleLine = true,
                    placeholder = { Text("Search keys & values") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            ButtonTooltip("Clear search") {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Search")
                                }
                            }
                        }
                    }
                )

                Spacer(Modifier.weight(1f))

                ButtonTooltip("Sync structure across files and save all to disk") {
                Button(onClick = {
                    if (files.isNotEmpty()) {
                        // 1. Capture the Master Structure (from File 0 / masterNodes)
                        // Note: masterNodes is derived from files[0] + missing keys, and updateMasterNodes updates files[0].
                        // So files[0].nodes is effectively our target structure.
                        val masterStructure = files[0].nodes

                        // 2. Propagate structure to all files
                        val newFiles = files.mapIndexed { index, file ->
                            if (index == 0) {
                                file // File 0 is already the master
                            } else {
                                // Reconstruct nodes for this file based on masterStructure
                                val newNodes = masterStructure.map { masterNode ->
                                    when (masterNode) {
                                        is XmlNode.StringEntry -> {
                                            // Find existin value or missing
                                            val existing = file.nodes.filterIsInstance<XmlNode.StringEntry>()
                                                .find { it.name == masterNode.name }

                                            // Keep existing value, or use empty/master value if it was missing
                                            // (Assuming we want to add the key if it's missing in this file)
                                            existing?.copy() ?: masterNode.copy(value = "")
                                        }
                                        // Copy comments/whitespace/other exactly to enforce structure sync
                                        is XmlNode.Comment -> masterNode.copy()
                                        is XmlNode.Whitespace -> masterNode.copy()
                                        is XmlNode.Other -> masterNode.copy()
                                    }
                                }
                                file.copy(nodes = newNodes)
                            }
                        }

                        // 3. Update State
                        setFiles(newFiles)

                        // 4. Save to Disk
                        newFiles.forEach { projectFile ->
                            XmlUtils.saveXml(projectFile.file, projectFile.nodes)
                        }
                    }
                }) {
                    Icon(Icons.Default.Save, contentDescription = "Save All")
                    Spacer(Modifier.width(8.dp))
                    Text("Save All")
                }
                }

                Spacer(Modifier.width(8.dp))

                ButtonTooltip(
                    if (showOnlyEmpty) "Showing only untranslated entries - click to show all" else "Show only untranslated entries"
                ) {
                    IconToggleButton(
                        checked = showOnlyEmpty,
                        onCheckedChange = { checked ->
                            showOnlyEmpty = checked
                            if (checked) {
                                frozenEmptyKeys = computeEmptyKeys()
                            }
                        }
                    ) {
                        Icon(
                            if (showOnlyEmpty) Icons.Default.FilterList else Icons.Default.FilterListOff,
                            contentDescription = "Filter Empty"
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Box {
                    ButtonTooltip("More workspace actions") {
                        IconButton(onClick = { showWorkspaceMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Actions")
                        }
                    }
                    DropdownMenu(
                        expanded = showWorkspaceMenu,
                        onDismissRequest = { showWorkspaceMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Remove all files from workspace") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.LayersClear,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            enabled = files.isNotEmpty(),
                            onClick = {
                                showWorkspaceMenu = false
                                showClearConfirm = true
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            // Grid Content
            val horizontalScrollState = rememberScrollState()

            Column(modifier = Modifier.fillMaxSize()) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScrollState)
                        .padding(8.dp)
                ) {
                    // Index Header
                    Text(
                        text = "#",
                        modifier = Modifier.width(40.dp),
                        style = MaterialTheme.typography.titleMedium
                    )

                     // Reorder Header
                    Spacer(Modifier.width(180.dp)) // Increased for Up/Down + Comment + Delete

                    // Key/Type Column Header
                    Text(
                        text = "Key / Type",
                        modifier = Modifier.width(200.dp),
                        style = MaterialTheme.typography.titleMedium
                    )

                    // File Headers
                    files.forEachIndexed { index, file ->
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.width(300.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = file.file.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                ButtonTooltip("Remove this file from the project") {
                                    IconButton(
                                        onClick = {
                                            val newFiles = files.filterIndexed { i, _ -> i != index }
                                            setFiles(newFiles)
                                            if (showOnlyEmpty) {
                                                frozenEmptyKeys = buildEmptyKeys(newFiles, buildMasterNodes(newFiles))
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove File",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = file.description,
                                onValueChange = { newDesc ->
                                    val newFiles = files.toMutableList()
                                    newFiles[index] = file.copy(description = newDesc)
                                    setFiles(newFiles.toList())
                                },
                                label = { Text("Description") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Data Rows
                 val filteredNodesSnapshot = if (showOnlyEmpty) {
                        masterNodes.filter { it is XmlNode.StringEntry && it.name in frozenEmptyKeys }
                    } else {
                        emptyList()
                    }

                val nodesDisplay = (if (showOnlyEmpty) filteredNodesSnapshot else masterNodes.filter { it !is XmlNode.Whitespace })
                    .filter { matchesSearch(it, files, searchQuery) }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(nodesDisplay) { index, node ->
                         Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(horizontalScrollState)
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                             // Index
                            Text(
                                text = "${index + 1}",
                                modifier = Modifier.width(40.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Reorder & Action Buttons
                            Row(modifier = Modifier.width(180.dp)) {
                                ButtonTooltip("Move up") {
                                IconButton(
                                    onClick = {
                                        val currentIdx = masterNodes.indexOf(node)
                                        if (currentIdx > 0) {
                                            // Scan backwards skipping whitespace
                                            var targetIdx = currentIdx - 1
                                            while (targetIdx >= 0 && masterNodes[targetIdx] is XmlNode.Whitespace) {
                                                targetIdx--
                                            }

                                            if (targetIdx >= 0) {
                                                // We found a non-whitespace node to swap with
                                                // BUT we need to potentially carry surrounding whitespace with us?
                                                // For simplicity, let's just swap the Node objects in the list
                                                // and not worry about moving their indentation (as parser might have captured newline as separate whitespace)
                                                // Swapping just the nodes is safer for visual reordering.

                                                val newNodes = masterNodes.toMutableList()
                                                val prev = newNodes[targetIdx]
                                                newNodes[targetIdx] = node
                                                newNodes[currentIdx] = prev
                                                updateMasterNodes(newNodes)
                                            }
                                        }
                                    },
                                    enabled = !showOnlyEmpty
                                ) { Icon(Icons.Default.ArrowUpward, "Up", modifier = Modifier.size(16.dp)) }
                                }

                                ButtonTooltip("Move down") {
                                IconButton(
                                    onClick = {
                                         val currentIdx = masterNodes.indexOf(node)
                                        if (currentIdx != -1) {
                                            // Scan users visible next, skipping whitespace
                                            var targetIdx = currentIdx + 1
                                            while (targetIdx < masterNodes.size && masterNodes[targetIdx] is XmlNode.Whitespace) {
                                                targetIdx++
                                            }

                                            if (targetIdx < masterNodes.size) {
                                                val newNodes = masterNodes.toMutableList()
                                                val next = newNodes[targetIdx]
                                                newNodes[targetIdx] = node
                                                newNodes[currentIdx] = next
                                                updateMasterNodes(newNodes)
                                            }
                                        }
                                    },
                                    enabled = !showOnlyEmpty
                                ) { Icon(Icons.Default.ArrowDownward, "Down", modifier = Modifier.size(16.dp)) }
                                }

                                // Add Comment Above
                                ButtonTooltip("Insert comment above this row") {
                                IconButton(
                                    onClick = {
                                        val currentIdx = masterNodes.indexOf(node)
                                        if (currentIdx != -1) {
                                            val newNodes = masterNodes.toMutableList()
                                            // Insert before currentIdx
                                            newNodes.add(currentIdx, XmlNode.Whitespace("\n    "))
                                            newNodes.add(currentIdx + 1, XmlNode.Comment(""))
                                            // We inserted 2 items, so remaining items shift effectively
                                            updateMasterNodes(newNodes)
                                        }
                                    },
                                    enabled = !showOnlyEmpty
                                ) { Icon(Icons.AutoMirrored.Filled.Comment, "Add Comment Above", modifier = Modifier.size(16.dp)) }
                                }

                                // Delete Action
                                ButtonTooltip("Delete this row") {
                                IconButton(
                                    onClick = {
                                        if (node is XmlNode.StringEntry) {
                                            // Remove this key from every file, not just the master
                                            // structure - otherwise it gets re-synced back into the
                                            // master as a "missing key" from the other files.
                                            setFiles(files.map { file ->
                                                file.copy(nodes = file.nodes.filterNot {
                                                    it is XmlNode.StringEntry && it.name == node.name
                                                })
                                            })
                                            frozenEmptyKeys = frozenEmptyKeys - node.name
                                        } else {
                                            val currentIdx = masterNodes.indexOf(node)
                                            if (currentIdx != -1) {
                                                val newNodes = masterNodes.toMutableList()
                                                newNodes.removeAt(currentIdx)
                                                updateMasterNodes(newNodes)
                                            }
                                        }
                                    }
                                ) { Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(16.dp)) }
                                }
                            }

                            when (node) {
                                is XmlNode.StringEntry -> {
                                    // Key Name
                                    Text(
                                        text = node.name,
                                        modifier = Modifier.width(200.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                     // Values per file
                                    files.forEachIndexed { fileIndex, file ->
                                        Spacer(Modifier.width(8.dp))

                                        val existingEntry = file.nodes.filterIsInstance<XmlNode.StringEntry>().find { it.name == node.name }
                                        val value = existingEntry?.value ?: ""
                                        val focusRequester = valueFieldFocusRequesters.getOrPut(fileIndex to node.name) { FocusRequester() }

                                        OutlinedTextField(
                                            value = value,
                                            onValueChange = { newValue ->
                                                val newNodes = file.nodes.toMutableList()
                                                val nodeIndex = newNodes.indexOfFirst { it is XmlNode.StringEntry && it.name == node.name }

                                                if (nodeIndex != -1) {
                                                    val oldEntry = newNodes[nodeIndex] as XmlNode.StringEntry
                                                    newNodes[nodeIndex] = oldEntry.copy(value = newValue)
                                                } else {
                                                    newNodes.add(XmlNode.Whitespace("\n    "))
                                                    newNodes.add(XmlNode.StringEntry(node.name, newValue))
                                                }

                                                val newFiles = files.toMutableList()
                                                newFiles[fileIndex] = file.copy(nodes = newNodes)
                                                setFiles(newFiles.toList())
                                            },
                                            modifier = Modifier
                                                .width(300.dp)
                                                .focusRequester(focusRequester)
                                                .onPreviewKeyEvent { keyEvent ->
                                                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Tab) {
                                                        val direction = if (keyEvent.isShiftPressed) -1 else 1
                                                        focusAdjacentValueField(nodesDisplay, index, fileIndex, direction, valueFieldFocusRequesters)
                                                        true
                                                    } else {
                                                        false
                                                    }
                                                }
                                                .background(
                                                     color = if (value.isEmpty()) androidx.compose.ui.graphics.Color.Yellow else androidx.compose.ui.graphics.Color.Transparent,
                                                ),
                                            placeholder = { Text("Empty") }
                                        )
                                    }
                                }
                                is XmlNode.Comment -> {
                                    // Comment Row
                                    OutlinedTextField(
                                        value = node.content,
                                        onValueChange = { newContent ->
                                            val currentIdx = masterNodes.indexOf(node)
                                            if (currentIdx != -1) {
                                                val newM = masterNodes.toMutableList()
                                                newM[currentIdx] = node.copy(content = newContent)
                                                updateMasterNodes(newM)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                                        label = { Text("Comment") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                                else -> {
                                     Text(
                                        text = "[Unknown Node]",
                                        modifier = Modifier.width(200.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.alpha(0.5f))
                    }

                }
            }
        }
    }
}
