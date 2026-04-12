package com.bitflaker.lucidsourcekit.utils.export

import android.graphics.Bitmap
import androidx.activity.ComponentActivity

class PdfExporter(val activity: ComponentActivity) {
    private var previews: Array<Bitmap>? = null
    var selectedTemplateIndex: Int = 0
    var templates: MutableList<PdfExportTemplate> = mutableListOf()

    fun addTemplate(template: PdfExportTemplate) {
        templates.add(template)
        previews = null
    }

    fun insertTemplate(index: Int, template: PdfExportTemplate) {
        templates.add(index, template)
        previews = null
    }

    fun remove(template: PdfExportTemplate) {
        templates.remove(template)
        previews = null
    }

    fun removeAt(index: Int) {
        templates.removeAt(index)
        previews = null
    }

    suspend fun generatePDF(config: ExportConfiguration) {
        if (selectedTemplateIndex < 0 || selectedTemplateIndex >= templates.size) throw IndexOutOfBoundsException()
        templates[selectedTemplateIndex].generatePDF(config)
    }

    suspend fun getPreviews(): Array<Bitmap> {
        val bitmaps = previews ?: templates.map { it.generatePreview(activity) }.toTypedArray()
        previews = bitmaps
        return bitmaps
    }
}

