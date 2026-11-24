package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import com.example.model.CropRect
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object BitmapProcessor {

  private const val MAX_SOURCE_DIMENSION = 2400
  private const val MAX_EXPORT_DIMENSION = 2200

  fun decodeOrientedBitmap(
    context: Context,
    uri: Uri,
    maxDimension: Int = MAX_SOURCE_DIMENSION,
  ): Bitmap? {
    val rawBitmap =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
          decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
          val width = info.size.width
          val height = info.size.height
          val longest = max(width, height)
          if (longest > maxDimension && longest > 0) {
            val scale = maxDimension.toFloat() / longest.toFloat()
            decoder.setTargetSize(
              (width * scale).roundToInt().coerceAtLeast(1),
              (height * scale).roundToInt().coerceAtLeast(1),
            )
          }
        }
      } else {
        decodeLegacyOrientedBitmap(context, uri, maxDimension)
      } ?: return null

    return flattenToOpaqueWhite(rawBitmap)
  }

  private fun decodeLegacyOrientedBitmap(
    context: Context,
    uri: Uri,
    maxDimension: Int,
  ): Bitmap? {
    val resolver = context.contentResolver
    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream, null, boundsOpts)
    }
    if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

    var sampleSize = 1
    while (
      boundsOpts.outWidth / sampleSize > maxDimension * 2 ||
        boundsOpts.outHeight / sampleSize > maxDimension * 2
    ) {
      sampleSize *= 2
    }

    val decodeOpts =
      BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
      }
    val decoded =
      resolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOpts)
      } ?: return null

    val orientation =
      resolver.openInputStream(uri)?.use { stream ->
        ExifInterface(stream).getAttributeInt(
          ExifInterface.TAG_ORIENTATION,
          ExifInterface.ORIENTATION_NORMAL,
        )
      } ?: ExifInterface.ORIENTATION_NORMAL

    val matrix = Matrix()
    when (orientation) {
      ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
      ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
      ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
      ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
      ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
      ExifInterface.ORIENTATION_TRANSPOSE -> {
        matrix.postRotate(90f)
        matrix.preScale(-1f, 1f)
      }
      ExifInterface.ORIENTATION_TRANSVERSE -> {
        matrix.postRotate(270f)
        matrix.preScale(-1f, 1f)
      }
    }

    val oriented =
      if (!matrix.isIdentity) {
        val rotated =
          Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated !== decoded) decoded.recycle()
        rotated
      } else {
        decoded
      }

    val longest = max(oriented.width, oriented.height)
    if (longest <= maxDimension) return oriented

    val scale = maxDimension.toFloat() / longest.toFloat()
    val targetW = (oriented.width * scale).roundToInt().coerceAtLeast(1)
    val targetH = (oriented.height * scale).roundToInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(oriented, targetW, targetH, true)
    if (scaled !== oriented) oriented.recycle()
    return scaled
  }

  private fun flattenToOpaqueWhite(source: Bitmap): Bitmap {
    if (!source.hasAlpha() && source.config == Bitmap.Config.ARGB_8888) {
      return source
    }
    val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    canvas.drawColor(Color.WHITE)
    canvas.drawBitmap(source, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
    if (out !== source) source.recycle()
    return out
  }

  fun rotateBitmap90(source: Bitmap, degrees: Int): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
  }

  fun cropBitmap(
    source: Bitmap,
    crop: CropRect,
    maxExportPx: Int = MAX_EXPORT_DIMENSION,
  ): Bitmap {
    val sx = (crop.x * source.width).roundToInt().coerceIn(0, source.width - 1)
    val sy = (crop.y * source.height).roundToInt().coerceIn(0, source.height - 1)
    val sw =
      (crop.w * source.width)
        .roundToInt()
        .coerceIn(1, source.width - sx)
    val sh =
      (crop.h * source.height)
        .roundToInt()
        .coerceIn(1, source.height - sy)

    val longest = max(sw, sh)
    val scale = min(1f, maxExportPx.toFloat() / longest.toFloat())
    val outW = (sw * scale).roundToInt().coerceAtLeast(1)
    val outH = (sh * scale).roundToInt().coerceAtLeast(1)

    val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    canvas.drawColor(Color.WHITE)
    val srcRect = Rect(sx, sy, sx + sw, sy + sh)
    val dstRect = RectF(0f, 0f, outW.toFloat(), outH.toFloat())
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    canvas.drawBitmap(source, srcRect, dstRect, paint)
    return out
  }

  /**
   * Automatically detects the outer edges / background around an ID card and returns
   * a normalized CropRect tightly framing the card.
   */
  fun detectCardBounds(source: Bitmap): CropRect {
    val maxAnalysisDim = 160
    val longest = max(source.width, source.height).coerceAtLeast(1)
    val scale = min(1f, maxAnalysisDim.toFloat() / longest.toFloat())
    val aw = (source.width * scale).roundToInt().coerceAtLeast(8)
    val ah = (source.height * scale).roundToInt().coerceAtLeast(8)

    val analysisBmp =
      if (aw == source.width && ah == source.height) source
      else Bitmap.createScaledBitmap(source, aw, ah, true)

    val pixels = IntArray(aw * ah)
    analysisBmp.getPixels(pixels, 0, aw, 0, 0, aw, ah)
    if (analysisBmp !== source) {
      analysisBmp.recycle()
    }

    return detectCardBoundsFromPixels(pixels, aw, ah)
  }

  internal fun detectCardBoundsFromPixels(
    pixels: IntArray,
    width: Int,
    height: Int,
  ): CropRect {
    if (width < 8 || height < 8 || pixels.size < width * height) {
      return CropRect.DEFAULT
    }

    // 1. Sample outer border (2px perimeter) to estimate background color
    val borderR = mutableListOf<Int>()
    val borderG = mutableListOf<Int>()
    val borderB = mutableListOf<Int>()

    for (y in 0 until height) {
      for (x in 0 until width) {
        if (x < 2 || x >= width - 2 || y < 2 || y >= height - 2) {
          val c = pixels[y * width + x]
          borderR.add((c ushr 16) and 0xFF)
          borderG.add((c ushr 8) and 0xFF)
          borderB.add(c and 0xFF)
        }
      }
    }

    borderR.sort()
    borderG.sort()
    borderB.sort()
    val mid = borderR.size / 2
    val bgR = borderR[mid]
    val bgG = borderG[mid]
    val bgB = borderB[mid]

    // Measure border spread (MAD) so textured desks don't over-trigger
    var spreadSum = 0L
    for (i in borderR.indices) {
      val dr = borderR[i] - bgR
      val dg = borderG[i] - bgG
      val db = borderB[i] - bgB
      spreadSum += kotlin.math.sqrt((dr * dr + dg * dg + db * db).toDouble()).roundToInt()
    }
    val avgBorderSpread = (spreadSum.toFloat() / borderR.size.coerceAtLeast(1))
    val colorThreshold = max(26f, avgBorderSpread * 2.1f + 16f)

    // Helper for luminance 0..255
    fun lum(idx: Int): Int {
      val c = pixels[idx]
      val r = (c ushr 16) and 0xFF
      val g = (c ushr 8) and 0xFF
      val b = c and 0xFF
      return (r * 299 + g * 587 + b * 114) / 1000
    }

    // 2. Build foreground mask combining background color distance + Sobel edge energy
    val fgMask = BooleanArray(width * height)
    for (y in 0 until height) {
      for (x in 0 until width) {
        val idx = y * width + x
        val c = pixels[idx]
        val dr = ((c ushr 16) and 0xFF) - bgR
        val dg = ((c ushr 8) and 0xFF) - bgG
        val db = (c and 0xFF) - bgB
        val dist = kotlin.math.sqrt((dr * dr + dg * dg + db * db).toDouble()).toFloat()

        var edgeEnergy = 0
        if (x in 1 until (width - 1) && y in 1 until (height - 1)) {
          val gx =
            -lum((y - 1) * width + (x - 1)) + lum((y - 1) * width + (x + 1)) -
              2 * lum(y * width + (x - 1)) + 2 * lum(y * width + (x + 1)) -
              lum((y + 1) * width + (x - 1)) + lum((y + 1) * width + (x + 1))
          val gy =
            -lum((y - 1) * width + (x - 1)) - 2 * lum((y - 1) * width + x) -
              lum((y - 1) * width + (x + 1)) +
              lum((y + 1) * width + (x - 1)) + 2 * lum((y + 1) * width + x) +
              lum((y + 1) * width + (x + 1))
          edgeEnergy = kotlin.math.abs(gx) + kotlin.math.abs(gy)
        }

        fgMask[idx] = dist > colorThreshold || edgeEnergy > 140
      }
    }

    // 3. Project onto rows and columns, then smooth with 3-tap box filter
    val rowDensity = FloatArray(height)
    for (y in 0 until height) {
      var count = 0
      val offset = y * width
      for (x in 0 until width) {
        if (fgMask[offset + x]) count++
      }
      rowDensity[y] = count.toFloat() / width.toFloat()
    }

    val colDensity = FloatArray(width)
    for (x in 0 until width) {
      var count = 0
      for (y in 0 until height) {
        if (fgMask[y * width + x]) count++
      }
      colDensity[x] = count.toFloat() / height.toFloat()
    }

    val smoothRows = smooth1D(rowDensity)
    val smoothCols = smooth1D(colDensity)

    // 4. Find bounding span where smoothed card signal exceeds threshold
    val minDensity = 0.22f
    var topIdx = 0
    while (topIdx < height && smoothRows[topIdx] < minDensity) topIdx++

    var bottomIdx = height - 1
    while (bottomIdx > topIdx && smoothRows[bottomIdx] < minDensity) bottomIdx--

    var leftIdx = 0
    while (leftIdx < width && smoothCols[leftIdx] < minDensity) leftIdx++

    var rightIdx = width - 1
    while (rightIdx > leftIdx && smoothCols[rightIdx] < minDensity) rightIdx--

    val normW = (rightIdx - leftIdx + 1).toFloat() / width.toFloat()
    val normH = (bottomIdx - topIdx + 1).toFloat() / height.toFloat()

    // Guardrail: reject tiny noise blobs or overly tiny selections
    if (normW < 0.25f || normH < 0.20f) {
      return CropRect.DEFAULT
    }

    // If the card already fills the frame edge-to-edge, return clean full bounds
    if (leftIdx <= 2 && rightIdx >= width - 3 && topIdx <= 2 && bottomIdx >= height - 3) {
      return CropRect.FULL
    }

    // Add a subtle 0.4% safety margin so rounded corners are preserved without desk bleed
    val padX = 0.004f
    val padY = 0.004f
    val x = ((leftIdx.toFloat() / width.toFloat()) - padX).coerceIn(0f, 0.9f)
    val y = ((topIdx.toFloat() / height.toFloat()) - padY).coerceIn(0f, 0.9f)
    val right = (((rightIdx + 1).toFloat() / width.toFloat()) + padX).coerceIn(x + 0.1f, 1f)
    val bottom = (((bottomIdx + 1).toFloat() / height.toFloat()) + padY).coerceIn(y + 0.1f, 1f)

    return CropRect(
      x = x,
      y = y,
      w = (right - x).coerceIn(0.1f, 1f - x),
      h = (bottom - y).coerceIn(0.1f, 1f - y),
    )
  }

  private fun smooth1D(values: FloatArray): FloatArray {
    if (values.size < 3) return values
    val out = FloatArray(values.size)
    out[0] = (values[0] + values[1]) / 2f
    for (i in 1 until values.size - 1) {
      out[i] = (values[i - 1] + values[i] + values[i + 1]) / 3f
    }
    out[values.lastIndex] = (values[values.lastIndex - 1] + values[values.lastIndex]) / 2f
    return out
  }
}
