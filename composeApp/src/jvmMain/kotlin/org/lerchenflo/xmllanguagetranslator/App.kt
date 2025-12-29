package org.lerchenflo.xmllanguagetranslator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
                file.content.forEach { keys.add(it.name) }
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
                            content = XmlUtils.parseXml(file)
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
                        XmlUtils.saveXml(projectFile.file, projectFile.content)
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
            // We use a Row with horizontal scroll for columns (Files)
            // Inside, a Column or LazyColumn for rows (Keys)
            // But actually, we want the KEYS to be rows.
            // So: LazyColumn (Rows = Keys)
            // Inside each Item: Row (Columns = Files)
            
            val horizontalScrollState = rememberScrollState()
            
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScrollState)
                        .padding(8.dp)
                ) {
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
                                val value = file.content.find { it.name == key }?.value
                                value == null || value.isEmpty()
                            }
                        }
                    } else {
                        emptyList()
                    }
                }

                val keysDisplay = if (showOnlyEmpty) filteredKeysSnapshot else allKeys

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(keysDisplay) { key ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(horizontalScrollState) // Syncing scrolling is hard this way, but simple approach first
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                
                                val existingRes = file.content.find { it.name == key }
                                val value = existingRes?.value ?: ""
                                
                                OutlinedTextField(
                                    value = value,
                                    onValueChange = { newValue ->
                                        // Update logic
                                        val newContent = file.content.toMutableList()
                                        val resIndex = newContent.indexOfFirst { it.name == key }
                                        
                                        if (resIndex != -1) {
                                            // Update existing
                                            if (newValue.isEmpty()) { 
                                                // Optional: remove if empty? Or keep empty string? user said "leave empty".
                                                // If we keep it, it writes empty tag.
                                                newContent[resIndex] = existingRes!!.copy(value = newValue)
                                            } else {
                                                newContent[resIndex] = existingRes!!.copy(value = newValue)
                                            }
                                        } else {
                                            // Create new
                                            newContent.add(StringResource(key, newValue))
                                        }
                                        
                                        val newFiles = files.toMutableList()
                                        newFiles[fileIndex] = file.copy(content = newContent)
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