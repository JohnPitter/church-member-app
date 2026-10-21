package com.churchmanagement.mobile.platform

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportFilesTest {
    @Test
    fun wordPreservesAccentsNewlinesAndEscapesExternalText() {
        val output = ByteArrayOutputStream()
        ReportFiles.word(listOf("Avaliação de João & Ana <assistidos>", "Potencial\nCriatividade"), output)
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries[entry.name] = zip.readBytes()
                entry = zip.nextEntry
            }
        }
        assertTrue(entries.keys.containsAll(listOf("[Content_Types].xml", "_rels/.rels", "word/document.xml")))
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        entries.values.forEach { factory.newDocumentBuilder().parse(ByteArrayInputStream(it)) }
        val document = factory.newDocumentBuilder().parse(ByteArrayInputStream(entries.getValue("word/document.xml")))
        val paragraphs = document.getElementsByTagNameNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "t")
        assertEquals(3, paragraphs.length)
        assertEquals("Avaliação de João & Ana <assistidos>", paragraphs.item(0).textContent)
        assertEquals("Criatividade", paragraphs.item(2).textContent)
    }
}
