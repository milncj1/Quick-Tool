package uk.co.milncj.quick.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow
import uk.co.milncj.quick.QuickViewModel

enum class QuickScreen(val label: String) {
    CAPTURE("Capture"),
    WAITING("Waiting"),
    REVIEW("Review"),
    SETTINGS("Settings"),
    CATEGORIES("Categories")
}

@Composable
fun QuickApp(
    viewModel: QuickViewModel,
    launchRequests: StateFlow<QuickLaunchRequest>
) {
    val uiState by viewModel.uiState.collectAsState()
    val launchRequest by launchRequests.collectAsState()
    var screen by rememberSaveable { mutableStateOf(launchRequest.screen) }
    var consumedRequestId by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(launchRequest.requestId) {
        screen = launchRequest.screen
        if (launchRequest.screen != QuickScreen.CAPTURE) {
            consumedRequestId = launchRequest.requestId
        }
    }

    val pendingCaptureRequest = launchRequest.takeIf {
        it.screen == QuickScreen.CAPTURE && it.requestId != consumedRequestId
    }

    val showBottomBar = screen in setOf(QuickScreen.CAPTURE, QuickScreen.WAITING, QuickScreen.REVIEW)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = screen == QuickScreen.CAPTURE,
                        onClick = { screen = QuickScreen.CAPTURE },
                        icon = { Text("+") },
                        label = { Text("Capture") }
                    )
                    NavigationBarItem(
                        selected = screen == QuickScreen.WAITING,
                        onClick = { screen = QuickScreen.WAITING },
                        icon = { Text(if (uiState.waiting.isEmpty()) "◷" else uiState.waiting.size.toString()) },
                        label = { Text("Waiting") }
                    )
                    NavigationBarItem(
                        selected = screen == QuickScreen.REVIEW,
                        onClick = { screen = QuickScreen.REVIEW },
                        icon = { Text("⌕") },
                        label = { Text("Review") }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (screen) {
            QuickScreen.CAPTURE -> CaptureScreen(
                modifier = Modifier.padding(innerPadding),
                viewModel = viewModel,
                initialCategory = viewModel.lastCategory,
                categories = uiState.categories,
                waitingCount = uiState.waiting.size,
                launchRequest = pendingCaptureRequest,
                onLaunchRequestConsumed = { consumedRequestId = it },
                onOpenWaiting = { screen = QuickScreen.WAITING },
                snackbarHostState = snackbarHostState
            )

            QuickScreen.WAITING -> ItemListScreen(
                modifier = Modifier.padding(innerPadding),
                items = uiState.waiting,
                categories = uiState.categories,
                onDone = viewModel::markDone,
                onUndoDone = viewModel::undoDone,
                onEdit = viewModel::update,
                onDelete = viewModel::delete,
                snackbarHostState = snackbarHostState
            )

            QuickScreen.REVIEW -> ReviewScreen(
                modifier = Modifier.padding(innerPadding),
                viewModel = viewModel,
                waiting = uiState.waiting,
                done = uiState.done,
                categories = uiState.categories,
                snackbarHostState = snackbarHostState,
                onOpenSettings = { screen = QuickScreen.SETTINGS }
            )

            QuickScreen.SETTINGS -> SettingsScreen(
                modifier = Modifier.padding(innerPadding),
                viewModel = viewModel,
                onBack = { screen = QuickScreen.REVIEW },
                onOpenCategories = { screen = QuickScreen.CATEGORIES }
            )

            QuickScreen.CATEGORIES -> CategoriesScreen(
                modifier = Modifier.padding(innerPadding),
                viewModel = viewModel,
                categories = uiState.categories,
                onBack = { screen = QuickScreen.SETTINGS }
            )
        }
    }
}
