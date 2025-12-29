package org.lerchenflo.xmllanguagetranslator

sealed class XmlNode {
    data class StringEntry(val name: String, val value: String) : XmlNode()
    data class Comment(val content: String) : XmlNode()
    data class Whitespace(val content: String) : XmlNode()
    data class Other(val content: String?) : XmlNode() // For other unhandled nodes if any
}
