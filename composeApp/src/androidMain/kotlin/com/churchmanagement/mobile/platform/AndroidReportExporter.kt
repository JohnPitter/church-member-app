package com.churchmanagement.mobile.platform

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.churchmanagement.mobile.feature.pedagogy.EvaluationDocument
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidReportExporter(private val context: Context) : ReportExporter {
    override val formats = listOf("PDF", "Word")

    override suspend fun export(document: EvaluationDocument, format: String) {
        require(format in formats)
        val extension = if (format == "PDF") "pdf" else "docx"
        val file = withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, "reports").apply { mkdirs() }
            File(directory, "${document.filename}.$extension").also { output ->
                output.outputStream().use {
                    if (format == "PDF") ReportFiles.pdf(document.lines, it) else ReportFiles.word(document.lines, it)
                }
            }
        }
        withContext(Dispatchers.Main) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.reports", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = if (format == "PDF") "application/pdf" else "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("Relatório pedagógico", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "Salvar ou compartilhar relatório").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}

internal object ReportFiles {
    fun word(lines: List<String>, output: OutputStream) {
        ZipOutputStream(output).use { zip ->
            zip.entry("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>""")
            zip.entry("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>""")
            val paragraphs = lines.flatMap { it.lines() }.joinToString("") {
                "<w:p><w:r><w:t xml:space=\"preserve\">${escapeXml(it)}</w:t></w:r></w:p>"
            }
            zip.entry("word/document.xml", """<?xml version="1.0" encoding="UTF-8"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>$paragraphs<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134"/></w:sectPr></w:body></w:document>""")
        }
    }

    fun pdf(lines: List<String>, output: OutputStream) {
        val pdf = PdfDocument()
        try {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
            var pageNumber = 1
            var page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            page.canvas.drawColor(Color.WHITE)
            var y = 45f
            lines.forEach { paragraph ->
                wrappedLines(paragraph, paint).forEach { line ->
                    if (y > 790f) {
                        pdf.finishPage(page)
                        page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, ++pageNumber).create())
                        page.canvas.drawColor(Color.WHITE)
                        y = 45f
                    }
                    page.canvas.drawText(line, 40f, y, paint)
                    y += 16f
                }
                y += 8f
            }
            pdf.finishPage(page)
            pdf.writeTo(output)
        } finally { pdf.close() }
    }

    private fun wrappedLines(text: String, paint: Paint): List<String> = buildList {
        text.lines().forEach { raw ->
            var remaining = raw
            if (remaining.isEmpty()) add("")
            while (remaining.isNotEmpty()) {
                val count = paint.breakText(remaining, true, 515f, null).coerceAtLeast(1)
                val space = if (count < remaining.length) remaining.lastIndexOf(' ', count) else -1
                val end = if (space > 0) space else count
                add(remaining.take(end))
                remaining = remaining.drop(end).trimStart()
            }
        }
    }

    private fun ZipOutputStream.entry(name: String, value: String) {
        putNextEntry(ZipEntry(name))
        write(value.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun escapeXml(value: String): String = value.filter { it >= ' ' || it == '\t' }
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")
}
