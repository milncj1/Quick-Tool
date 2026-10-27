package uk.co.milncj.quick.ui

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import uk.co.milncj.quick.QuickViewModel
import uk.co.milncj.quick.data.CaptureItem
import uk.co.milncj.quick.data.CaptureStatus

private enum class StatusFilter { ALL, WAITING, DONE }

@Composable
internal fun ReviewScreen(
    modifier: Modifier = Modifier,
    viewModel: QuickViewModel,
    waiting: List<CaptureItem>,
    done: List<CaptureItem>,
    categories: List<String>,
    snackbarHostState: SnackbarHostState,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf(StatusFilter.ALL) }
    var categoryFilter by remember { mutableStateOf(ALL_CATEGORIES) }
    var actionsFor by remember { mutableStateOf<CaptureItem?>(null) }
    var editing by remember { mutableStateOf<CaptureItem?>(null) }
    var deleting by remember { mutableStateOf<CaptureItem?>(null) }
    var viewingImage by remember { mutableStateOf<CaptureItem?>(null) }

    LaunchedEffect(categories) {
        if (categoryFilter != ALL_CATEGORIES && categoryFilter != NO_CATEGORY && categoryFilter !in categories) {
            categoryFilter = ALL_CATEGORIES
        }
    }

    fun markDone(item: CaptureItem) {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        viewModel.markDone(item)
        actionsFor = null
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Marked Done",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDone(item)
        }
    }

    val combined = remember(waiting, done) { (waiting + done).sortedByDescending { it.createdAt } }
    val filtered = combined.filter { item ->
        val statusMatches = when (statusFilter) {
            StatusFilter.ALL -> true
            StatusFilter.WAITING -> item.status == CaptureStatus.WAITING
            StatusFilter.DONE -> item.status == CaptureStatus.DONE
        }
        val categoryMatches = when (categoryFilter) {
            ALL_CATEGORIES -> true
            NO_CATEGORY -> item.category.isNullOrBlank()
            else -> item.category == categoryFilter
        }
        val textMatches = query.isBlank() ||
            item.text.contains(query, ignoreCase = true) ||
            (item.attachmentName?.contains(query, ignoreCase = true) == true)
        statusMatches && categoryMatches && textMatches
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Review", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = onOpenSettings) {
                    Text("⚙", style = MaterialTheme.typography.headlineSmall)
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search Quick…") },
                shape = MaterialTheme.shapes.large
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(statusFilter == StatusFilter.ALL, { statusFilter = StatusFilter.ALL }, label = { Text("All") })
                FilterChip(statusFilter == StatusFilter.WAITING, { statusFilter = StatusFilter.WAITING }, label = { Text("Waiting") })
                FilterChip(statusFilter == StatusFilter.DONE, { statusFilter = StatusFilter.DONE }, label = { Text("Done") })
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(categoryFilter == ALL_CATEGORIES, { categoryFilter = ALL_CATEGORIES }, label = { Text("All categories") })
                categories.forEach { category ->
                    FilterChip(categoryFilter == category, { categoryFilter = category }, label = { Text(category) })
                }
                FilterChip(categoryFilter == NO_CATEGORY, { categoryFilter = NO_CATEGORY }, label = { Text("None") })
            }
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Nothing found.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        if (query.isNotBlank() || statusFilter != StatusFilter.ALL || categoryFilter != ALL_CATEGORIES) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Try a different search or filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                QuickItemCard(
                    item = item,
                    showDoneAction = item.status == CaptureStatus.WAITING,
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

        item { Spacer(Modifier.height(8.dp)) }
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
                viewModel.update(item, text, category)
                editing = null
            }
        )
    }

    deleting?.let { item ->
        DeleteCaptureDialog(
            onDismiss = { deleting = null },
            onDelete = {
                viewModel.delete(item)
                deleting = null
            }
        )
    }

    viewingImage?.let { item ->
        FullScreenImageDialog(
            item = item,
            onDismiss = { viewingImage = null },
            onShare = { shareCapture(context, item) },
            onDone = if (item.status == CaptureStatus.WAITING) {
                {
                    viewingImage = null
                    markDone(item)
                }
            } else null
        )
    }
}

private const val ALL_CATEGORIES = "__all__"
private const val NO_CATEGORY = "__none__"
