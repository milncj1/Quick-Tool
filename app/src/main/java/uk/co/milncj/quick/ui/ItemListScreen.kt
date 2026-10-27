package uk.co.milncj.quick.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uk.co.milncj.quick.data.CaptureItem
import uk.co.milncj.quick.data.CaptureStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ItemListScreen(
    modifier: Modifier = Modifier,
    items: List<CaptureItem>,
    categories: List<String>,
    onDone: (CaptureItem) -> Unit,
    onUndoDone: (CaptureItem) -> Unit,
    onEdit: (CaptureItem, String, String?) -> Unit,
    onDelete: (CaptureItem) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var actionsFor by remember { mutableStateOf<CaptureItem?>(null) }
    var editing by remember { mutableStateOf<CaptureItem?>(null) }
    var deleting by remember { mutableStateOf<CaptureItem?>(null) }
    var viewingImage by remember { mutableStateOf<CaptureItem?>(null) }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    fun markDone(item: CaptureItem) {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        onDone(item)
        actionsFor = null
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Marked Done",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onUndoDone(item)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text("Waiting", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            if (items.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Swipe left to mark Done.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("You're clear.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Nothing waiting in Quick.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                markDone(item)
                                true
                            } else false
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text("✓ Done", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    ) {
                        QuickItemCard(
                            item = item,
                            showDoneAction = true,
                            onDone = { markDone(item) },
                            onOpenActions = { actionsFor = item },
                            onOpenImage = { viewingImage = item },
                            onOpenFile = {
                                if (!openAttachment(context, item)) {
                                    scope.launch { snackbarHostState.showSnackbar("No app could open this file.") }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    actionsFor?.let { item ->
        ItemActionSheet(
            item = item,
            onDismiss = { actionsFor = null },
            onEdit = {
                actionsFor = null
                editing = item
            },
            onShare = { shareCapture(context, item) },
            onDone = { markDone(item) },
            onDelete = {
                actionsFor = null
                deleting = item
            }
        )
    }

    editing?.let { item ->
        EditCaptureDialog(
            item = item,
            categories = categories,
            onDismiss = { editing = null },
            onSave = { text, category ->
                onEdit(item, text, category)
                editing = null
            }
        )
    }

    deleting?.let { item ->
        DeleteCaptureDialog(
            onDismiss = { deleting = null },
            onDelete = {
                onDelete(item)
                deleting = null
            }
        )
    }

    viewingImage?.let { item ->
        FullScreenImageDialog(
            item = item,
            onDismiss = { viewingImage = null },
            onShare = { shareCapture(context, item) },
            onDone = {
                viewingImage = null
                markDone(item)
            }
        )
    }
}

@Composable
internal fun QuickItemCard(
    item: CaptureItem,
    showDoneAction: Boolean,
    onDone: () -> Unit,
    onOpenActions: () -> Unit,
    onOpenImage: () -> Unit,
    onOpenFile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenActions),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (item.text.isNotBlank()) CaptureText(item.text)

            if (item.hasAttachment) {
                if (item.text.isNotBlank()) Spacer(Modifier.height(12.dp))
                val isImage = item.attachmentMimeType?.startsWith("image/") == true
                AttachmentPreview(
                    path = requireNotNull(item.attachmentPath),
                    mimeType = item.attachmentMimeType,
                    displayName = item.attachmentName ?: "Attachment",
                    onOpen = if (isImage) onOpenImage else onOpenFile
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val detail = buildString {
                    if (!item.category.isNullOrBlank()) {
                        append(item.category)
                        append(" · ")
                    }
                    append(formatRelativeTime(item.createdAt))
                }
                Text(
                    detail,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (showDoneAction) {
                    TextButton(onClick = onDone) {
                        Text("✓", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ItemActionSheet(
    item: CaptureItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Text(
                item.text.ifBlank { item.attachmentName ?: "Capture" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3
            )
            Spacer(Modifier.height(12.dp))
            if (item.status == CaptureStatus.WAITING) {
                ActionSheetRow("Edit", onEdit)
            }
            ActionSheetRow("Share", onShare)
            if (item.status == CaptureStatus.WAITING) {
                ActionSheetRow("Mark Done", onDone)
            }
            ActionSheetRow("Delete", onDelete, destructive = true)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ActionSheetRow(label: String, onClick: () -> Unit, destructive: Boolean = false) {
    TextButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 4.dp)
    ) {
        Text(
            label,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium,
            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
internal fun EditCaptureDialog(
    item: CaptureItem,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit
) {
    var text by remember(item.id) { mutableStateOf(item.text) }
    var category by remember(item.id) { mutableStateOf(item.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit capture") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(Modifier.height(12.dp))
                CategorySelector(selected = category, categories = categories, onSelected = { category = it })
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank() || item.hasAttachment,
                onClick = { onSave(text, category) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun DeleteCaptureDialog(onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete this capture?") },
        text = { Text("This cannot be undone.") },
        confirmButton = { TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
