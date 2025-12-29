package org.lerchenflo.xmllanguagetranslator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Comment
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

        // Master Structure Logic
        val masterNodes = remember(files) {
            if (files.isEmpty()) emptyList<XmlNode>() else {
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
                master.toList()
            }
        }

        fun updateMasterNodes(newNodes: List<XmlNode>) {
             if (files.isNotEmpty()) {
                 val newFiles = files.toMutableList()
                 newFiles[0] = newFiles[0].copy(nodes = newNodes)
                 files = newFiles.toList()
             }
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
                        files = newFiles

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
                            Text(
                                text = file.file.name,
                                style = MaterialTheme.typography.titleSmall
                            )
                            OutlinedTextField(
                                value = file.description,
                                onValueChange = { newDesc ->
                                    val newFiles = files.toMutableList()
                                    newFiles[index] = file.copy(description = newDesc)
                                    files = newFiles.toList()
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
                 val filteredNodesSnapshot = remember(showOnlyEmpty) {
                    if (showOnlyEmpty) {
                         masterNodes.filter { node ->
                             if (node is XmlNode.StringEntry) {
                                  files.any { file ->
                                    val entry = file.nodes.filterIsInstance<XmlNode.StringEntry>().find { it.name == node.name }
                                    val value = entry?.value
                                    value == null || value.isEmpty()
                                }
                             } else {
                                 false 
                             }
                        }
                    } else {
                        emptyList()
                    }
                }
                
                val nodesDisplay = if (showOnlyEmpty) filteredNodesSnapshot else masterNodes.filter { it !is XmlNode.Whitespace }

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

                                // Add Comment Above
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
                                ) { Icon(Icons.Default.Comment, "Add Comment Above", modifier = Modifier.size(16.dp)) }
                                
                                // Delete Action
                                IconButton(
                                    onClick = {
                                        val currentIdx = masterNodes.indexOf(node)
                                        if (currentIdx != -1) {
                                            val newNodes = masterNodes.toMutableList()
                                            newNodes.removeAt(currentIdx)
                                            updateMasterNodes(newNodes)
                                        }
                                    },
                                    enabled = !showOnlyEmpty
                                ) { Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(16.dp)) }
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