package org.lerchenflo.xmllanguagetranslator.translator.presentation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.lerchenflo.xmllanguagetranslator.sharedui.ButtonTooltip
import org.lerchenflo.xmllanguagetranslator.translator.domain.Workspace

@Composable
fun WorkspaceTabs(
    workspaces: List<Workspace>,
    activeIndex: Int,
    onSelect: (Int) -> Unit,
    onCreate: () -> Unit,
    onClose: (Int) -> Unit,
    onRename: (Int) -> Unit,
) {
    ScrollableTabRow(
        selectedTabIndex = activeIndex,
        edgePadding = 8.dp
    ) {
        workspaces.forEachIndexed { index, workspace ->
            Tab(
                selected = index == activeIndex,
                onClick = { onSelect(index) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(workspace.name, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(4.dp))
                    ButtonTooltip("Rename this workspace") {
                        IconButton(
                            onClick = { onRename(index) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Rename ${workspace.name}",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    ButtonTooltip("Close this workspace") {
                        IconButton(
                            onClick = { onClose(index) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close ${workspace.name}",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        Tab(selected = false, onClick = onCreate) {
            ButtonTooltip("New workspace") {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "New Workspace",
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun WorkspaceNameDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Name") }
            )
        },
        confirmButton = {
            ButtonTooltip("Confirm the workspace name") {
                TextButton(
                    onClick = { onConfirm(name.trim()) },
                    enabled = name.isNotBlank()
                ) {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            ButtonTooltip("Discard and close this dialog") {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
