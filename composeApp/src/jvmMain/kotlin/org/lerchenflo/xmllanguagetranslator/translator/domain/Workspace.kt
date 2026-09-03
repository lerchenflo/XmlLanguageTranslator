package org.lerchenflo.xmllanguagetranslator.translator.domain

data class Workspace(
    val id: String,
    val name: String,
    val files: List<ProjectFile> = emptyList(),
    val filePaths: List<String> = emptyList(),
    val descriptions: List<String> = emptyList(),
    val isLoaded: Boolean = false
)
