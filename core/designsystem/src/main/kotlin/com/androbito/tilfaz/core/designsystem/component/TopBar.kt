package com.androbito.tilfaz.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay

/** Brand + clock bar like the Smarters header: logo left, time/date, then whatever the screen adds on the right. */
@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    title: String = "Tilfaz",
    showClock: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val now by rememberCurrentTime()
    Row(
        modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceL),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceL)
    ) {
        BrandMark(title)
        if (showClock) {
            val local = LocalDateTime.ofInstant(now, ZoneId.systemDefault())
            Text(
                text = local.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = local.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                style = MaterialTheme.typography.titleMedium,
                color = TilfazColors.OnSurfaceMuted
            )
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) { actions() }
    }
}

@Composable
fun BrandMark(title: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing =
                MaterialTheme.typography.headlineSmall.fontSize * 0.08
            ),
            color = TilfazColors.Accent
        )
    }
}

/** Ticks every 30 seconds; cheap enough to keep the clock honest without re-composing every second. */
@Composable
fun rememberCurrentTime(): State<Instant> {
    val state = remember { mutableStateOf(Instant.now()) }
    var tick by state
    LaunchedEffect(Unit) {
        while (true) {
            tick = Instant.now()
            delay(30_000L)
        }
    }
    return state
}
