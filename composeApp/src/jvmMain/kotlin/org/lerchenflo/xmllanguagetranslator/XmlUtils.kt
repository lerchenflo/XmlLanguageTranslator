package org.lerchenflo.xmllanguagetranslator

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object XmlUtils {
    fun parseXml(file: File): List<StringResource> {
        val resources = mutableListOf<StringResource>()
        try {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(file)
            doc.documentElement.normalize()

            val nodeList = doc.getElementsByTagName("string")
            for (i in 0 until nodeList.length) {
                val node = nodeList.item(i)
                if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                    val element = node as Element
                    val name = element.getAttribute("name")
                    val value = element.textContent
                    resources.add(StringResource(name, value))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return resources
    }

    fun saveXml(file: File, resources: List<StringResource>) {
        try {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.newDocument()

            // Root element <resources>
            val rootElement = doc.createElement("resources")
            doc.appendChild(rootElement)

            for (res in resources) {
                // <string name="...">value</string>
                val stringElement = doc.createElement("string")
                stringElement.setAttribute("name", res.name)
                stringElement.textContent = res.value
                rootElement.appendChild(stringElement)
            }

            // Write to file
            val transformerFactory = javax.xml.transform.TransformerFactory.newInstance()
            val transformer = transformerFactory.newTransformer()
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
            val source = javax.xml.transform.dom.DOMSource(doc)
            val result = javax.xml.transform.stream.StreamResult(file)
            transformer.transform(source, result)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
