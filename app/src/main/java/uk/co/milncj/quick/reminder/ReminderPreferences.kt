package uk.co.milncj.quick.reminder

import android.content.Context

data class ReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = 18,
    val minute: Int = 0,
    val oldItemDays: Int = 3
)

class ReminderPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, 0)

    fun load(): ReminderSettings = ReminderSettings(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        hour = prefs.getInt(KEY_HOUR, 18),
        minute = prefs.getInt(KEY_MINUTE, 0),
        oldItemDays = prefs.getInt(KEY_OLD_ITEM_DAYS, 3)
    )

    fun save(settings: ReminderSettings) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putInt(KEY_HOUR, settings.hour)
            .putInt(KEY_MINUTE, settings.minute)
            .putInt(KEY_OLD_ITEM_DAYS, settings.oldItemDays)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "quick_reminders"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_HOUR = "hour"
        private const val KEY_MINUTE = "minute"
        private const val KEY_OLD_ITEM_DAYS = "old_item_days"
    }
}
