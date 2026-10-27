package uk.co.milncj.quick.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import uk.co.milncj.quick.BuildConfig
import uk.co.milncj.quick.QuickViewModel
import uk.co.milncj.quick.reminder.ReminderPreferences
import uk.co.milncj.quick.reminder.ReminderScheduler
import uk.co.milncj.quick.reminder.ReminderSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: QuickViewModel,
    onBack: () -> Unit,
    onOpenCategories: () -> Unit
) {
    val context = LocalContext.current
    var reminderSettings by remember { mutableStateOf(ReminderPreferences(context).load()) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    fun persistReminder(settings: ReminderSettings) {
        ReminderPreferences(context).save(settings)
        ReminderScheduler.apply(context, settings)
        reminderSettings = settings
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            persistReminder(reminderSettings.copy(enabled = true))
            message = "Waiting reminders enabled."
        } else {
            persistReminder(reminderSettings.copy(enabled = false))
            message = "Notifications weren't enabled."
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        if (!enabled) {
            persistReminder(reminderSettings.copy(enabled = false))
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            persistReminder(reminderSettings.copy(enabled = true))
            message = "Waiting reminders enabled."
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(
                uri,
                onSuccess = { result ->
                    message = "Backup saved: ${result.itemCount} captures, ${result.attachmentCount} attachments."
                },
                onError = { message = "Couldn't create the backup." }
            )
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingRestore = uri
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        HeaderWithBack(title = "Settings", onBack = onBack)

        SettingsSectionTitle("REMINDERS")
        SettingsToggleRow(
            title = "Waiting reminder",
            subtitle = "Only when Quick isn't empty",
            checked = reminderSettings.enabled,
            onCheckedChange = ::setReminderEnabled
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingsRow(
            title = "Reminder time",
            value = "%02d:%02d".format(reminderSettings.hour, reminderSettings.minute),
            onClick = {
                TimePickerDialog(
                    context,
                    { _, hour, minute -> persistReminder(reminderSettings.copy(hour = hour, minute = minute)) },
                    reminderSettings.hour,
                    reminderSettings.minute,
                    true
                ).show()
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
            Text("Older-item warning", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(reminderSettings.oldItemDays == 0, { persistReminder(reminderSettings.copy(oldItemDays = 0)) }, label = { Text("Off") })
                FilterChip(reminderSettings.oldItemDays == 3, { persistReminder(reminderSettings.copy(oldItemDays = 3)) }, label = { Text("3 days") })
                FilterChip(reminderSettings.oldItemDays == 7, { persistReminder(reminderSettings.copy(oldItemDays = 7)) }, label = { Text("7 days") })
            }
        }

        SettingsSectionTitle("CATEGORIES")
        SettingsRow(title = "Manage categories", value = "›", onClick = onOpenCategories)

        SettingsSectionTitle("DATA")
        SettingsRow(
            title = "Create backup",
            value = "›",
            onClick = {
                val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(Date())
                exportLauncher.launch("quick-backup-$stamp.quick.zip")
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingsRow(
            title = "Restore backup",
            value = "›",
            onClick = { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }
        )

        SettingsSectionTitle("ABOUT")
        SettingsRow(title = "Quick", value = BuildConfig.VERSION_NAME)

        message?.let {
            Spacer(Modifier.height(18.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(28.dp))
    }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restore Quick backup?") },
            text = { Text("This replaces the captures, categories and reminder settings currently in Quick with the backup contents.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    viewModel.restoreBackup(
                        uri,
                        onSuccess = { result ->
                            message = "Restored ${result.itemCount} captures and ${result.attachmentCount} attachments."
                        },
                        onError = { message = "Couldn't restore that Quick backup." }
                    )
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancel") } }
        )
    }
}

@Composable
internal fun CategoriesScreen(
    modifier: Modifier = Modifier,
    viewModel: QuickViewModel,
    categories: List<String>,
    onBack: () -> Unit
) {
    var newCategory by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        HeaderWithBack(title = "Categories", onBack = onBack)
        Text(
            "Add, rename or reorder. Deleting a category keeps its captures and changes them to None.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))

        categories.forEachIndexed { index, category ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(category, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = index > 0,
                            onClick = { viewModel.moveCategory(category, -1) }
                        ) { Text("↑ Up") }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = index < categories.lastIndex,
                            onClick = { viewModel.moveCategory(category, 1) }
                        ) { Text("↓ Down") }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                renaming = category
                                renameText = category
                            }
                        ) { Text("Rename") }
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = { deleting = category }
                        ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = newCategory,
            onValueChange = { newCategory = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("New category") },
            shape = MaterialTheme.shapes.medium
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                viewModel.addCategory(newCategory) { added ->
                    message = if (added) null else "That category couldn't be added."
                    if (added) newCategory = ""
                }
            }
        ) { Text("+ Add category") }

        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(28.dp))
    }

    renaming?.let { oldName ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename $oldName") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameCategory(oldName, renameText) { success ->
                        if (!success) message = "That category name is already in use."
                    }
                    renaming = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } }
        )
    }

    deleting?.let { category ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete $category?") },
            text = { Text("Existing captures in this category will become None. The captures themselves are not deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory(category)
                    deleting = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun HeaderWithBack(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) { Text("‹ Back") }
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Spacer(Modifier.height(24.dp))
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SettingsRow(title: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
