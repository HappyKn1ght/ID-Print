package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.model.CardImageState
import com.example.model.CardSide
import com.example.model.CropRect
import com.example.model.IdPrintUiState
import com.example.model.PrintLayout
import com.example.util.A4PdfGenerator
import com.example.util.BitmapProcessor
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IdPrintViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(IdPrintUiState())
  val uiState: StateFlow<IdPrintUiState> = _uiState.asStateFlow()

  fun onImagePicked(context: Context, side: CardSide, uri: Uri) {
    val appContext = context.applicationContext
    updateSide(side) { it.copy(isLoading = true) }

    viewModelScope.launch {
      val decoded = withContext(Dispatchers.IO) {
        BitmapProcessor.decodeOrientedBitmap(appContext, uri)
      }
      if (decoded != null) {
        val detectedCrop = withContext(Dispatchers.Default) {
          BitmapProcessor.detectCardBounds(decoded)
        }
        updateSide(side) {
          CardImageState(
            bitmap = decoded,
            crop = detectedCrop,
            autoCrop = detectedCrop,
            isManualCropMode = false,
            isLoading = false,
          )
        }
      } else {
        updateSide(side) { it.copy(isLoading = false) }
        _uiState.update {
          it.copy(errorMessage = appContext.getString(R.string.error_load_image))
        }
      }
    }
  }

  fun onToggleManualCrop(side: CardSide, enabled: Boolean) {
    updateSide(side) { it.copy(isManualCropMode = enabled) }
  }

  fun onCropChanged(side: CardSide, crop: CropRect) {
    updateSide(side) { it.copy(crop = crop) }
  }

  fun onRotate(side: CardSide, degrees: Int) {
    val currentBitmap =
      when (side) {
        CardSide.FRONT -> _uiState.value.front.bitmap
        CardSide.BACK -> _uiState.value.back.bitmap
      } ?: return

    viewModelScope.launch {
      val (rotated, detectedCrop) = withContext(Dispatchers.Default) {
        val rot = BitmapProcessor.rotateBitmap90(currentBitmap, degrees)
        val auto = BitmapProcessor.detectCardBounds(rot)
        rot to auto
      }
      updateSide(side) {
        it.copy(bitmap = rotated, crop = detectedCrop, autoCrop = detectedCrop)
      }
    }
  }

  fun onResetCrop(side: CardSide) {
    updateSide(side) { it.copy(crop = it.autoCrop) }
  }

  fun onSwapSides() {
    _uiState.update { state ->
      state.copy(front = state.back, back = state.front)
    }
  }

  fun onLayoutSelected(layout: PrintLayout) {
    _uiState.update { it.copy(layout = layout) }
  }

  fun onClearAll(context: Context) {
    val cacheDir = File(context.applicationContext.cacheDir, "pdfs")
    cacheDir.listFiles()?.forEach { it.delete() }
    _uiState.value = IdPrintUiState(layout = _uiState.value.layout)
  }

  fun onDismissPdfSheet() {
    _uiState.update { it.copy(generatedPdf = null) }
  }

  fun onDismissError() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  fun onGeneratePdf(context: Context) {
    val snapshot = _uiState.value
    if (!snapshot.canGeneratePdf) return

    val appContext = context.applicationContext
    _uiState.update { it.copy(isGeneratingPdf = true, errorMessage = null) }

    viewModelScope.launch {
      try {
        val result =
          withContext(Dispatchers.IO) {
            val croppedList = mutableListOf<Bitmap>()
            try {
              snapshot.front.bitmap?.let { bmp ->
                croppedList.add(BitmapProcessor.cropBitmap(bmp, snapshot.front.crop))
              }
              snapshot.back.bitmap?.let { bmp ->
                croppedList.add(BitmapProcessor.cropBitmap(bmp, snapshot.back.crop))
              }
              A4PdfGenerator.generateAndSave(
                context = appContext,
                croppedImages = croppedList,
                layout = snapshot.layout,
              )
            } finally {
              croppedList.forEach { it.recycle() }
            }
          }
        _uiState.update { it.copy(isGeneratingPdf = false, generatedPdf = result) }
      } catch (_: Exception) {
        _uiState.update {
          it.copy(
            isGeneratingPdf = false,
            errorMessage = appContext.getString(R.string.error_generate_pdf),
          )
        }
      }
    }
  }

  private fun updateSide(side: CardSide, transform: (CardImageState) -> CardImageState) {
    _uiState.update { state ->
      when (side) {
        CardSide.FRONT -> state.copy(front = transform(state.front))
        CardSide.BACK -> state.copy(back = transform(state.back))
      }
    }
  }
}
