package uk.co.milncj.quick.data

data class CaptureItem(
    val id: Long,
    val text: String,
    val category: String?,
    val status: CaptureStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val doneAt: Long?,
    val attachmentPath: String?,
    val attachmentMimeType: String?,
    val attachmentName: String?
) {
    val hasAttachment: Boolean
        get() = !attachmentPath.isNullOrBlank()
}

data class DraftAttachment(
    val path: String,
    val mimeType: String?,
    val displayName: String
)

enum class CaptureStatus {
    WAITING,
    DONE
}
