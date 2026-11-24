package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.CropRect
import com.example.ui.theme.CropStageBackground
import com.example.ui.theme.StudioCobalt
import com.example.ui.theme.StudioCobaltLight
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class DragMode {
  NW,
  NE,
  SW,
  SE,
  N,
  S,
  W,
  E,
  MOVE,
  NEW,
}

@Composable
fun IdCropCanvas(
  bitmap: Bitmap,
  crop: CropRect,
  onCropChange: (CropRect) -> Unit,
  modifier: Modifier = Modifier,
) {
  val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
  val latestCrop by rememberUpdatedState(crop)
  val latestOnCropChange by rememberUpdatedState(onCropChange)
  val density = LocalDensity.current
  val hitRadiusPx = with(density) { 26.dp.toPx() }
  val minCropPx = with(density) { 28.dp.toPx() }

  var activeMode by remember { mutableStateOf<DragMode?>(null) }

  Canvas(
    modifier =
      modifier
        .background(CropStageBackground)
        .pointerInput(bitmap) {
          while (currentCoroutineContext().isActive) {
            awaitPointerEventScope {
              val down = awaitFirstDown(requireUnconsumed = false)
              val canvasW = size.width.toFloat()
              val canvasH = size.height.toFloat()
              if (canvasW <= 0f || canvasH <= 0f) return@awaitPointerEventScope

            val scale = min(canvasW / bitmap.width.toFloat(), canvasH / bitmap.height.toFloat())
            val drawW = bitmap.width * scale
            val drawH = bitmap.height * scale
            val offsetX = (canvasW - drawW) / 2f
            val offsetY = (canvasH - drawH) / 2f

            fun clampToImage(pt: Offset): Offset =
              Offset(
                x = pt.x.coerceIn(offsetX, offsetX + drawW),
                y = pt.y.coerceIn(offsetY, offsetY + drawH),
              )

            val startPt = clampToImage(down.position)
            val current = latestCrop
            val cLeft = offsetX + current.x * drawW
            val cTop = offsetY + current.y * drawH
            val cW = current.w * drawW
            val cH = current.h * drawH
            val cRight = cLeft + cW
            val cBottom = cTop + cH

            val handles =
              listOf(
                DragMode.NW to Offset(cLeft, cTop),
                DragMode.NE to Offset(cRight, cTop),
                DragMode.SW to Offset(cLeft, cBottom),
                DragMode.SE to Offset(cRight, cBottom),
                DragMode.N to Offset(cLeft + cW / 2f, cTop),
                DragMode.S to Offset(cLeft + cW / 2f, cBottom),
                DragMode.W to Offset(cLeft, cTop + cH / 2f),
                DragMode.E to Offset(cRight, cTop + cH / 2f),
              )

            var bestMode: DragMode? = null
            var bestDist = Float.MAX_VALUE
            for ((mode, pos) in handles) {
              val dist = hypot(startPt.x - pos.x, startPt.y - pos.y)
              if (dist <= hitRadiusPx && dist < bestDist) {
                bestDist = dist
                bestMode = mode
              }
            }

            val mode =
              bestMode
                ?: if (
                  startPt.x in cLeft..cRight && startPt.y in cTop..cBottom
                ) {
                  DragMode.MOVE
                } else {
                  DragMode.NEW
                }

            activeMode = mode
            down.consume()

            var hasMovedEnoughForNew = false

            while (true) {
              val event = awaitPointerEvent()
              val change = event.changes.firstOrNull { it.id == down.id } ?: break
              if (!change.pressed) {
                change.consume()
                break
              }

              val pt = clampToImage(change.position)
              change.consume()

              var newLeft = cLeft
              var newTop = cTop
              var newRight = cRight
              var newBottom = cBottom

              when (mode) {
                DragMode.MOVE -> {
                  val dx = pt.x - startPt.x
                  val dy = pt.y - startPt.y
                  newLeft = (cLeft + dx).coerceIn(offsetX, offsetX + drawW - cW)
                  newTop = (cTop + dy).coerceIn(offsetY, offsetY + drawH - cH)
                  newRight = newLeft + cW
                  newBottom = newTop + cH
                }
                DragMode.NEW -> {
                  if (!hasMovedEnoughForNew) {
                    if (hypot(pt.x - startPt.x, pt.y - startPt.y) < 8f) continue
                    hasMovedEnoughForNew = true
                  }
                  val x1 = min(startPt.x, pt.x)
                  val y1 = min(startPt.y, pt.y)
                  val x2 = max(startPt.x, pt.x)
                  val y2 = max(startPt.y, pt.y)
                  newLeft = x1
                  newTop = y1
                  newRight = max(x1 + minCropPx, x2).coerceAtMost(offsetX + drawW)
                  newBottom = max(y1 + minCropPx, y2).coerceAtMost(offsetY + drawH)
                  if (newRight - newLeft < minCropPx) {
                    newLeft = (newRight - minCropPx).coerceAtLeast(offsetX)
                  }
                  if (newBottom - newTop < minCropPx) {
                    newTop = (newBottom - minCropPx).coerceAtLeast(offsetY)
                  }
                }
                else -> {
                  if (mode == DragMode.NW || mode == DragMode.W || mode == DragMode.SW) {
                    newLeft = pt.x.coerceIn(offsetX, cRight - minCropPx)
                  }
                  if (mode == DragMode.NE || mode == DragMode.E || mode == DragMode.SE) {
                    newRight = pt.x.coerceIn(cLeft + minCropPx, offsetX + drawW)
                  }
                  if (mode == DragMode.NW || mode == DragMode.N || mode == DragMode.NE) {
                    newTop = pt.y.coerceIn(offsetY, cBottom - minCropPx)
                  }
                  if (mode == DragMode.SW || mode == DragMode.S || mode == DragMode.SE) {
                    newBottom = pt.y.coerceIn(cTop + minCropPx, offsetY + drawH)
                  }
                }
              }

              val normX = ((newLeft - offsetX) / drawW).coerceIn(0f, 1f)
              val normY = ((newTop - offsetY) / drawH).coerceIn(0f, 1f)
              val normW = ((newRight - newLeft) / drawW).coerceIn(0.02f, 1f - normX)
              val normH = ((newBottom - newTop) / drawH).coerceIn(0.02f, 1f - normY)

              latestOnCropChange(CropRect(normX, normY, normW, normH))
            }
              activeMode = null
            }
          }
        }
  ) {
    val canvasW = size.width
    val canvasH = size.height
    if (canvasW <= 0f || canvasH <= 0f) return@Canvas

    val scale = min(canvasW / bitmap.width.toFloat(), canvasH / bitmap.height.toFloat())
    val drawW = bitmap.width * scale
    val drawH = bitmap.height * scale
    val offsetX = (canvasW - drawW) / 2f
    val offsetY = (canvasH - drawH) / 2f

    drawImage(
      image = imageBitmap,
      dstOffset = IntOffset(offsetX.roundToInt(), offsetY.roundToInt()),
      dstSize = IntSize(drawW.roundToInt().coerceAtLeast(1), drawH.roundToInt().coerceAtLeast(1)),
    )

    val cropLeft = offsetX + crop.x * drawW
    val cropTop = offsetY + crop.y * drawH
    val cropW = crop.w * drawW
    val cropH = crop.h * drawH
    val cropRight = cropLeft + cropW
    val cropBottom = cropTop + cropH

    // Dimmed overlay outside crop rect
    val scrimPath =
      Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(Rect(offsetX, offsetY, offsetX + drawW, offsetY + drawH))
        addRect(Rect(cropLeft, cropTop, cropRight, cropBottom))
      }
    drawPath(path = scrimPath, color = Color.Black.copy(alpha = 0.56f))

    // Rule-of-thirds grid inside crop
    val gridColor = Color.White.copy(alpha = 0.34f)
    val gridStroke = 1.dp.toPx()
    drawLine(
      color = gridColor,
      start = Offset(cropLeft + cropW / 3f, cropTop),
      end = Offset(cropLeft + cropW / 3f, cropBottom),
      strokeWidth = gridStroke,
    )
    drawLine(
      color = gridColor,
      start = Offset(cropLeft + 2f * cropW / 3f, cropTop),
      end = Offset(cropLeft + 2f * cropW / 3f, cropBottom),
      strokeWidth = gridStroke,
    )
    drawLine(
      color = gridColor,
      start = Offset(cropLeft, cropTop + cropH / 3f),
      end = Offset(cropRight, cropTop + cropH / 3f),
      strokeWidth = gridStroke,
    )
    drawLine(
      color = gridColor,
      start = Offset(cropLeft, cropTop + 2f * cropH / 3f),
      end = Offset(cropRight, cropTop + 2f * cropH / 3f),
      strokeWidth = gridStroke,
    )

    // Crisp outer border
    drawRect(
      color = Color.White,
      topLeft = Offset(cropLeft, cropTop),
      size = Size(cropW, cropH),
      style = Stroke(width = 1.5.dp.toPx()),
    )

    // Corner L-brackets
    val arm = min(18.dp.toPx(), min(cropW, cropH) * 0.35f)
    val bracketStroke = 3.2.dp.toPx()
    val cornerColor = if (activeMode != null) StudioCobaltLight else Color.White

    // Top-left
    drawLine(cornerColor, Offset(cropLeft, cropTop), Offset(cropLeft + arm, cropTop), bracketStroke)
    drawLine(cornerColor, Offset(cropLeft, cropTop), Offset(cropLeft, cropTop + arm), bracketStroke)
    // Top-right
    drawLine(cornerColor, Offset(cropRight - arm, cropTop), Offset(cropRight, cropTop), bracketStroke)
    drawLine(cornerColor, Offset(cropRight, cropTop), Offset(cropRight, cropTop + arm), bracketStroke)
    // Bottom-left
    drawLine(cornerColor, Offset(cropLeft, cropBottom), Offset(cropLeft + arm, cropBottom), bracketStroke)
    drawLine(cornerColor, Offset(cropLeft, cropBottom - arm), Offset(cropLeft, cropBottom), bracketStroke)
    // Bottom-right
    drawLine(cornerColor, Offset(cropRight - arm, cropBottom), Offset(cropRight, cropBottom), bracketStroke)
    drawLine(cornerColor, Offset(cropRight, cropBottom - arm), Offset(cropRight, cropBottom), bracketStroke)

    // Corner circular knobs
    val knobRadius = 5.5.dp.toPx()
    val corners =
      listOf(
        Offset(cropLeft, cropTop),
        Offset(cropRight, cropTop),
        Offset(cropLeft, cropBottom),
        Offset(cropRight, cropBottom),
      )
    corners.forEach { center ->
      drawCircle(color = StudioCobalt, radius = knobRadius, center = center)
      drawCircle(
        color = Color.White,
        radius = knobRadius,
        center = center,
        style = Stroke(width = 1.8.dp.toPx()),
      )
    }

    // Edge midpoint pills
    val pillLong = 16.dp.toPx()
    val pillShort = 4.5.dp.toPx()
    val pillRadius = CornerRadius(pillShort / 2f, pillShort / 2f)
    // North & South
    listOf(
      Offset(cropLeft + cropW / 2f - pillLong / 2f, cropTop - pillShort / 2f),
      Offset(cropLeft + cropW / 2f - pillLong / 2f, cropBottom - pillShort / 2f),
    ).forEach { topLeft ->
      drawRoundRect(Color.White, topLeft, Size(pillLong, pillShort), pillRadius)
    }
    // West & East
    listOf(
      Offset(cropLeft - pillShort / 2f, cropTop + cropH / 2f - pillLong / 2f),
      Offset(cropRight - pillShort / 2f, cropTop + cropH / 2f - pillLong / 2f),
    ).forEach { topLeft ->
      drawRoundRect(Color.White, topLeft, Size(pillShort, pillLong), pillRadius)
    }
  }
}

