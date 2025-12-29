package org.lerchenflo.xmllanguagetranslator

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object XmlUtils {
    fun parseXml(file: File): List<XmlNode> {
        val nodes = mutableListOf<XmlNode>()
        try {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(file)
            
            // Note: We do NOT normalize here to preserve original whitespace if possible, 
            // though XML parsers might still normalize some text.
            // Iterating child nodes of root <resources>
            val rootChildren = doc.documentElement.childNodes
            
            for (i in 0 until rootChildren.length) {
                val node = rootChildren.item(i)
                when (node.nodeType) {
                    Node.ELEMENT_NODE -> {
                        val element = node as Element
                        if (element.tagName == "string") {
                             val name = element.getAttribute("name")
                             val value = element.textContent
                             nodes.add(XmlNode.StringEntry(name, value))
                        } else {
                            // preserve other elements? or ignore?
                            // For strings.xml mainly <string> is relevant.
                            // Ignoring others for now or treating as Other if needed, but let's stick to <string>
                             nodes.add(XmlNode.Other(null)) // Placeholder for unknown elements
                        }
                    }
                    Node.COMMENT_NODE -> {
                        nodes.add(XmlNode.Comment(node.textContent))
                    }
                    Node.TEXT_NODE -> {
                        // This captures whitespace between tags
                        nodes.add(XmlNode.Whitespace(node.textContent))
                    }
                    else -> {
                        nodes.add(XmlNode.Other(node.textContent))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return nodes
    }

    fun saveXml(file: File, nodes: List<XmlNode>) {
        try {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.newDocument()

            // Root element <resources>
            val rootElement = doc.createElement("resources")
            doc.appendChild(rootElement)

            for (node in nodes) {
                when (node) {
                    is XmlNode.StringEntry -> {
                        val stringElement = doc.createElement("string")
                        stringElement.setAttribute("name", node.name)
                        stringElement.textContent = node.value
                        rootElement.appendChild(stringElement)
                    }
                    is XmlNode.Comment -> {
                        val commentNode = doc.createComment(node.content)
                        rootElement.appendChild(commentNode)
                    }
                    is XmlNode.Whitespace -> {
                        val textNode = doc.createTextNode(node.content)
                        rootElement.appendChild(textNode)
                    }
                    is XmlNode.Other -> {
                        // limited support for "Other", maybe skip or try to append text
                        if (node.content != null) {
                            // doc.createTextNode(node.content) ? 
                            // It's safer to ignore potentially malformed "Other" in this simple implementation
                        }
                    }
                }
            }

            // Write to file
            val transformerFactory = javax.xml.transform.TransformerFactory.newInstance()
            val transformer = transformerFactory.newTransformer()
            // Turn OFF indent because we are providing our own whitespace
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "no")
            val source = javax.xml.transform.dom.DOMSource(doc)
            val result = javax.xml.transform.stream.StreamResult(file)
            transformer.transform(source, result)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
