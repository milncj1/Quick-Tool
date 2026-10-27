package uk.co.milncj.quick.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uk.co.milncj.quick.QuickViewModel
import uk.co.milncj.quick.data.AttachmentStore
import uk.co.milncj.quick.data.DraftAttachment

@Composable
internal fun CaptureScreen(
    modifier: Modifier = Modifier,
    viewModel: QuickViewModel,
    initialCategory: String?,
    categories: List<String>,
    waitingCount: Int,
    launchRequest: QuickLaunchRequest?,
    onLaunchRequestConsumed: (Long) -> Unit,
    onOpenWaiting: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var text by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(initialCategory) }
    var attachment by remember { mutableStateOf<DraftAttachment?>(null) }
    var pendingCamera by remember { mutableStateOf<AttachmentStore.CameraDraft?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(categories) {
        if (category != null && category !in categories) category = null
    }

    fun showMessage(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!spoken.isNullOrBlank()) {
                text = if (text.isBlank()) spoken else "$text\n$spoken"
            }
        }
        focusRequester.requestFocus()
        keyboard?.show()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val pending = pendingCamera
        pendingCamera = null
        if (success && pending != null) {
            attachment?.let(viewModel::discardDraft)
            attachment = pending.attachment
        } else {
            viewModel.discardDraft(pending?.attachment)
        }
        focusRequester.requestFocus()
    }

    fun launchVoice() {
        keyboard?.hide()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak into Quick")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            showMessage("Speech recognition isn't available on this phone.")
        }
    }

    fun launchPhoto() {
        keyboard?.hide()
        viewModel.createCameraDraft(
            onCreated = { draft ->
                pendingCamera = draft
                try {
                    cameraLauncher.launch(draft.contentUri)
                } catch (_: ActivityNotFoundException) {
                    pendingCamera = null
                    viewModel.discardDraft(draft.attachment)
                    showMessage("Camera isn't available.")
                }
            },
            onError = { showMessage("Couldn't prepare the camera.") }
        )
    }

    fun saveNow() {
        if (isSaving || (text.isBlank() && attachment == null)) return
        isSaving = true
        viewModel.save(
            text = text,
            category = category,
            attachment = attachment,
            onSaved = {
                text = ""
                attachment = null
                isSaving = false
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                keyboard?.show()
                showMessage("Saved")
            },
            onError = {
                isSaving = false
                showMessage("Couldn't save this capture.")
            }
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    LaunchedEffect(launchRequest?.requestId) {
        val request = launchRequest ?: return@LaunchedEffect

        request.sharedText?.let { text = it }
        request.sharedUri?.let { uri ->
            viewModel.importSharedAttachment(
                uri = uri,
                mimeType = request.sharedMimeType,
                onImported = { imported ->
                    attachment?.let(viewModel::discardDraft)
                    attachment = imported
                },
                onError = { showMessage("Couldn't import the shared file.") }
            )
        }

        when (request.captureAction) {
            QuickCaptureAction.VOICE -> launchVoice()
            QuickCaptureAction.PHOTO -> launchPhoto()
            null -> Unit
        }
        onLaunchRequestConsumed(request.requestId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Quick", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Capture something",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onOpenWaiting) {
                Text(if (waitingCount == 1) "1 waiting" else "$waitingCount waiting")
            }
        }

        Spacer(Modifier.height(22.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .focusRequester(focusRequester),
            placeholder = { Text("What's on your mind?") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { saveNow() }),
            shape = MaterialTheme.shapes.large
        )

        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(modifier = Modifier.weight(1f), onClick = { launchVoice() }) { Text("🎙 Voice") }
            OutlinedButton(modifier = Modifier.weight(1f), onClick = { launchPhoto() }) { Text("📷 Photo") }
        }

        attachment?.let { draft ->
            Spacer(Modifier.height(14.dp))
            AttachmentPreview(draft.path, draft.mimeType, draft.displayName)
            TextButton(onClick = {
                viewModel.discardDraft(draft)
                attachment = null
            }) { Text("Remove attachment") }
        }

        Spacer(Modifier.height(14.dp))
        CategorySelector(selected = category, categories = categories, onSelected = { category = it })
        Spacer(Modifier.height(22.dp))

        Button(
            modifier = Modifier.fillMaxWidth().height(58.dp),
            enabled = !isSaving && (text.isNotBlank() || attachment != null),
            onClick = { saveNow() },
            shape = MaterialTheme.shapes.large
        ) {
            Text(if (isSaving) "SAVING…" else "SAVE", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Category is optional.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
