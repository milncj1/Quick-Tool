package uk.co.milncj.quick.ui

import android.net.Uri

data class QuickLaunchRequest(
    val requestId: Long = System.nanoTime(),
    val screen: QuickScreen = QuickScreen.CAPTURE,
    val captureAction: QuickCaptureAction? = null,
    val sharedText: String? = null,
    val sharedUri: Uri? = null,
    val sharedMimeType: String? = null
)

enum class QuickCaptureAction {
    VOICE,
    PHOTO
}
