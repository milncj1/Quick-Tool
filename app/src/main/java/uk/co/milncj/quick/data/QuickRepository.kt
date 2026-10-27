package uk.co.milncj.quick.data

import android.content.Context
import android.net.Uri

class QuickRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = QuickDatabase(appContext)
    private val attachments = AttachmentStore(appContext)

    fun save(
        text: String,
        category: String?,
        draftAttachment: DraftAttachment?,
        now: Long = System.currentTimeMillis()
    ): Long {
        val persisted = draftAttachment?.let(attachments::persist)
        return try {
            database.insert(text, category, persisted, now)
        } catch (error: Throwable) {
            attachments.deletePermanent(persisted?.path)
            throw error
        }
    }

    fun update(id: Long, text: String, category: String?, now: Long = System.currentTimeMillis()) =
        database.update(id, text, category, now)

    fun markDone(id: Long, now: Long = System.currentTimeMillis()) =
        database.markDone(id, now)

    fun undoDone(id: Long, now: Long = System.currentTimeMillis()) =
        database.markWaiting(id, now)

    fun reassignCategory(oldName: String, newName: String?) =
        database.updateCategory(oldName, newName)

    fun delete(item: CaptureItem) {
        database.delete(item.id)
        attachments.deletePermanent(item.attachmentPath)
    }

    fun waiting(): List<CaptureItem> = database.list(CaptureStatus.WAITING)

    fun done(): List<CaptureItem> = database.list(CaptureStatus.DONE)

    fun purgeExpiredDone(now: Long = System.currentTimeMillis()): Int {
        val expired = database.expiredDone(now)
        expired.forEach { item ->
            database.delete(item.id)
            attachments.deletePermanent(item.attachmentPath)
        }
        return expired.size
    }

    fun createCameraDraft(): AttachmentStore.CameraDraft = attachments.createCameraDraft()

    fun importSharedUri(uri: Uri, mimeType: String?): DraftAttachment =
        attachments.importSharedUri(uri, mimeType)

    fun discardDraft(draft: DraftAttachment?) = attachments.discardDraft(draft)
}
