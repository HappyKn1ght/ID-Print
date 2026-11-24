package com.example.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.model.CardSide
import com.example.model.GeneratedPdfResult
import com.example.ui.components.A4SheetPreviewCard
import com.example.ui.components.IdSideCard
import com.example.util.A4PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdPrintScreen(
  viewModel: IdPrintViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var showPrivacyDialog by remember { mutableStateOf(false) }

  val frontPicker =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia(),
      onResult = { uri ->
        if (uri != null) viewModel.onImagePicked(context, CardSide.FRONT, uri)
      },
    )

  val backPicker =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia(),
      onResult = { uri ->
        if (uri != null) viewModel.onImagePicked(context, CardSide.BACK, uri)
      },
    )

  val saveAsLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("application/pdf"),
      onResult = { destUri: Uri? ->
        val srcUri = uiState.generatedPdf?.contentUri ?: return@rememberLauncherForActivityResult
        if (destUri != null) {
          scope.launch {
            val ok =
              withContext(Dispatchers.IO) {
                A4PdfGenerator.copyUriToDestination(context, srcUri, destUri)
              }
            if (ok) {
              snackbarHostState.showSnackbar("PDF saved successfully")
            }
          }
        }
      },
    )

  LaunchedEffect(uiState.errorMessage) {
    val msg = uiState.errorMessage ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(msg)
    viewModel.onDismissError()
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      StudioHeaderBar(
        hasAnyImage = uiState.hasAnyImage,
        onPrivacyClick = { showPrivacyDialog = true },
        onClearAll = { viewModel.onClearAll(context) },
      )
    },
    bottomBar = {
      GeneratePdfBottomBar(
        enabled = uiState.canGeneratePdf,
        isGenerating = uiState.isGeneratingPdf,
        onGenerateClick = { viewModel.onGeneratePdf(context) },
      )
    },
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
    ) {
      val isWide = maxWidth >= 760.dp
      val scrollState = rememberScrollState()

      Column(
        modifier =
          Modifier.fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        if (isWide) {
          Row(
            modifier = Modifier.fillMaxWidth().widthIn(max = 1100.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
          ) {
            IdSideCard(
              side = CardSide.FRONT,
              state = uiState.front,
              onPickImage = {
                frontPicker.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              onToggleManualCrop = { viewModel.onToggleManualCrop(CardSide.FRONT, it) },
              onCropChange = { viewModel.onCropChanged(CardSide.FRONT, it) },
              onRotate = { viewModel.onRotate(CardSide.FRONT, it) },
              onResetCrop = { viewModel.onResetCrop(CardSide.FRONT) },
              modifier = Modifier.weight(1f),
            )
            IdSideCard(
              side = CardSide.BACK,
              state = uiState.back,
              onPickImage = {
                backPicker.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              onToggleManualCrop = { viewModel.onToggleManualCrop(CardSide.BACK, it) },
              onCropChange = { viewModel.onCropChanged(CardSide.BACK, it) },
              onRotate = { viewModel.onRotate(CardSide.BACK, it) },
              onResetCrop = { viewModel.onResetCrop(CardSide.BACK) },
              modifier = Modifier.weight(1f),
            )
          }
          A4SheetPreviewCard(
            front = uiState.front,
            back = uiState.back,
            layout = uiState.layout,
            onLayoutChange = viewModel::onLayoutSelected,
            onSwapSides = viewModel::onSwapSides,
            modifier = Modifier.widthIn(max = 640.dp),
          )
          CreatorFooterCard(
            onContactClick = {
              val email = context.getString(R.string.developer_email)
              val subject = context.getString(R.string.developer_email_subject)
              val mailtoUri = Uri.parse("mailto:$email?subject=${Uri.encode(subject)}")
              val emailIntent =
                Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
                  putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                  putExtra(Intent.EXTRA_SUBJECT, subject)
                }
              try {
                context.startActivity(emailIntent)
              } catch (_: ActivityNotFoundException) {
                scope.launch {
                  snackbarHostState.showSnackbar(context.getString(R.string.no_email_app))
                }
              }
            },
            modifier = Modifier.widthIn(max = 640.dp),
          )
        } else {
          Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            IdSideCard(
              side = CardSide.FRONT,
              state = uiState.front,
              onPickImage = {
                frontPicker.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              onToggleManualCrop = { viewModel.onToggleManualCrop(CardSide.FRONT, it) },
              onCropChange = { viewModel.onCropChanged(CardSide.FRONT, it) },
              onRotate = { viewModel.onRotate(CardSide.FRONT, it) },
              onResetCrop = { viewModel.onResetCrop(CardSide.FRONT) },
            )
            IdSideCard(
              side = CardSide.BACK,
              state = uiState.back,
              onPickImage = {
                backPicker.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              onToggleManualCrop = { viewModel.onToggleManualCrop(CardSide.BACK, it) },
              onCropChange = { viewModel.onCropChanged(CardSide.BACK, it) },
              onRotate = { viewModel.onRotate(CardSide.BACK, it) },
              onResetCrop = { viewModel.onResetCrop(CardSide.BACK) },
            )
            A4SheetPreviewCard(
              front = uiState.front,
              back = uiState.back,
              layout = uiState.layout,
              onLayoutChange = viewModel::onLayoutSelected,
              onSwapSides = viewModel::onSwapSides,
            )
            CreatorFooterCard(
              onContactClick = {
                val email = context.getString(R.string.developer_email)
                val subject = context.getString(R.string.developer_email_subject)
                val mailtoUri = Uri.parse("mailto:$email?subject=${Uri.encode(subject)}")
                val emailIntent =
                  Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                  }
                try {
                  context.startActivity(emailIntent)
                } catch (_: ActivityNotFoundException) {
                  scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.no_email_app))
                  }
                }
              },
            )
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
      }
    }
  }

  val pdfResult = uiState.generatedPdf
  if (pdfResult != null) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = viewModel::onDismissPdfSheet,
      sheetState = sheetState,
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      PdfReadySheetContent(
        result = pdfResult,
        onOpenPdf = {
          val viewIntent =
            Intent(Intent.ACTION_VIEW).apply {
              setDataAndType(pdfResult.contentUri, "application/pdf")
              addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
          try {
            context.startActivity(viewIntent)
          } catch (_: ActivityNotFoundException) {
            scope.launch {
              snackbarHostState.showSnackbar(context.getString(R.string.no_pdf_viewer))
            }
          }
        },
        onPrintPdf = {
          A4PdfGenerator.printPdf(
            context = context,
            pdfUri = pdfResult.contentUri,
            jobName = pdfResult.fileName,
          )
        },
        onSharePdf = {
          val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
              type = "application/pdf"
              putExtra(Intent.EXTRA_STREAM, pdfResult.contentUri)
              addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
          context.startActivity(Intent.createChooser(shareIntent, pdfResult.fileName))
        },
        onSaveAs = { saveAsLauncher.launch(pdfResult.fileName) },
        onDone = viewModel::onDismissPdfSheet,
      )
    }
  }

  if (showPrivacyDialog) {
    AlertDialog(
      onDismissRequest = { showPrivacyDialog = false },
      icon = {
        Icon(
          imageVector = Icons.Filled.Lock,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.secondary,
        )
      },
      title = { Text(stringResource(R.string.privacy_tooltip_title)) },
      text = { Text(stringResource(R.string.privacy_tooltip_body)) },
      confirmButton = {
        TextButton(onClick = { showPrivacyDialog = false }) {
          Text(stringResource(R.string.action_close))
        }
      },
    )
  }
}

