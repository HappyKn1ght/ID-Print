package com.example

import com.example.model.CropRect
import com.example.model.PrintLayout
import com.example.util.A4PdfGenerator
import com.example.util.BitmapProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class A4PdfGeneratorTest {

  @Test
  fun sideBySidePlacementsFitWithinA4AndAlignHorizontally() {
    val placements =
      A4PdfGenerator.calculateA4Placements(
        imageSizes = listOf(856 to 540, 856 to 540),
        layout = PrintLayout.SIDE_BY_SIDE,
      )
    assertEquals(2, placements.size)
    val leftCard = placements[0]
    val rightCard = placements[1]
    assertTrue(leftCard.leftPt > 0f)
    assertTrue(rightCard.leftPt > leftCard.rightPt)
    assertTrue(rightCard.rightPt < A4PdfGenerator.A4_WIDTH_PT)
    assertEquals(leftCard.topPt, rightCard.topPt, 0.01f)
    assertEquals(leftCard.widthPt, rightCard.widthPt, 0.01f)
  }

  @Test
  fun topBottomPlacementsStackVerticallyWithinA4() {
    val placements =
      A4PdfGenerator.calculateA4Placements(
        imageSizes = listOf(856 to 540, 856 to 540),
        layout = PrintLayout.TOP_BOTTOM,
      )
    assertEquals(2, placements.size)
    val topCard = placements[0]
    val bottomCard = placements[1]
    assertTrue(bottomCard.topPt > topCard.bottomPt)
    assertTrue(bottomCard.bottomPt < A4PdfGenerator.A4_HEIGHT_PT)
    assertEquals(topCard.leftPt, bottomCard.leftPt, 0.01f)
  }

  @Test
  fun singleCardPlacementCentersHorizontallyOnA4() {
    val placements =
      A4PdfGenerator.calculateA4Placements(
        imageSizes = listOf(1000 to 630),
        layout = PrintLayout.SIDE_BY_SIDE,
      )
    assertEquals(1, placements.size)
    val card = placements.first()
    val centerPage = (A4PdfGenerator.A4_WIDTH_MM * A4PdfGenerator.MM_TO_PT) / 2f
    val centerCard = (card.leftPt + card.rightPt) / 2f
    assertEquals(centerPage, centerCard, 0.01f)
  }

  @Test
  fun cropRectDefaultsRemainWithinNormalizedBounds() {
    val crop = CropRect.DEFAULT
    assertTrue(crop.x >= 0f && crop.y >= 0f)
    assertTrue(crop.x + crop.w <= 1f)
    assertTrue(crop.y + crop.h <= 1f)
  }

  @Test
  fun autoCropDetectsCenteredCardOnSolidBackground() {
    val width = 100
    val height = 100
    val bgColor = 0xFF202020.toInt() // Dark table
    val cardColor = 0xFFF5F5F5.toInt() // Light ID card
    val pixels = IntArray(width * height) { bgColor }

    // Place ID card in [20..80) x [25..75)
    for (y in 25 until 75) {
      for (x in 20 until 80) {
        pixels[y * width + x] = cardColor
      }
    }

    val detected = BitmapProcessor.detectCardBoundsFromPixels(pixels, width, height)
    assertEquals(0.20f, detected.x, 0.04f)
    assertEquals(0.25f, detected.y, 0.04f)
    assertEquals(0.60f, detected.w, 0.06f)
    assertEquals(0.50f, detected.h, 0.06f)
  }
}
