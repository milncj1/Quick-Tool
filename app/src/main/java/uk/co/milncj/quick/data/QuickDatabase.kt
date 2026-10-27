package uk.co.milncj.quick.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class QuickDatabase(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE captures (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                text TEXT NOT NULL,
                category TEXT,
                status TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                done_at INTEGER,
                attachment_path TEXT,
                attachment_mime_type TEXT,
                attachment_name TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_captures_status_created ON captures(status, created_at DESC)")
        db.execSQL("CREATE INDEX idx_captures_done_at ON captures(done_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE captures ADD COLUMN attachment_path TEXT")
            db.execSQL("ALTER TABLE captures ADD COLUMN attachment_mime_type TEXT")
            db.execSQL("ALTER TABLE captures ADD COLUMN attachment_name TEXT")
        }
    }

    fun insert(
        text: String,
        category: String?,
        attachment: DraftAttachment?,
        now: Long
    ): Long {
        val values = valuesFor(
            id = null,
            text = text,
            category = category,
            status = CaptureStatus.WAITING,
            createdAt = now,
            updatedAt = now,
            doneAt = null,
            attachmentPath = attachment?.path,
            attachmentMimeType = attachment?.mimeType,
            attachmentName = attachment?.displayName
        )
        return writableDatabase.insertOrThrow("captures", null, values)
    }

    fun update(id: Long, text: String, category: String?, now: Long) {
        val values = ContentValues().apply {
            put("text", text.trim())
            if (category == null) putNull("category") else put("category", category)
            put("updated_at", now)
        }
        writableDatabase.update("captures", values, "id = ?", arrayOf(id.toString()))
    }

    fun markDone(id: Long, now: Long) {
        val values = ContentValues().apply {
            put("status", CaptureStatus.DONE.name)
            put("updated_at", now)
            put("done_at", now)
        }
        writableDatabase.update("captures", values, "id = ?", arrayOf(id.toString()))
    }

    fun markWaiting(id: Long, now: Long) {
        val values = ContentValues().apply {
            put("status", CaptureStatus.WAITING.name)
            put("updated_at", now)
            putNull("done_at")
        }
        writableDatabase.update("captures", values, "id = ?", arrayOf(id.toString()))
    }

    fun updateCategory(oldName: String, newName: String?, now: Long = System.currentTimeMillis()) {
        val values = ContentValues().apply {
            if (newName == null) putNull("category") else put("category", newName)
            put("updated_at", now)
        }
        writableDatabase.update("captures", values, "category = ?", arrayOf(oldName))
    }

    fun delete(id: Long) {
        writableDatabase.delete("captures", "id = ?", arrayOf(id.toString()))
    }

    fun expiredDone(now: Long): List<CaptureItem> {
        val cutoff = RetentionPolicy.doneCutoffMillis(now)
        readableDatabase.query(
            "captures",
            COLUMNS,
            "status = ? AND done_at IS NOT NULL AND done_at <= ?",
            arrayOf(CaptureStatus.DONE.name, cutoff.toString()),
            null,
            null,
            null
        ).use { cursor ->
            return buildList {
                while (cursor.moveToNext()) add(cursor.toCaptureItem())
            }
        }
    }

    fun list(status: CaptureStatus): List<CaptureItem> {
        val orderBy = if (status == CaptureStatus.WAITING) "created_at DESC" else "done_at DESC"
        readableDatabase.query(
            "captures",
            COLUMNS,
            "status = ?",
            arrayOf(status.name),
            null,
            null,
            orderBy
        ).use { cursor ->
            return buildList {
                while (cursor.moveToNext()) add(cursor.toCaptureItem())
            }
        }
    }

    fun all(): List<CaptureItem> {
        readableDatabase.query(
            "captures",
            COLUMNS,
            null,
            null,
            null,
            null,
            "created_at ASC"
        ).use { cursor ->
            return buildList {
                while (cursor.moveToNext()) add(cursor.toCaptureItem())
            }
        }
    }

    fun replaceAll(items: List<CaptureItem>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("captures", null, null)
            items.forEach { item ->
                db.insertOrThrow(
                    "captures",
                    null,
                    valuesFor(
                        id = item.id,
                        text = item.text,
                        category = item.category,
                        status = item.status,
                        createdAt = item.createdAt,
                        updatedAt = item.updatedAt,
                        doneAt = item.doneAt,
                        attachmentPath = item.attachmentPath,
                        attachmentMimeType = item.attachmentMimeType,
                        attachmentName = item.attachmentName
                    )
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun valuesFor(
        id: Long?,
        text: String,
        category: String?,
        status: CaptureStatus,
        createdAt: Long,
        updatedAt: Long,
        doneAt: Long?,
        attachmentPath: String?,
        attachmentMimeType: String?,
        attachmentName: String?
    ): ContentValues = ContentValues().apply {
        if (id != null) put("id", id)
        put("text", text.trim())
        if (category == null) putNull("category") else put("category", category)
        put("status", status.name)
        put("created_at", createdAt)
        put("updated_at", updatedAt)
        if (doneAt == null) putNull("done_at") else put("done_at", doneAt)
        if (attachmentPath == null) putNull("attachment_path") else put("attachment_path", attachmentPath)
        if (attachmentMimeType == null) putNull("attachment_mime_type") else put("attachment_mime_type", attachmentMimeType)
        if (attachmentName == null) putNull("attachment_name") else put("attachment_name", attachmentName)
    }

    private fun Cursor.toCaptureItem(): CaptureItem = CaptureItem(
        id = getLong(getColumnIndexOrThrow("id")),
        text = getString(getColumnIndexOrThrow("text")),
        category = getNullableString("category"),
        status = CaptureStatus.valueOf(getString(getColumnIndexOrThrow("status"))),
        createdAt = getLong(getColumnIndexOrThrow("created_at")),
        updatedAt = getLong(getColumnIndexOrThrow("updated_at")),
        doneAt = getNullableLong("done_at"),
        attachmentPath = getNullableString("attachment_path"),
        attachmentMimeType = getNullableString("attachment_mime_type"),
        attachmentName = getNullableString("attachment_name")
    )

    private fun Cursor.getNullableString(column: String): String? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getString(index)
    }

    private fun Cursor.getNullableLong(column: String): Long? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getLong(index)
    }

    companion object {
        private const val DATABASE_NAME = "quick.db"
        private const val DATABASE_VERSION = 2
        private val COLUMNS = arrayOf(
            "id",
            "text",
            "category",
            "status",
            "created_at",
            "updated_at",
            "done_at",
            "attachment_path",
            "attachment_mime_type",
            "attachment_name"
        )
    }
}
