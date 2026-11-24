package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CardImageState
import com.example.model.CropRect
import com.example.model.PrintLayout
import com.example.util.A4PdfGenerator
import kotlin.math.roundToInt

private data class PreviewCardItem(
  val bitmap: Bitmap,
  val crop: CropRect,
)

@Composable
fun A4SheetPreviewCard(
  front: CardImageState,
  back: CardImageState,
  layout: PrintLayout,
  onLayoutChange: (PrintLayout) -> Unit,
  onSwapSides: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val items =
    remember(front, back) {
      buildList {
        front.bitmap?.let { add(PreviewCardItem(it, front.crop)) }
        back.bitmap?.let { add(PreviewCardItem(it, back.crop)) }
      }
    }

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.a4_preview_title),
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = stringResource(R.string.a4_preview_specs),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (front.hasImage && back.hasImage) {
          OutlinedButton(
            onClick = onSwapSides,
            modifier = Modifier.testTag("swap_sides_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.SwapHoriz,
              contentDescription = stringResource(R.string.swap_sides),
              modifier = Modifier.padding(end = 4.dp),
            )
            Text(
              text = "Swap",
              style = MaterialTheme.typography.labelMedium,
            )
          }
        }
      }

      // Segmented layout toggle
      LayoutSegmentedToggle(
        selectedLayout = layout,
        onLayoutChange = onLayoutChange,
      )

      // Live A4 Paper Sheet Preview
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(10.dp),
            )
            .padding(vertical = 14.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
      ) {
        A4PaperCanvas(
          items = items,
          layout = layout,
          modifier =
            Modifier.widthIn(max = 220.dp)
              .fillMaxWidth(0.68f)
              .aspectRatio(A4PdfGenerator.A4_WIDTH_MM / A4PdfGenerator.A4_HEIGHT_MM)
              .shadow(elevation = 4.dp, shape = RoundedCornerShape(3.dp))
              .background(Color.White, RoundedCornerShape(3.dp))
              .border(0.5.dp, Color(0xFFD0D7E2), RoundedCornerShape(3.dp))
              .testTag("a4_live_preview_canvas"),
        )
      }
    }
  }
}

@Composable
fun LayoutSegmentedToggle(
  selectedLayout: PrintLayout,
  onLayoutChange: (PrintLayout) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
        .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    val options =
      listOf(
        PrintLayout.SIDE_BY_SIDE to stringResource(R.string.layout_side_by_side),
        PrintLayout.TOP_BOTTOM to stringResource(R.string.layout_top_bottom),
      )
    options.forEach { (layout, label) ->
      val selected = layout == selectedLayout
      Surface(
        onClick = { onLayoutChange(layout) },
        modifier =
          Modifier.weight(1f)
            .testTag(
              if (layout == PrintLayout.SIDE_BY_SIDE) "layout_side_by_side_btn"
              else "layout_top_bottom_btn"
            ),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shadowElevation = if (selected) 1.dp else 0.dp,
      ) {
        Box(
          modifier = Modifier.padding(vertical = 11.dp, horizontal = 8.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color =
              if (selected) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun A4PaperCanvas(
  items: List<PreviewCardItem>,
  layout: PrintLayout,
  modifier: Modifier = Modifier,
) {
  val imageBitmaps = remember(items) { items.map { it.bitmap.asImageBitmap() } }

  Canvas(modifier = modifier.fillMaxSize()) {
    val pageScale = size.width / A4PdfGenerator.A4_WIDTH_PT.toFloat()

    if (items.isEmpty()) {
      // Subtle empty slot guides so shopkeeper immediately understands A4 orientation
      val guideSizes = listOf(856 to 540, 856 to 540)
      val placements = A4PdfGenerator.calculateA4Placements(guideSizes, layout)
      val placeholderPaint = Color(0xFFE5EAF2)
      val borderPaint = Color(0xFFC5CEDD)
      placements.forEach { p ->
        val topLeft = Offset(p.leftPt * pageScale, p.topPt * pageScale)
        val rectSize = Size(p.widthPt * pageScale, p.heightPt * pageScale)
        drawRect(color = placeholderPaint, topLeft = topLeft, size = rectSize)
        drawRect(
          color = borderPaint,
          topLeft = topLeft,
          size = rectSize,
          style = Stroke(width = 1.dp.toPx()),
        )
      }
      return@Canvas
    }

    val croppedDims =
      items.map { item ->
        val w = (item.crop.w * item.bitmap.width).roundToInt().coerceAtLeast(1)
        val h = (item.crop.h * item.bitmap.height).roundToInt().coerceAtLeast(1)
        w to h
      }

    val placements = A4PdfGenerator.calculateA4Placements(croppedDims, layout)

    placements.forEachIndexed { index, p ->
      val item = items[index]
      val bmp = item.bitmap
      val imgBmp = imageBitmaps[index]

      val sx = (item.crop.x * bmp.width).roundToInt().coerceIn(0, bmp.width - 1)
      val sy = (item.crop.y * bmp.height).roundToInt().coerceIn(0, bmp.height - 1)
      val sw = (item.crop.w * bmp.width).roundToInt().coerceIn(1, bmp.width - sx)
      val sh = (item.crop.h * bmp.height).roundToInt().coerceIn(1, bmp.height - sy)

      val leftPx = (p.leftPt * pageScale).roundToInt()
      val topPx = (p.topPt * pageScale).roundToInt()
      val wPx = (p.widthPt * pageScale).roundToInt().coerceAtLeast(1)
      val hPx = (p.heightPt * pageScale).roundToInt().coerceAtLeast(1)

      drawImage(
        image = imgBmp,
        srcOffset = IntOffset(sx, sy),
        srcSize = IntSize(sw, sh),
        dstOffset = IntOffset(leftPx, topPx),
        dstSize = IntSize(wPx, hPx),
      )
    }
  }
}
