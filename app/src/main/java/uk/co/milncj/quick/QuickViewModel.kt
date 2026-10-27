package uk.co.milncj.quick

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.co.milncj.quick.backup.QuickBackupManager
import uk.co.milncj.quick.data.AttachmentStore
import uk.co.milncj.quick.data.CaptureItem
import uk.co.milncj.quick.data.CategoryStore
import uk.co.milncj.quick.data.DraftAttachment
import uk.co.milncj.quick.data.QuickRepository
import uk.co.milncj.quick.widget.QuickWidgetProvider

data class QuickUiState(
    val waiting: List<CaptureItem> = emptyList(),
    val done: List<CaptureItem> = emptyList(),
    val categories: List<String> = emptyList(),
    val isLoading: Boolean = true
)

class QuickViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuickRepository(application)
    private val categoryStore = CategoryStore(application)
    private val backupManager = QuickBackupManager(application)
    private val prefs = application.getSharedPreferences(PREFS_NAME, 0)

    private val _uiState = MutableStateFlow(QuickUiState())
    val uiState: StateFlow<QuickUiState> = _uiState.asStateFlow()

    val lastCategory: String?
        get() = prefs.getString(KEY_LAST_CATEGORY, null)
            ?.takeIf { categoryStore.load().contains(it) }

    init {
        refresh()
    }

    fun save(
        text: String,
        category: String?,
        attachment: DraftAttachment?,
        onSaved: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        if (text.isBlank() && attachment == null) return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.save(text, category, attachment)
                    prefs.edit().apply {
                        if (category == null) remove(KEY_LAST_CATEGORY) else putString(KEY_LAST_CATEGORY, category)
                    }.apply()
                }
            }.onSuccess {
                refresh()
                onSaved()
            }.onFailure(onError)
        }
    }

    fun update(item: CaptureItem, text: String, category: String?) {
        if (text.isBlank() && !item.hasAttachment) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.update(item.id, text, category) }
            refresh()
        }
    }

    fun markDone(item: CaptureItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.markDone(item.id) }
            refresh()
        }
    }

    fun undoDone(item: CaptureItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.undoDone(item.id) }
            refresh()
        }
    }

    fun delete(item: CaptureItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.delete(item) }
            refresh()
        }
    }

    fun addCategory(name: String, onResult: (Boolean) -> Unit = {}) {
        val cleaned = name.trim().take(30)
        if (cleaned.isBlank()) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val added = withContext(Dispatchers.IO) {
                val current = categoryStore.load()
                if (current.size >= CategoryStore.MAX_CATEGORIES || current.any { it.equals(cleaned, true) }) {
                    false
                } else {
                    categoryStore.save(current + cleaned)
                    true
                }
            }
            if (added) refresh()
            onResult(added)
        }
    }

    fun renameCategory(oldName: String, newName: String, onResult: (Boolean) -> Unit = {}) {
        val cleaned = newName.trim().take(30)
        if (cleaned.isBlank()) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val renamed = withContext(Dispatchers.IO) {
                val current = categoryStore.load()
                if (current.any { !it.equals(oldName, true) && it.equals(cleaned, true) }) {
                    false
                } else {
                    repository.reassignCategory(oldName, cleaned)
                    categoryStore.save(current.map { if (it == oldName) cleaned else it })
                    if (prefs.getString(KEY_LAST_CATEGORY, null) == oldName) {
                        prefs.edit().putString(KEY_LAST_CATEGORY, cleaned).apply()
                    }
                    true
                }
            }
            if (renamed) refresh()
            onResult(renamed)
        }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.reassignCategory(name, null)
                categoryStore.save(categoryStore.load().filterNot { it == name })
                if (prefs.getString(KEY_LAST_CATEGORY, null) == name) {
                    prefs.edit().remove(KEY_LAST_CATEGORY).apply()
                }
            }
            refresh()
        }
    }

    fun moveCategory(name: String, direction: Int) {
        if (direction == 0) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val current = categoryStore.load().toMutableList()
                val index = current.indexOf(name)
                if (index < 0) return@withContext
                val target = (index + direction).coerceIn(0, current.lastIndex)
                if (target != index) {
                    val moved = current.removeAt(index)
                    current.add(target, moved)
                    categoryStore.save(current)
                }
            }
            refresh()
        }
    }

    fun exportBackup(
        uri: Uri,
        onSuccess: (QuickBackupManager.Result) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { backupManager.exportTo(uri) }
            }.onSuccess(onSuccess).onFailure(onError)
        }
    }

    fun restoreBackup(
        uri: Uri,
        onSuccess: (QuickBackupManager.Result) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { backupManager.restoreFrom(uri) }
            }.onSuccess { result ->
                val available = categoryStore.load()
                if (prefs.getString(KEY_LAST_CATEGORY, null)?.let(available::contains) != true) {
                    prefs.edit().remove(KEY_LAST_CATEGORY).apply()
                }
                refresh()
                onSuccess(result)
            }.onFailure(onError)
        }
    }

    fun createCameraDraft(
        onCreated: (AttachmentStore.CameraDraft) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { repository.createCameraDraft() }
            }.onSuccess(onCreated).onFailure(onError)
        }
    }

    fun importSharedAttachment(
        uri: Uri,
        mimeType: String?,
        onImported: (DraftAttachment) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { repository.importSharedUri(uri, mimeType) }
            }.onSuccess(onImported).onFailure(onError)
        }
    }

    fun discardDraft(draft: DraftAttachment?) {
        if (draft == null) return
        viewModelScope.launch(Dispatchers.IO) { repository.discardDraft(draft) }
    }

    fun refresh() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.purgeExpiredDone()
                Triple(repository.waiting(), repository.done(), categoryStore.load())
            }
            _uiState.value = QuickUiState(
                waiting = result.first,
                done = result.second,
                categories = result.third,
                isLoading = false
            )
            QuickWidgetProvider.refreshAll(getApplication())
        }
    }

    companion object {
        private const val PREFS_NAME = "quick_preferences"
        private const val KEY_LAST_CATEGORY = "last_category"
    }
}
