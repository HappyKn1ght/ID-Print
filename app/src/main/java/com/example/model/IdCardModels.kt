package com.example.model

import android.graphics.Bitmap
import android.net.Uri
import kotlin.math.roundToInt

enum class CardSide {
  FRONT,
  BACK,
}

enum class PrintLayout {
  SIDE_BY_SIDE,
  TOP_BOTTOM,
}

data class CropRect(
  val x: Float,
  val y: Float,
  val w: Float,
  val h: Float,
) {
  companion object {
    val DEFAULT = CropRect(x = 0.04f, y = 0.04f, w = 0.92f, h = 0.92f)
    val FULL = CropRect(x = 0f, y = 0f, w = 1f, h = 1f)
  }
}

data class CardImageState(
  val bitmap: Bitmap? = null,
  val crop: CropRect = CropRect.DEFAULT,
  val autoCrop: CropRect = CropRect.DEFAULT,
  val isManualCropMode: Boolean = false,
  val isLoading: Boolean = false,
) {
  val hasImage: Boolean
    get() = bitmap != null

  val croppedPixelWidth: Int
    get() = bitmap?.let { (crop.w * it.width).roundToInt().coerceAtLeast(1) } ?: 0

  val croppedPixelHeight: Int
    get() = bitmap?.let { (crop.h * it.height).roundToInt().coerceAtLeast(1) } ?: 0
}

data class GeneratedPdfResult(
  val fileName: String,
  val contentUri: Uri,
  val savedToDownloads: Boolean,
)

data class IdPrintUiState(
  val front: CardImageState = CardImageState(),
  val back: CardImageState = CardImageState(),
  val layout: PrintLayout = PrintLayout.SIDE_BY_SIDE,
  val isGeneratingPdf: Boolean = false,
  val generatedPdf: GeneratedPdfResult? = null,
  val errorMessage: String? = null,
) {
  val canGeneratePdf: Boolean
    get() = (front.hasImage || back.hasImage) && !isGeneratingPdf && !front.isLoading && !back.isLoading

  val hasBothImages: Boolean
    get() = front.hasImage && back.hasImage

  val hasAnyImage: Boolean
    get() = front.hasImage || back.hasImage
}