@Composable
fun FullScreenCropDialog(
  title: String,
  bitmap: Bitmap,
  crop: CropRect,
  onCropChange: (CropRect) -> Unit,
  onRotateLeft: () -> Unit,
  onRotateRight: () -> Unit,
  onReset: () -> Unit,
  onDismiss: () -> Unit,
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = CropStageBackground,
      contentColor = Color.White,
    ) {
      Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.testTag("fullscreen_crop_close"),
            ) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.action_close),
              )
            }
            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium,
              color = Color.White,
            )
          }
          Button(
            onClick = onDismiss,
            modifier = Modifier.testTag("fullscreen_crop_done"),
          ) {
            Icon(
              imageVector = Icons.Filled.Check,
              contentDescription = null,
              modifier = Modifier.padding(end = 6.dp),
            )
            Text(stringResource(R.string.action_done))
          }
        }

        Box(
          modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
        ) {
          IdCropCanvas(
            bitmap = bitmap,
            crop = crop,
            onCropChange = onCropChange,
            modifier = Modifier.fillMaxSize().testTag("fullscreen_crop_canvas"),
          )
        }

        Row(
          modifier =
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(onClick = onRotateLeft) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.RotateLeft,
              contentDescription = stringResource(R.string.action_rotate_left),
              tint = Color.White,
            )
            Text(
              text = "90°",
              color = Color.White,
              modifier = Modifier.padding(start = 6.dp),
            )
          }
          TextButton(onClick = onRotateRight) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.RotateRight,
              contentDescription = stringResource(R.string.action_rotate_right),
              tint = Color.White,
            )
            Text(
              text = "90°",
              color = Color.White,
              modifier = Modifier.padding(start = 6.dp),
            )
          }
          TextButton(onClick = onReset) {
            Icon(
              imageVector = Icons.Filled.RestartAlt,
              contentDescription = stringResource(R.string.action_reset_crop),
              tint = Color.White,
            )
            Text(
              text = stringResource(R.string.action_reset_crop),
              color = Color.White,
              modifier = Modifier.padding(start = 6.dp),
            )
          }
        }
      }
    }
  }
}
