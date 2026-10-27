package uk.co.milncj.quick

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import uk.co.milncj.quick.ui.QuickApp
import uk.co.milncj.quick.ui.QuickCaptureAction
import uk.co.milncj.quick.ui.QuickLaunchRequest
import uk.co.milncj.quick.ui.QuickScreen
import uk.co.milncj.quick.ui.QuickTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_OPEN_CAPTURE = "uk.co.milncj.quick.OPEN_CAPTURE"
        const val ACTION_OPEN_WAITING = "uk.co.milncj.quick.OPEN_WAITING"
        const val ACTION_OPEN_VOICE = "uk.co.milncj.quick.OPEN_VOICE"
        const val ACTION_OPEN_PHOTO = "uk.co.milncj.quick.OPEN_PHOTO"
    }

    private val launchRequests = MutableStateFlow(QuickLaunchRequest())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        launchRequests.value = requestFromIntent(intent)

        setContent {
            QuickTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val quickViewModel: QuickViewModel = viewModel()
                    QuickApp(
                        viewModel = quickViewModel,
                        launchRequests = launchRequests
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchRequests.value = requestFromIntent(intent)
    }

    private fun requestFromIntent(intent: Intent?): QuickLaunchRequest {
        return when (intent?.action) {
            ACTION_OPEN_WAITING -> QuickLaunchRequest(screen = QuickScreen.WAITING)
            ACTION_OPEN_VOICE -> QuickLaunchRequest(
                screen = QuickScreen.CAPTURE,
                captureAction = QuickCaptureAction.VOICE
            )
            ACTION_OPEN_PHOTO -> QuickLaunchRequest(
                screen = QuickScreen.CAPTURE,
                captureAction = QuickCaptureAction.PHOTO
            )
            Intent.ACTION_SEND -> QuickLaunchRequest(
                screen = QuickScreen.CAPTURE,
                sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString(),
                sharedUri = IntentCompat.getParcelableExtra(
                    intent,
                    Intent.EXTRA_STREAM,
                    Uri::class.java
                ),
                sharedMimeType = intent.type
            )
            else -> QuickLaunchRequest(screen = QuickScreen.CAPTURE)
        }
    }
}
