package uk.co.milncj.quick.backup

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import uk.co.milncj.quick.data.AttachmentStore
import uk.co.milncj.quick.data.CaptureItem
import uk.co.milncj.quick.data.CaptureStatus
import uk.co.milncj.quick.data.CategoryStore
import uk.co.milncj.quick.data.DraftAttachment
import uk.co.milncj.quick.data.QuickDatabase
import uk.co.milncj.quick.reminder.ReminderPreferences
import uk.co.milncj.quick.reminder.ReminderScheduler
import uk.co.milncj.quick.reminder.ReminderSettings
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class QuickBackupManager(context: Context) {
    private val appContext = context.applicationContext
    private val database = QuickDatabase(appContext)
    private val attachments = AttachmentStore(appContext)
    private val categories = CategoryStore(appContext)

    data class Result(val itemCount: Int, val attachmentCount: Int)

    fun exportTo(uri: Uri): Result {
        val items = database.all()
        val root = JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put("createdAt", System.currentTimeMillis())
            put("categories", JSONArray(categories.load()))
            val reminder = ReminderPreferences(appContext).load()
            put("reminder", JSONObject().apply {
                put("enabled", reminder.enabled)
                put("hour", reminder.hour)
                put("minute", reminder.minute)
                put("oldItemDays", reminder.oldItemDays)
            })
        }

        val itemArray = JSONArray()
        var attachmentCount = 0
        val attachmentEntries = mutableListOf<Pair<String, File>>()
        items.forEach { item ->
            val entryName = item.attachmentPath
                ?.let(::File)
                ?.takeIf { it.exists() && it.isFile }
                ?.let { file ->
                    val safeName = sanitize(item.attachmentName ?: file.name)
                    "attachments/${item.id}-$safeName".also { attachmentEntries += it to file }
                }

            itemArray.put(JSONObject().apply {
                put("id", item.id)
                put("text", item.text)
                putNullable("category", item.category)
                put("status", item.status.name)
                put("createdAt", item.createdAt)
                put("updatedAt", item.updatedAt)
                putNullable("doneAt", item.doneAt)
                putNullable("attachmentMimeType", item.attachmentMimeType)
                putNullable("attachmentName", item.attachmentName)
                putNullable("attachmentEntry", entryName)
            })
        }
        root.put("items", itemArray)

        val output = requireNotNull(appContext.contentResolver.openOutputStream(uri, "w")) {
            "Unable to open backup destination"
        }
        output.use { raw ->
            ZipOutputStream(raw.buffered()).use { zip ->
                attachmentEntries.forEach { (entryName, file) ->
                    zip.putNextEntry(ZipEntry(entryName))
                    file.inputStream().buffered().use { it.copyTo(zip) }
                    zip.closeEntry()
                    attachmentCount++
                }
                zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                zip.write(root.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return Result(items.size, attachmentCount)
    }

    fun restoreFrom(uri: Uri): Result {
        val restoreDir = File(appContext.cacheDir, "quick-restore-${System.currentTimeMillis()}").apply { mkdirs() }
        val extracted = mutableMapOf<String, File>()
        var manifest: String? = null

        try {
            val input = requireNotNull(appContext.contentResolver.openInputStream(uri)) {
                "Unable to open backup"
            }
            input.use { raw ->
                ZipInputStream(raw.buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.isDirectory) {
                            zip.closeEntry()
                            continue
                        }
                        when {
                            entry.name == MANIFEST_ENTRY -> {
                                manifest = zip.readBytes().toString(Charsets.UTF_8)
                            }
                            entry.name.startsWith("attachments/") -> {
                                val target = safeRestoreTarget(restoreDir, entry.name.removePrefix("attachments/"))
                                target.outputStream().buffered().use { zip.copyTo(it) }
                                extracted[entry.name] = target
                            }
                        }
                        zip.closeEntry()
                    }
                }
            }

            val root = JSONObject(requireNotNull(manifest) { "Backup manifest is missing" })
            require(root.optString("format") == FORMAT) { "This is not a Quick backup" }
            require(root.optInt("version") == VERSION) { "Unsupported Quick backup version" }

            val oldItems = database.all()
            val restored = mutableListOf<CaptureItem>()
            val newPermanentPaths = mutableListOf<String>()
            var restoredAttachments = 0

            try {
                val array = root.getJSONArray("items")
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    val entryName = json.nullableString("attachmentEntry")
                    val restoredAttachment = entryName?.let { name ->
                        val tempFile = requireNotNull(extracted[name]) { "Backup attachment is missing: $name" }
                        val draft = DraftAttachment(
                            path = tempFile.absolutePath,
                            mimeType = json.nullableString("attachmentMimeType"),
                            displayName = json.nullableString("attachmentName") ?: tempFile.name
                        )
                        attachments.persist(draft).also {
                            newPermanentPaths += it.path
                            restoredAttachments++
                        }
                    }

                    restored += CaptureItem(
                        id = json.getLong("id"),
                        text = json.optString("text", ""),
                        category = json.nullableString("category"),
                        status = CaptureStatus.valueOf(json.getString("status")),
                        createdAt = json.getLong("createdAt"),
                        updatedAt = json.getLong("updatedAt"),
                        doneAt = json.nullableLong("doneAt"),
                        attachmentPath = restoredAttachment?.path,
                        attachmentMimeType = restoredAttachment?.mimeType,
                        attachmentName = restoredAttachment?.displayName
                    )
                }

                database.replaceAll(restored)
                oldItems.forEach { attachments.deletePermanent(it.attachmentPath) }

                val restoredCategories = buildList {
                    val categoryArray = root.optJSONArray("categories")
                    if (categoryArray != null) {
                        for (index in 0 until categoryArray.length()) {
                            categoryArray.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
                        }
                    }
                }
                if (restoredCategories.isNotEmpty()) categories.save(restoredCategories)

                root.optJSONObject("reminder")?.let { reminderJson ->
                    val settings = ReminderSettings(
                        enabled = reminderJson.optBoolean("enabled", false),
                        hour = reminderJson.optInt("hour", 18).coerceIn(0, 23),
                        minute = reminderJson.optInt("minute", 0).coerceIn(0, 59),
                        oldItemDays = reminderJson.optInt("oldItemDays", 3).takeIf { it in listOf(0, 3, 7) } ?: 3
                    )
                    ReminderPreferences(appContext).save(settings)
                    ReminderScheduler.apply(appContext, settings)
                }

                return Result(restored.size, restoredAttachments)
            } catch (error: Throwable) {
                newPermanentPaths.forEach(attachments::deletePermanent)
                throw error
            }
        } finally {
            restoreDir.deleteRecursively()
        }
    }

    private fun safeRestoreTarget(directory: File, fileName: String): File {
        val target = File(directory, sanitize(fileName)).canonicalFile
        require(target.parentFile == directory.canonicalFile) { "Invalid attachment path" }
        return target
    }

    private fun sanitize(name: String): String = name
        .replace(Regex("[^A-Za-z0-9._ -]"), "_")
        .trim()
        .take(120)
        .ifBlank { "attachment" }

    private fun JSONObject.putNullable(key: String, value: Any?) {
        put(key, value ?: JSONObject.NULL)
    }

    private fun JSONObject.nullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else getString(key)

    private fun JSONObject.nullableLong(key: String): Long? =
        if (!has(key) || isNull(key)) null else getLong(key)

    companion object {
        private const val FORMAT = "quick-backup"
        private const val VERSION = 1
        private const val MANIFEST_ENTRY = "quick-backup.json"
    }
}
