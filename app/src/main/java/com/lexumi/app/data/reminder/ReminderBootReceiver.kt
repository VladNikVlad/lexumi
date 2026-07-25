package com.lexumi.app.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-arms the daily study reminder after a device reboot, if the user has
 * reminders enabled in Settings. Actual scheduling (AlarmManager /
 * WorkManager) can be wired in here; kept minimal since notification content
 * and timing are a product decision outside the scenario's core scope.
 */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // TODO: read UserPreferences.remindersEnabled and re-schedule via WorkManager.
        }
    }
}