@Composable
private fun StudioHeaderBar(
  hasAnyImage: Boolean,
  onPrivacyClick: () -> Unit,
  onClearAll: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
  ) {
    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier =
              Modifier.size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "ID",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onPrimary,
            )
          }
          Column {
            Text(
              text = stringResource(R.string.app_name),
              style = MaterialTheme.typography.titleLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = stringResource(R.string.header_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            onClick = onPrivacyClick,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("privacy_badge_button"),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = stringResource(R.string.privacy_badge),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = "Local",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
          }

          AnimatedVisibility(visible = hasAnyImage) {
            OutlinedButton(
              onClick = onClearAll,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("clear_all_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.DeleteSweep,
                contentDescription = stringResource(R.string.action_clear_all),
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "Clear",
                style = MaterialTheme.typography.labelMedium,
              )
            }
          }
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
  }
}

@Composable
private fun GeneratePdfBottomBar(
  enabled: Boolean,
  isGenerating: Boolean,
  onGenerateClick: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 4.dp,
    shadowElevation = 8.dp,
  ) {
    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
      ) {
        Button(
          onClick = onGenerateClick,
          enabled = enabled,
          modifier =
            Modifier.widthIn(max = 600.dp)
              .fillMaxWidth()
              .height(54.dp)
              .testTag("generate_pdf_button"),
          shape = RoundedCornerShape(12.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
          if (isGenerating) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.4.dp,
              color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = stringResource(R.string.generating_pdf),
              style = MaterialTheme.typography.titleMedium,
            )
          } else {
            Icon(
              imageVector = Icons.Filled.Download,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.generate_download_pdf),
              style = MaterialTheme.typography.titleMedium,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PdfReadySheetContent(
  result: GeneratedPdfResult,
  onOpenPdf: () -> Unit,
  onPrintPdf: () -> Unit,
  onSharePdf: () -> Unit,
  onSaveAs: () -> Unit,
  onDone: () -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier =
          Modifier.size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.CheckCircle,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.size(26.dp),
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.pdf_ready_title),
          style = MaterialTheme.typography.titleLarge,
        )
        Text(
          text =
            if (result.savedToDownloads) {
              stringResource(R.string.pdf_saved_location, result.fileName)
            } else {
              stringResource(R.string.pdf_saved_private)
            },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Button(
        onClick = onOpenPdf,
        modifier = Modifier.weight(1f).height(48.dp).testTag("open_pdf_button"),
        shape = RoundedCornerShape(10.dp),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.OpenInNew,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(R.string.action_open_pdf))
      }

      FilledTonalButton(
        onClick = onPrintPdf,
        modifier = Modifier.weight(1f).height(48.dp).testTag("print_pdf_button"),
        shape = RoundedCornerShape(10.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Print,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(R.string.action_print_pdf))
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      OutlinedButton(
        onClick = onSharePdf,
        modifier = Modifier.weight(1f).height(48.dp).testTag("share_pdf_button"),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      ) {
        Icon(
          imageVector = Icons.Filled.Share,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(R.string.action_share_pdf))
      }

      OutlinedButton(
        onClick = onSaveAs,
        modifier = Modifier.weight(1f).height(48.dp).testTag("save_as_pdf_button"),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      ) {
        Icon(
          imageVector = Icons.Filled.FolderOpen,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(R.string.action_save_as))
      }
    }

    TextButton(
      onClick = onDone,
      modifier = Modifier.align(Alignment.End).testTag("close_pdf_sheet_button"),
    ) {
      Text(stringResource(R.string.action_done))
    }
  }
}

@Composable
private fun CreatorFooterCard(
  onContactClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth().testTag("creator_footer_card"),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
    border =
      BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = stringResource(R.string.creator_footer_text),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        textAlign = TextAlign.Center,
      )
      FilledTonalButton(
        onClick = onContactClick,
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .height(48.dp)
            .testTag("contact_developer_button"),
        shape = RoundedCornerShape(10.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Email,
          contentDescription = stringResource(R.string.action_contact_developer),
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.action_contact_developer),
          style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}
