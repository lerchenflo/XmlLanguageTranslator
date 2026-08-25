package org.lerchenflo.xmllanguagetranslator

import java.io.File
import java.util.prefs.Preferences
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

object FilePicker {
    private val prefs = Preferences.userNodeForPackage(FilePicker::class.java)
    private const val LAST_DIR_KEY = "last_directory"
    private const val OPEN_FILES_KEY = "open_files"
    private const val OPEN_FILES_SEPARATOR = "\n"

    fun saveOpenFiles(paths: List<String>) {
        prefs.put(OPEN_FILES_KEY, paths.joinToString(OPEN_FILES_SEPARATOR))
    }

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
