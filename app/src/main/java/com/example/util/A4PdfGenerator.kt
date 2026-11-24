package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.GeneratedPdfResult
import com.example.model.PrintLayout
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

data class CardPlacement(
  val leftPt: Float,
  val topPt: Float,
  val rightPt: Float,
  val bottomPt: Float,
) {
  val widthPt: Float
    get() = rightPt - leftPt

  val heightPt: Float
    get() = bottomPt - topPt
}

object A4PdfGenerator {

  const val MM_TO_PT = 72f / 25.4f
  const val A4_WIDTH_MM = 210f
  const val A4_HEIGHT_MM = 297f
  const val A4_WIDTH_PT = 595
  const val A4_HEIGHT_PT = 842

  private const val TOP_MARGIN_MM = 22f
  private const val GAP_MM = 12f
  private const val SIDE_SLOT_W_MM = 86f
  private const val SIDE_SLOT_H_MM = 56f
  private const val STACK_SLOT_W_MM = 90f
  private const val STACK_SLOT_H_MM = 58f

  fun calculateA4Placements(
    imageSizes: List<Pair<Int, Int>>,
    layout: PrintLayout,
  ): List<CardPlacement> {
    if (imageSizes.isEmpty()) return emptyList()

    val pageW = A4_WIDTH_MM * MM_TO_PT
    val topY = TOP_MARGIN_MM * MM_TO_PT
    val gapPt = GAP_MM * MM_TO_PT

    val slotW =
      (if (layout == PrintLayout.SIDE_BY_SIDE) SIDE_SLOT_W_MM else STACK_SLOT_W_MM) * MM_TO_PT
    val slotH =
      (if (layout == PrintLayout.SIDE_BY_SIDE) SIDE_SLOT_H_MM else STACK_SLOT_H_MM) * MM_TO_PT

    if (imageSizes.size == 1) {
      val (imgW, imgH) = imageSizes.first()
      val (wPt, hPt) = fitIntoSlot(imgW, imgH, slotW, slotH)
      val xPt = (pageW - wPt) / 2f
      val yPt = topY + (slotH - hPt) / 2f
      return listOf(CardPlacement(xPt, yPt, xPt + wPt, yPt + hPt))
    }

    return if (layout == PrintLayout.SIDE_BY_SIDE) {
      val totalW = slotW * 2f + gapPt
      val startX = (pageW - totalW) / 2f
      imageSizes.take(2).mapIndexed { idx, (imgW, imgH) ->
        val (wPt, hPt) = fitIntoSlot(imgW, imgH, slotW, slotH)
        val slotLeft = startX + idx * (slotW + gapPt)
        val xPt = slotLeft + (slotW - wPt) / 2f
        val yPt = topY + (slotH - hPt) / 2f
        CardPlacement(xPt, yPt, xPt + wPt, yPt + hPt)
      }
    } else {
      imageSizes.take(2).mapIndexed { idx, (imgW, imgH) ->
        val (wPt, hPt) = fitIntoSlot(imgW, imgH, slotW, slotH)
        val slotTop = topY + idx * (slotH + gapPt)
        val xPt = (pageW - wPt) / 2f
        val yPt = slotTop + (slotH - hPt) / 2f
        CardPlacement(xPt, yPt, xPt + wPt, yPt + hPt)
      }
    }
  }

  private fun fitIntoSlot(
    imgW: Int,
    imgH: Int,
    slotW: Float,
    slotH: Float,
  ): Pair<Float, Float> {
    val safeW = imgW.coerceAtLeast(1).toFloat()
    val safeH = imgH.coerceAtLeast(1).toFloat()
    val scale = min(slotW / safeW, slotH / safeH)
    return (safeW * scale) to (safeH * scale)
  }

