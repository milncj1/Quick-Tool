package uk.co.milncj.quick.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

class AttachmentStore(private val context: Context) {
    private val draftDir = File(context.cacheDir, "quick-drafts")
    private val permanentDir = File(context.filesDir, "attachments")

    data class CameraDraft(
        val attachment: DraftAttachment,
        val contentUri: Uri
    )

    fun createCameraDraft(): CameraDraft {
        draftDir.mkdirs()
        val displayName = "quick-photo-${System.currentTimeMillis()}.jpg"
        val file = File(draftDir, uniqueFileName(displayName))
        if (!file.createNewFile()) error("Could not create camera file")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return CameraDraft(
            attachment = DraftAttachment(
                path = file.absolutePath,
                mimeType = "image/jpeg",
                displayName = displayName
            ),
            contentUri = uri
        )
    }

    fun importSharedUri(uri: Uri, hintedMimeType: String?): DraftAttachment {
        draftDir.mkdirs()
        val mimeType = hintedMimeType ?: context.contentResolver.getType(uri)
        val originalName = queryDisplayName(uri)
            ?: buildFallbackName(mimeType)
        val safeDisplayName = sanitizeDisplayName(originalName)
        val target = File(draftDir, uniqueFileName(safeDisplayName))

        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to read shared content" }
            target.outputStream().use { output -> input.copyTo(output) }
        }

        return DraftAttachment(
            path = target.absolutePath,
            mimeType = mimeType,
            displayName = safeDisplayName
        )
    }

    fun persist(draft: DraftAttachment): DraftAttachment {
        permanentDir.mkdirs()
        val source = File(draft.path)
        require(source.exists()) { "Attachment draft no longer exists" }

        if (isInside(source, permanentDir)) return draft

        val target = File(permanentDir, uniqueFileName(draft.displayName))
        if (!source.renameTo(target)) {
            source.inputStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            source.delete()
        }

        return draft.copy(path = target.absolutePath)
    }

    fun discardDraft(draft: DraftAttachment?) {
        if (draft == null) return
        val file = File(draft.path)
        if (isInside(file, draftDir)) file.delete()
    }

    fun deletePermanent(path: String?) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        if (isInside(file, permanentDir)) file.delete()
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                } else null
            }
        }.getOrNull()
    }

    private fun buildFallbackName(mimeType: String?): String {
        val extension = mimeType
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?.takeIf { it.isNotBlank() }
            ?.let { ".${it}" }
            ?: ""
        return "shared-${System.currentTimeMillis()}$extension"
    }

    private fun sanitizeDisplayName(name: String): String {
        val cleaned = name
            .replace(Regex("[^A-Za-z0-9._ -]"), "_")
            .trim()
            .take(120)
        return cleaned.ifBlank { "attachment" }
    }

    private fun uniqueFileName(displayName: String): String =
        "${UUID.randomUUID()}-${sanitizeDisplayName(displayName)}"

    private fun isInside(file: File, directory: File): Boolean {
        return runCatching {
            val parent = directory.canonicalFile
            val candidate = file.canonicalFile
            candidate.path == parent.path || candidate.path.startsWith(parent.path + File.separator)
        }.getOrDefault(false)
    }
}
