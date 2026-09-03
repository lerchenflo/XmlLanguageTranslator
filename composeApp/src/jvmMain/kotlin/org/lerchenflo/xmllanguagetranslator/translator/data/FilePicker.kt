package org.lerchenflo.xmllanguagetranslator.translator.data

import java.io.File
import java.util.prefs.Preferences
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

object FilePicker {
    // Fixed absolute path rather than Preferences.userNodeForPackage(FilePicker::class.java):
    // that call derives the storage location from this class's Java package, which would silently
    // orphan every user's already-saved preferences (open files, last directory) the moment this
    // class moved from org.lerchenflo.xmllanguagetranslator into the translator/data package.
    internal val prefs: Preferences = Preferences.userRoot().node("org/lerchenflo/xmllanguagetranslator")
    private const val LAST_DIR_KEY = "last_directory"
    private const val OPEN_FILES_KEY = "open_files"
    private const val OPEN_FILES_SEPARATOR = "\n"

    // Kept only for one-time migration into WorkspaceStore's per-workspace layout.
    fun loadOpenFiles(): List<File> {
        val stored = prefs.get(OPEN_FILES_KEY, "")
        if (stored.isEmpty()) return emptyList()
        return stored.split(OPEN_FILES_SEPARATOR)
            .filter { it.isNotBlank() }
            .map(::File)
            .filter { it.isFile }
    }

    fun pickFile(): File? {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Select strings.xml"
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY

        // Optional: Add XML filter
        val filter = FileNameExtensionFilter("XML Files", "xml")
        fileChooser.fileFilter = filter

        // Reopen in the directory we last picked a file from
        val lastDir = prefs.get(LAST_DIR_KEY, null)?.let(::File)?.takeIf { it.isDirectory }
        if (lastDir != null) {
            fileChooser.currentDirectory = lastDir
        }
        fileChooser.selectedFile = File(lastDir ?: fileChooser.currentDirectory, "strings.xml")

        return if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            val selected = fileChooser.selectedFile
            selected.parentFile?.let { prefs.put(LAST_DIR_KEY, it.absolutePath) }
            selected
        } else {
            null
        }
    }
}
