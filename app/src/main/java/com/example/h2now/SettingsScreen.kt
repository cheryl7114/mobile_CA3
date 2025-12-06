package com.example.h2now

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    waterViewModel: WaterViewModel
) {
    val darkModeEnabled by waterViewModel.darkModeEnabled.collectAsState()
    val notificationsEnabled by waterViewModel.notificationsEnabled.collectAsState()
    val reminderAlertsEnabled by waterViewModel.reminderAlertsEnabled.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        SettingItem(
            title = "Dark Mode",
            checked = darkModeEnabled,
            onCheckedChange = { waterViewModel.setDarkMode(it) }
        )
        SettingItem(
            title = "Enable Notifications",
            checked = notificationsEnabled,
            onCheckedChange = { waterViewModel.setNotifications(it) }
        )
        SettingItem(
            title = "Reminder Alerts",
            checked = reminderAlertsEnabled,
            onCheckedChange = { waterViewModel.setReminderAlerts(it) }
        )
    }
}

@Composable
fun SettingItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
