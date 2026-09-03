package org.lerchenflo.xmllanguagetranslator

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.lerchenflo.xmllanguagetranslator.translator.presentation.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "XmlLanguageTranslator",
    ) {
        App()
    }
}