  fun generateAndSave(
    context: Context,
    croppedImages: List<Bitmap>,
    layout: PrintLayout,
  ): GeneratedPdfResult {
    require(croppedImages.isNotEmpty()) { "At least one cropped image is required" }

    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH_PT, A4_HEIGHT_PT, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    canvas.drawColor(Color.WHITE)

    val placements =
      calculateA4Placements(
        imageSizes = croppedImages.map { it.width to it.height },
        layout = layout,
      )

    val bitmapPaint =
      Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

    placements.forEachIndexed { index, placement ->
      val bitmap = croppedImages[index]
      val dstRect =
        RectF(placement.leftPt, placement.topPt, placement.rightPt, placement.bottomPt)
      canvas.drawBitmap(bitmap, null, dstRect, bitmapPaint)
    }

    pdfDocument.finishPage(page)

    val pdfBytes =
      ByteArrayOutputStream().use { baos ->
        pdfDocument.writeTo(baos)
        pdfDocument.close()
        baos.toByteArray()
      }

    val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    val fileName = "ID-Card-A4-$stamp.pdf"

    // Always write to app cache/pdfs for secure FileProvider Open / Share / Print
    val cacheDir = File(context.cacheDir, "pdfs").apply {
      mkdirs()
      listFiles()?.forEach { it.delete() }
    }
    val cacheFile = File(cacheDir, fileName)
    FileOutputStream(cacheFile).use { it.write(pdfBytes) }
    val providerUri =
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)

    var savedToDownloads = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      savedToDownloads = saveToMediaStoreDownloads(context, fileName, pdfBytes)
    }

    return GeneratedPdfResult(
      fileName = fileName,
      contentUri = providerUri,
      savedToDownloads = savedToDownloads,
    )
  }

  private fun saveToMediaStoreDownloads(
    context: Context,
    fileName: String,
    pdfBytes: ByteArray,
  ): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    return try {
      val resolver = context.contentResolver
      val values =
        ContentValues().apply {
          put(MediaStore.Downloads.DISPLAY_NAME, fileName)
          put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
          put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
          put(MediaStore.Downloads.IS_PENDING, 1)
        }
      val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
      resolver.openOutputStream(uri)?.use { out ->
        out.write(pdfBytes)
        out.flush()
      } ?: return false
      values.clear()
      values.put(MediaStore.Downloads.IS_PENDING, 0)
      resolver.update(uri, values, null, null)
      true
    } catch (_: Exception) {
      false
    }
  }

  fun copyUriToDestination(context: Context, sourceUri: Uri, destUri: Uri): Boolean {
    return try {
      context.contentResolver.openInputStream(sourceUri)?.use { input ->
        context.contentResolver.openOutputStream(destUri)?.use { output ->
          input.copyTo(output)
          output.flush()
        }
      }
      true
    } catch (_: Exception) {
      false
    }
  }

  fun printPdf(context: Context, pdfUri: Uri, jobName: String) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    val adapter =
      object : PrintDocumentAdapter() {
        override fun onLayout(
          oldAttributes: PrintAttributes?,
          newAttributes: PrintAttributes?,
          cancellationSignal: CancellationSignal?,
          callback: LayoutResultCallback,
          extras: Bundle?,
        ) {
          if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
          }
          val info =
            PrintDocumentInfo.Builder(jobName)
              .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
              .setPageCount(1)
              .build()
          callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
          pages: Array<out PageRange>?,
          destination: ParcelFileDescriptor,
          cancellationSignal: CancellationSignal?,
          callback: WriteResultCallback,
        ) {
          if (cancellationSignal?.isCanceled == true) {
            callback.onWriteCancelled()
            return
          }
          try {
            context.contentResolver.openInputStream(pdfUri)?.use { input ->
              FileOutputStream(destination.fileDescriptor).use { output ->
                input.copyTo(output)
              }
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
          } catch (e: Exception) {
            callback.onWriteFailed(e.message)
          }
        }
      }

    val attributes =
      PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        .build()

    printManager.print(jobName, adapter, attributes)
  }
}
