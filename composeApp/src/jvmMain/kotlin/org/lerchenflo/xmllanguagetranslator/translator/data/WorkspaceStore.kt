package org.lerchenflo.xmllanguagetranslator.translator.data

import org.lerchenflo.xmllanguagetranslator.translator.domain.ProjectFile
import org.lerchenflo.xmllanguagetranslator.translator.domain.Workspace
import java.io.File

object WorkspaceStore {
    // Same fixed node as FilePicker - see the comment there for why this isn't
    // Preferences.userNodeForPackage(...).
    private val prefs = FilePicker.prefs
    private const val WORKSPACE_IDS_KEY = "workspace_ids"
    private const val ACTIVE_WORKSPACE_KEY = "active_workspace"
    private const val LEGACY_OPEN_FILES_KEY = "open_files"
    private const val LIST_SEPARATOR = "\n"
    private const val WORKSPACES_NODE = "ws"

    private fun workspaceNode(id: String) = prefs.node("$WORKSPACES_NODE/$id")

    private fun readList(value: String): List<String> =
        value.split(LIST_SEPARATOR).filter { it.isNotBlank() }

    fun load(): Pair<List<Workspace>, String?> {
        val idsRaw = prefs.get(WORKSPACE_IDS_KEY, "")
        if (idsRaw.isEmpty()) {
            return loadLegacy()
        }

        val ids = readList(idsRaw)
        val workspaces = ids.map { id ->
            val node = workspaceNode(id)
            Workspace(
                id = id,
                name = node.get("name", "Workspace"),
                filePaths = readList(node.get("files", "")),
                // Not filtered for blanks: an empty description must stay index-aligned with filePaths.
                descriptions = node.get("descriptions", "").split(LIST_SEPARATOR)
            )
        }

        val activeId = prefs.get(ACTIVE_WORKSPACE_KEY, null)
        return workspaces.ifEmpty { listOf(Workspace(id = newId(), name = "Workspace 1")) } to activeId
    }

    private fun loadLegacy(): Pair<List<Workspace>, String?> {
        val legacyFiles = FilePicker.loadOpenFiles().map { it.absolutePath }
        val id = newId()
        val workspace = Workspace(id = id, name = "Workspace 1", filePaths = legacyFiles)
        prefs.remove(LEGACY_OPEN_FILES_KEY)
        return listOf(workspace) to id
    }

    fun save(workspaces: List<Workspace>, activeId: String?) {
        val ids = workspaces.map { it.id }
        prefs.put(WORKSPACE_IDS_KEY, ids.joinToString(LIST_SEPARATOR))
        prefs.put(ACTIVE_WORKSPACE_KEY, activeId ?: "")

        for (workspace in workspaces) {
            val node = workspaceNode(workspace.id)
            node.put("name", workspace.name)
            val paths = if (workspace.isLoaded) workspace.files.map { it.file.absolutePath } else workspace.filePaths
            val descriptions = if (workspace.isLoaded) workspace.files.map { it.description.replace("\n", " ") } else workspace.descriptions
            node.put("files", paths.joinToString(LIST_SEPARATOR))
            node.put("descriptions", descriptions.joinToString(LIST_SEPARATOR))
        }

        val knownIds = ids.toSet()
        for (childName in prefs.node(WORKSPACES_NODE).childrenNames()) {
            if (childName !in knownIds) {
                prefs.node("$WORKSPACES_NODE/$childName").removeNode()
            }
        }
    }

    private fun newId(): String = java.util.UUID.randomUUID().toString()
}

// Parses a workspace's filePaths/descriptions from disk into files. Skips paths that no
// longer exist. Lives here (not on the domain model) so parsing/XML concerns stay in the data layer.
fun Workspace.parsed(): Workspace {
    val loadedFiles = filePaths.mapIndexedNotNull { index, path ->
        val file = File(path)
        if (!file.isFile) return@mapIndexedNotNull null
        ProjectFile(
            file = file,
            description = descriptions.getOrElse(index) { "" },
            nodes = XmlUtils.parseXml(file)
        )
    }
    return copy(files = loadedFiles, isLoaded = true)
}
