package uk.co.milncj.quick.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.co.milncj.quick.data.CaptureItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
internal fun CategorySelector(
    selected: String?,
    categories: List<String>,
    onSelected: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelected(null) },
            label = { Text("None") }
        )
        categories.forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelected(category) },
                label = { Text(category) }
            )
        }
    }
}

@Composable
internal fun CaptureText(text: String) {
    if (text.isBlank()) return
    val context = LocalContext.current
    val link = remember(text) { extractWebLink(text) }

    if (link == null) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (link.note.isNotBlank()) {
            Text(link.note, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(10.dp))
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openWebLink(context, link.url) },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    text = "🔗 ${link.label}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Open link",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun AttachmentPreview(
    path: String,
    mimeType: String?,
    displayName: String,
    onOpen: (() -> Unit)? = null
) {
    val isImage = mimeType?.startsWith("image/") == true
    if (isImage) {
        val preview by produceState<ImageBitmap?>(initialValue = null, path) {
            value = withContext(Dispatchers.IO) { decodePreview(path) }
        }
        preview?.let { image ->
            val baseModifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(14.dp))
            Image(
                bitmap = image,
                contentDescription = displayName,
                modifier = if (onOpen == null) baseModifier else baseModifier.clickable(onClick = onOpen),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(6.dp))
        }
        Text(
            text = if (onOpen == null) "📷 $displayName" else "📷 $displayName · Tap to view",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        val icon = if (mimeType == "application/pdf") "📄" else "📎"
        Surface(
            modifier = if (onOpen == null) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().clickable(onClick = onOpen),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    text = "$icon $displayName",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (onOpen != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Open file",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
internal fun FullScreenImageDialog(
    item: CaptureItem,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDone: (() -> Unit)? = null
) {
    val path = item.attachmentPath ?: return
    val image by produceState<ImageBitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) { decodePreview(path, 2400) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Back", color = Color.White) }
                    Row {
                        TextButton(onClick = onShare) { Text("Share", color = Color.White) }
                        if (onDone != null) {
                            TextButton(onClick = onDone) { Text("Done", color = Color.White) }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    image?.let {
                        Image(
                            bitmap = it,
                            contentDescription = item.attachmentName ?: "Photo",
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                    } ?: Text("Unable to open photo", color = Color.White)
                }
            }
        }
    }
}

internal fun shareCapture(context: Context, item: CaptureItem) {
    val intent = Intent(Intent.ACTION_SEND)
    if (item.hasAttachment) {
        val file = File(requireNotNull(item.attachmentPath))
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        intent.type = item.attachmentMimeType ?: "application/octet-stream"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (item.text.isNotBlank()) intent.putExtra(Intent.EXTRA_TEXT, item.text)
    } else {
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, item.text)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Share from Quick"))
    } catch (_: ActivityNotFoundException) {
        // No compatible share target installed.
    }
}

internal fun openAttachment(context: Context, item: CaptureItem): Boolean {
    val path = item.attachmentPath ?: return false
    val file = File(path)
    if (!file.exists()) return false
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, item.attachmentMimeType ?: "application/octet-stream")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

internal fun formatRelativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val diff = (now - timestamp).coerceAtLeast(0L)
    val minute = 60_000L
    val hour = 60 * minute

    if (diff < minute) return "Just now"
    if (diff < hour) {
        val minutes = diff / minute
        return if (minutes == 1L) "1 min ago" else "$minutes min ago"
    }

    val itemCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    val nowCalendar = Calendar.getInstance().apply { timeInMillis = now }
    val time = SimpleDateFormat("HH:mm", Locale.UK).format(Date(timestamp))

    if (itemCalendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR) &&
        itemCalendar.get(Calendar.DAY_OF_YEAR) == nowCalendar.get(Calendar.DAY_OF_YEAR)
    ) return "Today, $time"

    val yesterday = (nowCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (itemCalendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
        itemCalendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
    ) return "Yesterday, $time"

    val sixDaysAgo = (nowCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6) }
    if (itemCalendar.after(sixDaysAgo)) {
        return SimpleDateFormat("EEE, HH:mm", Locale.UK).format(Date(timestamp))
    }

    return SimpleDateFormat("d MMM, HH:mm", Locale.UK).format(Date(timestamp))
}

private data class WebLink(val url: String, val label: String, val note: String)

private fun extractWebLink(text: String): WebLink? {
    val match = WEB_URL.find(text) ?: return null
    val url = match.value.trimEnd('.', ',', ';', ':', ')', ']', '}')
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
    if (uri.scheme !in setOf("http", "https")) return null
    val label = uri.host?.removePrefix("www.")?.takeIf { it.isNotBlank() } ?: "link"

    val note = buildString {
        append(text.substring(0, match.range.first))
        val tailStart = match.range.first + url.length
        if (tailStart < text.length) append(text.substring(tailStart))
    }.trim()

    return WebLink(url = url, label = label, note = note)
}

private fun openWebLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        // No browser installed.
    }
}

private val WEB_URL = Regex("https?://\\S+", RegexOption.IGNORE_CASE)

private fun decodePreview(path: String, maxDimension: Int = 1200): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (bounds.outWidth / sampleSize > maxDimension || bounds.outHeight / sampleSize > maxDimension) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory.decodeFile(path, options)?.asImageBitmap()
}
