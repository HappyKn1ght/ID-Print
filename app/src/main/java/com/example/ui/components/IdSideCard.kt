package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CardImageState
import com.example.model.CardSide
import com.example.model.CropRect
import com.example.ui.theme.CropStageBackground
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun IdSideCard(
  side: CardSide,
  state: CardImageState,
  onPickImage: () -> Unit,
  onToggleManualCrop: (Boolean) -> Unit,
  onCropChange: (CropRect) -> Unit,
  onRotate: (Int) -> Unit,
  onResetCrop: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showFullScreenCrop by remember { mutableStateOf(false) }

  val sideTag = if (side == CardSide.FRONT) "front" else "back"
  val title =
    if (side == CardSide.FRONT) stringResource(R.string.front_card_title)
    else stringResource(R.string.back_card_title)

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Column {
      // Card Header
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier =
              Modifier.size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = if (side == CardSide.FRONT) "1" else "2",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
          )
          StatusBadge(isReady = state.hasImage)
        }

        if (state.hasImage) {
          IconButton(
            onClick = onPickImage,
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .testTag("change_${sideTag}_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.SwapHoriz,
              contentDescription = stringResource(R.string.action_change_image),
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      // Card Body
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        when {
          state.isLoading -> {
            Box(
              modifier = Modifier.fillMaxWidth().height(230.dp),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator()
            }
          }
          state.bitmap != null -> {
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .height(230.dp)
                  .clip(RoundedCornerShape(10.dp)),
            ) {
              if (state.isManualCropMode) {
                IdCropCanvas(
                  bitmap = state.bitmap,
                  crop = state.crop,
                  onCropChange = onCropChange,
                  modifier = Modifier.fillMaxSize().testTag("crop_canvas_$sideTag"),
                )
              } else {
                CroppedCardPreview(
                  bitmap = state.bitmap,
                  crop = state.crop,
                  modifier = Modifier.fillMaxSize().testTag("cropped_preview_$sideTag"),
                )
              }
            }

            if (state.isManualCropMode) {
              // Manual Adjust Toolbar
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  ToolChipButton(
                    icon = Icons.AutoMirrored.Filled.RotateLeft,
                    label = "90°",
                    contentDescription = stringResource(R.string.action_rotate_left),
                    onClick = { onRotate(-90) },
                    testTag = "rotate_left_$sideTag",
                  )
                  ToolChipButton(
                    icon = Icons.AutoMirrored.Filled.RotateRight,
                    label = "90°",
                    contentDescription = stringResource(R.string.action_rotate_right),
                    onClick = { onRotate(90) },
                    testTag = "rotate_right_$sideTag",
                  )
                  ToolChipButton(
                    icon = Icons.Filled.Refresh,
                    label = stringResource(R.string.action_auto_crop),
                    contentDescription = stringResource(R.string.action_auto_crop),
                    onClick = onResetCrop,
                    testTag = "reset_crop_$sideTag",
                  )
                }

                IconButton(
                  onClick = { showFullScreenCrop = true },
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .testTag("expand_crop_$sideTag"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Fullscreen,
                    contentDescription = stringResource(R.string.action_expand_crop),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              Button(
                onClick = { onToggleManualCrop(false) },
                modifier =
                  Modifier.fillMaxWidth()
                    .height(48.dp)
                    .testTag("done_adjusting_$sideTag"),
                shape = RoundedCornerShape(10.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Check,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp).padding(end = 4.dp),
                )
                Text(stringResource(R.string.action_done_adjusting))
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = stringResource(R.string.crop_instruction),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.weight(1f),
                )
                Text(
                  text = "${state.croppedPixelWidth}×${state.croppedPixelHeight}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(start = 8.dp),
                )
              }
            } else {
              // Clean Auto-Cropped Preview Toolbar + Manual Crop / Adjust Button
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.AutoFixHigh,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                  )
                  Text(
                    text = stringResource(R.string.auto_cropped_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                  )
                }
                Text(
                  text = "${state.croppedPixelWidth}×${state.croppedPixelHeight} px",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                OutlinedButton(
                  onClick = { onToggleManualCrop(true) },
                  modifier =
                    Modifier.weight(1f)
                      .height(48.dp)
                      .testTag("manual_crop_$sideTag"),
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                  contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Crop,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.size(6.dp))
                  Text(
                    text = stringResource(R.string.action_manual_crop_adjust),
                    style = MaterialTheme.typography.labelLarge,
                  )
                }

                ToolChipButton(
                  icon = Icons.AutoMirrored.Filled.RotateLeft,
                  label = "90°",
                  contentDescription = stringResource(R.string.action_rotate_left),
                  onClick = { onRotate(-90) },
                  testTag = "rotate_left_$sideTag",
                )
                ToolChipButton(
                  icon = Icons.AutoMirrored.Filled.RotateRight,
                  label = "90°",
                  contentDescription = stringResource(R.string.action_rotate_right),
                  onClick = { onRotate(90) },
                  testTag = "rotate_right_$sideTag",
                )
              }
            }
          }
          else -> {
            UploadDropBox(
              side = side,
              onClick = onPickImage,
              modifier = Modifier.testTag("upload_${sideTag}_button"),
            )
          }
        }
      }
    }
  }

  if (showFullScreenCrop && state.bitmap != null) {
    FullScreenCropDialog(
      title = title,
      bitmap = state.bitmap,
      crop = state.crop,
      onCropChange = onCropChange,
      onRotateLeft = { onRotate(-90) },
      onRotateRight = { onRotate(90) },
      onReset = onResetCrop,
      onDismiss = { showFullScreenCrop = false },
    )
  }
}

