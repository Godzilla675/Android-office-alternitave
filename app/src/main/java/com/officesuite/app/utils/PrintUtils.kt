package com.officesuite.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.*
import android.print.pdf.PrintedPdfDocument
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Utility class for printing documents.
 * Implements Nice-to-Have Feature #15: Productivity Enhancements (Print Support)
 */
object PrintUtils {

    /**
     * Print a text document
     */
    fun printText(context: Context, text: String, jobName: String = "Document") {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        
        val printAdapter = object : PrintDocumentAdapter() {
            private var pageCount = 0
            private lateinit var pdfDocument: PrintedPdfDocument
            
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                pdfDocument = PrintedPdfDocument(context, newAttributes)
                
                // Calculate page count (rough estimate: 50 lines per page)
                val lines = text.split("\n").size
                pageCount = (lines / 50) + 1
                
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                
                val info = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(pageCount)
                    .build()
                
                callback.onLayoutFinished(info, true)
            }
            
            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                val lines = text.split("\n")
                val linesPerPage = 50
                var lineIndex = 0
                var pageNumber = 0
                
                while (lineIndex < lines.size) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onWriteCancelled()
                        return
                    }
                    
                    val pageInfo = PdfDocument.PageInfo.Builder(612, 792, pageNumber).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    
                    val paint = android.graphics.Paint().apply {
                        color = Color.BLACK
                        textSize = 12f
                    }
                    
                    var y = 40f
                    val endIndex = minOf(lineIndex + linesPerPage, lines.size)
                    
                    for (i in lineIndex until endIndex) {
                        canvas.drawText(lines[i].take(80), 40f, y, paint)
                        y += 14f
                    }
                    
                    pdfDocument.finishPage(page)
                    lineIndex = endIndex
                    pageNumber++
                }
                
                try {
                    destination?.let {
                        pdfDocument.writeTo(FileOutputStream(it.fileDescriptor))
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                } finally {
                    pdfDocument.close()
                }
            }
        }
        
        printManager.print(jobName, printAdapter, null)
    }

    /**
     * Print an HTML document using WebView
     */
    fun printHtml(context: Context, htmlContent: String, jobName: String = "Document") {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                createWebPrintJob(context, view!!, jobName)
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
    }

    private fun createWebPrintJob(context: Context, webView: WebView, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val printAdapter = webView.createPrintDocumentAdapter(jobName)
        printManager.print(jobName, printAdapter, null)
    }

    /**
     * Print a bitmap image
     */
    fun printBitmap(context: Context, bitmap: Bitmap, jobName: String = "Image") {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        
        val printAdapter = object : PrintDocumentAdapter() {
            private lateinit var pdfDocument: PrintedPdfDocument
            
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                pdfDocument = PrintedPdfDocument(context, newAttributes)
                
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                
                val info = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
                    .setPageCount(1)
                    .build()
                
                callback.onLayoutFinished(info, true)
            }
            
            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                    return
                }
                
                val pageInfo = PdfDocument.PageInfo.Builder(612, 792, 0).create()
                val page = pdfDocument.startPage(pageInfo)
                
                // Scale bitmap to fit page
                val scale = minOf(
                    pageInfo.pageWidth.toFloat() / bitmap.width,
                    pageInfo.pageHeight.toFloat() / bitmap.height
                ) * 0.9f // 90% to leave margins
                
                val scaledWidth = bitmap.width * scale
                val scaledHeight = bitmap.height * scale
                val left = (pageInfo.pageWidth - scaledWidth) / 2
                val top = (pageInfo.pageHeight - scaledHeight) / 2
                
                val scaledBitmap = Bitmap.createScaledBitmap(
                    bitmap, 
                    scaledWidth.toInt(), 
                    scaledHeight.toInt(), 
                    true
                )
                
                page.canvas.drawBitmap(scaledBitmap, left, top, null)
                pdfDocument.finishPage(page)
                
                try {
                    destination?.let {
                        pdfDocument.writeTo(FileOutputStream(it.fileDescriptor))
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                } finally {
                    pdfDocument.close()
                    if (scaledBitmap != bitmap) {
                        scaledBitmap.recycle()
                    }
                }
            }
        }
        
        printManager.print(jobName, printAdapter, null)
    }

    /**
     * Print a PDF file directly
     */
    fun printPdfFile(context: Context, file: File, jobName: String = file.name) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        
        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                
                val info = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                
                callback.onLayoutFinished(info, true)
            }
            
            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                    return
                }
                
                try {
                    FileInputStream(file).use { input ->
                        FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }
        
        printManager.print(jobName, printAdapter, null)
    }
}
