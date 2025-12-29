package org.lerchenflo.xmllanguagetranslator

import java.io.File

data class ProjectFile(
    val file: File,
    val description: String = "",
    val content: List<StringResource> = emptyList()
)
