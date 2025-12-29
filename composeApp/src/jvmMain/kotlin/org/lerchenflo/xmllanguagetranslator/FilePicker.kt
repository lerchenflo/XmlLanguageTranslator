package org.lerchenflo.xmllanguagetranslator

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

object FilePicker {
    fun pickFile(): File? {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Select strings.xml"
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY

        // Optional: Add XML filter
        val filter = FileNameExtensionFilter("XML Files", "xml")
        fileChooser.fileFilter = filter

        // Optional: Set suggested filename
        fileChooser.selectedFile = File("strings.xml")

        return if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            fileChooser.selectedFile
        } else {
            null
        }
    }
}