@Composable
private fun CroppedCardPreview(
  bitmap: Bitmap,
  crop: CropRect,
  modifier: Modifier = Modifier,
) {
  val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

  Canvas(
    modifier = modifier.background(CropStageBackground),
  ) {
    val canvasW = size.width
    val canvasH = size.height
    if (canvasW <= 0f || canvasH <= 0f) return@Canvas

    val sx = (crop.x * bitmap.width).roundToInt().coerceIn(0, bitmap.width - 1)
    val sy = (crop.y * bitmap.height).roundToInt().coerceIn(0, bitmap.height - 1)
    val sw = (crop.w * bitmap.width).roundToInt().coerceIn(1, bitmap.width - sx)
    val sh = (crop.h * bitmap.height).roundToInt().coerceIn(1, bitmap.height - sy)

    val pad = 12.dp.toPx()
    val availW = (canvasW - pad * 2f).coerceAtLeast(1f)
    val availH = (canvasH - pad * 2f).coerceAtLeast(1f)
    val scale = min(availW / sw.toFloat(), availH / sh.toFloat())

    val drawW = (sw * scale).roundToInt().coerceAtLeast(1)
    val drawH = (sh * scale).roundToInt().coerceAtLeast(1)
    val left = ((canvasW - drawW) / 2f).roundToInt()
    val top = ((canvasH - drawH) / 2f).roundToInt()

    drawImage(
      image = imageBitmap,
      srcOffset = IntOffset(sx, sy),
      srcSize = IntSize(sw, sh),
      dstOffset = IntOffset(left, top),
      dstSize = IntSize(drawW, drawH),
    )
  }
}

@Composable
private fun StatusBadge(isReady: Boolean) {
  val bg =
    if (isReady) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
  val fg =
    if (isReady) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

  Surface(
    shape = CircleShape,
    color = bg,
  ) {
    Text(
      text =
        if (isReady) stringResource(R.string.status_ready)
        else stringResource(R.string.status_not_uploaded),
      style = MaterialTheme.typography.labelMedium,
      color = fg,
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
    )
  }
}

@Composable
private fun ToolChipButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  contentDescription: String,
  onClick: () -> Unit,
  testTag: String,
) {
  Surface(
    onClick = onClick,
    modifier = Modifier.minimumInteractiveComponentSize().testTag(testTag),
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
private fun UploadDropBox(
  side: CardSide,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val borderColor = MaterialTheme.colorScheme.outline
  val primaryColor = MaterialTheme.colorScheme.primary
  val title =
    if (side == CardSide.FRONT) stringResource(R.string.upload_front_title)
    else stringResource(R.string.upload_back_title)

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .height(210.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .drawBehind {
          drawRoundRect(
            color = borderColor,
            style =
              Stroke(
                width = 1.8.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f),
              ),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
          )
        }
        .clickable(onClick = onClick)
        .padding(20.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Box(
        modifier =
          Modifier.size(52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.AddPhotoAlternate,
          contentDescription = title,
          tint = primaryColor,
          modifier = Modifier.size(28.dp),
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = stringResource(R.string.upload_hint_formats),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}
