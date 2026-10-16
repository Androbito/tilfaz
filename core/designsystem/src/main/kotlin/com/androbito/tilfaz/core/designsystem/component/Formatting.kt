package com.androbito.tilfaz.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.androbito.tilfaz.core.designsystem.R
import java.time.Duration
import java.time.Instant

/** "Last updated: 42 min ago" the way the home tiles show it. */
@Composable
fun lastUpdatedLabel(lastSyncedAt: Instant?, now: Instant): String {
    if (lastSyncedAt == null) return stringResource(R.string.ds_never_updated)
    return stringResource(R.string.ds_last_updated, relativeTime(lastSyncedAt, now))
}

@Composable
fun relativeTime(instant: Instant, now: Instant): String {
    val duration = Duration.between(instant, now).abs()
    return when {
        duration.toMinutes() < 1 -> stringResource(R.string.ds_just_now)
        duration.toHours() < 1 -> stringResource(R.string.ds_minutes_ago, duration.toMinutes().toInt())
        duration.toDays() < 1 -> stringResource(R.string.ds_hours_ago, duration.toHours().toInt())
        else -> stringResource(R.string.ds_days_ago, duration.toDays().toInt())
    }
}

fun formatDuration(seconds: Long?): String? {
    if (seconds == null || seconds <= 0) return null
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

fun formatPlayerTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
