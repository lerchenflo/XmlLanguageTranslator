package org.lerchenflo.xmllanguagetranslator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.awt.Color
import java.io.File

@Composable
@Preview
fun App() {
    MaterialTheme {
        var files by remember { mutableStateOf(listOf<ProjectFile>()) }
        var showOnlyEmpty by remember { mutableStateOf(false) }

        // Compute all unique keys maintaining order (priority to first files)
        val allKeys = remember(files) {
            val keys = LinkedHashSet<String>()
            files.forEach { file ->
                // Filter extracting only StringEntry nodes
                file.nodes.filterIsInstance<XmlNode.StringEntry>().forEach { keys.add(it.name) }
            }
            keys.toList()
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar: Actions
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = {
                    val file = FilePicker.pickFile()
                    if (file != null) {
                        files = files + ProjectFile(
                            file = file,
                            nodes = XmlUtils.parseXml(file)
                        )
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add File")
                    Spacer(Modifier.width(8.dp))
                    Text("Add File")
                }
                
                Spacer(Modifier.weight(1f))

                Button(onClick = {
                    files.forEach { projectFile ->
                        XmlUtils.saveXml(projectFile.file, projectFile.nodes)
                    }
                }) {
                    Icon(Icons.Default.Save, contentDescription = "Save All")
                    Spacer(Modifier.width(8.dp))
                    Text("Save All")
                }

                Spacer(Modifier.width(8.dp))

                IconToggleButton(
                    checked = showOnlyEmpty,
                    onCheckedChange = { showOnlyEmpty = it }
                ) {
                    Icon(
                        if (showOnlyEmpty) Icons.Default.FilterList else Icons.Default.FilterListOff,
                        contentDescription = "Filter Empty"
                    )
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

                    // Key Column Header
                    Text(
                        text = "Key / File",
                        modifier = Modifier.width(200.dp),
                        style = MaterialTheme.typography.titleMedium
                    )

                    // File Headers
                    files.forEachIndexed { index, file ->
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.width(300.dp)) {
                            Text(
                                text = file.file.name,
                                style = MaterialTheme.typography.titleSmall
                            )
                            OutlinedTextField(
                                value = file.description,
                                onValueChange = { newDesc ->
                                    val newFiles = files.toMutableList()
                                    newFiles[index] = file.copy(description = newDesc)
                                    files = newFiles.toList() // Trigger recomposition
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
                val filteredKeysSnapshot = remember(showOnlyEmpty) {
                    if (showOnlyEmpty) {
                        allKeys.filter { key ->
                            files.any { file ->
                                val entry = file.nodes.filterIsInstance<XmlNode.StringEntry>().find { it.name == key }
                                val value = entry?.value
                                value == null || value.isEmpty()
                            }
                        }
                    } else {
                        emptyList()
                    }
                }

                val keysDisplay = if (showOnlyEmpty) filteredKeysSnapshot else allKeys

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(keysDisplay) { index, key ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(horizontalScrollState) // Syncing scrolling is difficult here without custom layout
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

                            // Key Name
                            Text(
                                text = key,
                                modifier = Modifier.width(200.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Values per file
                            files.forEachIndexed { fileIndex, file ->
                                Spacer(Modifier.width(8.dp))
                                
                                val existingEntry = file.nodes.filterIsInstance<XmlNode.StringEntry>().find { it.name == key }
                                val value = existingEntry?.value ?: ""
                                
                                OutlinedTextField(
                                    value = value,
                                    onValueChange = { newValue ->
                                        // Update logic
                                        val newNodes = file.nodes.toMutableList()
                                        // We need to find the specific index of the NODE
                                        val nodeIndex = newNodes.indexOfFirst { it is XmlNode.StringEntry && it.name == key }
                                        
                                        if (nodeIndex != -1) {
                                            // Update existing
                                            // Preserve the entry but update value
                                            val oldEntry = newNodes[nodeIndex] as XmlNode.StringEntry
                                            newNodes[nodeIndex] = oldEntry.copy(value = newValue)
                                        } else {
                                            // Create new
                                            // Add whitespace for indentation if possible (simple heuristic: 4 spaces)
                                            // And newline before
                                            
                                            // To make it look nice, we try to append before the last generic whitespace (usually closing tag indentation) 
                                            // or just at end.
                                            
                                            // Simple append:
                                            newNodes.add(XmlNode.Whitespace("\n    ")) // Indent
                                            newNodes.add(XmlNode.StringEntry(key, newValue))
                                            // We rely on the parser having a newline at end of file usually, or we add one step later?
                                            // Ideally we find the last </resources> closing and insert before it?
                                            // Our parser returns children of <resources>. So we just append to list.
                                            // But we need a newline after the entry too probably?
                                            // Let's just append Newline + Entry? 
                                            // The list structure is: [Whitespace, Comment, Whitespace, String, Whitespace]
                                            // If we append at end, it might be: [..., Whitespace(closing indent), String] which renders:
                                            // ...
                                            // </resources><string>...</string> -> Invalid if </resources> is implicit outside list.
                                            // XmlUtils.saveXml constructs <resources> around the nodes. 
                                            // So appending to list puts it inside <resources>.
                                            // So [..., String] becomes <resources>...<string>...</string></resources>.
                                            // We just strictly need proper indentation.
                                        }
                                        
                                        val newFiles = files.toMutableList()
                                        newFiles[fileIndex] = file.copy(nodes = newNodes)
                                        files = newFiles.toList()
                                    },
                                    modifier = Modifier
                                        .width(300.dp)
                                        .background(
                                            color = if (value.isEmpty()) androidx.compose.ui.graphics.Color.Yellow else androidx.compose.ui.graphics.Color.Transparent,
                                        ),
                                    placeholder = { Text("Empty") }
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.alpha(0.5f))
                    }
                }
            }
        }
    }
}