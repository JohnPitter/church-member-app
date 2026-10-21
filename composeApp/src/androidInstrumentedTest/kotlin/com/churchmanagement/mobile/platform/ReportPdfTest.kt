package com.churchmanagement.mobile.platform

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportPdfTest {
    @Test
    fun longReportCreatesMultipleReadablePages() {
        val directory = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)!!
        val file = File(directory, "pedagogy-export-test.pdf")
        val lines = listOf("Avaliação qualitativa · Coordenação Pedagógica", "Potencial: expressão, criatividade & convivência") +
            (1..150).map { "Observação $it: " + "Acompanhamento individual e coletivo. ".repeat(6) }
        file.outputStream().use { ReportFiles.pdf(lines, it) }
        PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
            assertTrue(renderer.pageCount > 3)
            renderer.openPage(0).use { page ->
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                File(directory, "pedagogy-export-test.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
        }
    }
}
