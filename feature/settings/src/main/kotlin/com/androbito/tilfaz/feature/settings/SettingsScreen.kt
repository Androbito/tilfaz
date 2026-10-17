package com.androbito.tilfaz.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.androbito.tilfaz.core.designsystem.component.FocusableSurface
import com.androbito.tilfaz.core.designsystem.component.ProvideMinimalBringIntoView
import com.androbito.tilfaz.core.designsystem.component.TilfazButton
import com.androbito.tilfaz.core.designsystem.component.TopBar
import com.androbito.tilfaz.core.designsystem.component.TvTextField
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors
import com.androbito.tilfaz.core.model.BufferProfile
import com.androbito.tilfaz.core.model.StreamFormat
import com.androbito.tilfaz.core.model.SubtitleBackground
import com.androbito.tilfaz.core.model.SubtitleColor
import com.androbito.tilfaz.core.model.SubtitleSize
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

fun NavGraphBuilder.settingsScreen(onSignedOut: () -> Unit) {
    composable<SettingsRoute> { SettingsRoute(onSignedOut) }
}

@Composable
fun SettingsRoute(onSignedOut: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.signedOut) { if (state.signedOut) onSignedOut() }
    SettingsScreen(state, viewModel::onEvent)
}

@Composable
fun SettingsScreen(state: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    val signOutFocus = remember { FocusRequester() }
    val cancelFocus = remember { FocusRequester() }
    var restoreSignOutFocus by remember { mutableStateOf(false) }
    // The button that was pressed disappears when the confirmation shows (and vice versa), so focus has to be handed over.
    LaunchedEffect(state.confirmSignOut) {
        withFrameNanos { } // the new button joins the focus tree once its first frame is laid out
        if (state.confirmSignOut) {
            cancelFocus.requestFocus()
        } else if (restoreSignOutFocus) {
            signOutFocus.requestFocus()
            restoreSignOutFocus = false
        }
    }
    ProvideMinimalBringIntoView {
        Column(Modifier.fillMaxSize()) {
            TopBar(title = "Tilfaz · ${stringResource(R.string.settings_title)}")
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Dimens.ScreenPadding).padding(bottom = Dimens.SpaceXl),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL)
            ) {
                SectionTitle(stringResource(R.string.settings_section_playback))

                Setting(stringResource(R.string.settings_live_format), stringResource(R.string.settings_live_format_hint)) {
                    ChoiceRow(
                        options = listOf(
                            StreamFormat.MPEG_TS to stringResource(R.string.settings_format_ts),
                            StreamFormat.HLS to stringResource(R.string.settings_format_hls)
                        ),
                        selected = state.settings.liveFormat,
                        onSelect = { onEvent(SettingsEvent.SetLiveFormat(it)) }
                    )
                }
                Setting(stringResource(R.string.settings_buffer)) {
                    ChoiceRow(
                        options = listOf(
                            BufferProfile.LOW_LATENCY to stringResource(R.string.settings_buffer_low),
                            BufferProfile.BALANCED to stringResource(R.string.settings_buffer_balanced),
                            BufferProfile.STABLE to stringResource(R.string.settings_buffer_stable)
                        ),
                        selected = state.settings.bufferProfile,
                        onSelect = { onEvent(SettingsEvent.SetBuffer(it)) }
                    )
                }
                Setting(stringResource(R.string.settings_user_agent), stringResource(R.string.settings_user_agent_hint)) {
                    var draft by remember(state.settings.userAgent) { mutableStateOf(state.settings.userAgent) }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TvTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            label = "",
                            onImeAction = { onEvent(SettingsEvent.SetUserAgent(draft)) },
                            modifier = Modifier.width(560.dp)
                        )
                        TilfazButton(onClick = { onEvent(SettingsEvent.SetUserAgent(draft)) }) { Text("OK") }
                        OutlinedButton(onClick = { onEvent(SettingsEvent.ResetUserAgent) }) { Text(stringResource(R.string.settings_user_agent_reset)) }
                    }
                }
                Setting(stringResource(R.string.settings_resume)) {
                    ChoiceRow(
                        options = listOf(true to stringResource(R.string.settings_on), false to stringResource(R.string.settings_off)),
                        selected = state.settings.resumePlayback,
                        onSelect = { onEvent(SettingsEvent.SetResume(it)) }
                    )
                }

                SectionTitle(stringResource(R.string.settings_section_subtitles))

                Setting(stringResource(R.string.settings_audio_language), stringResource(R.string.settings_audio_language_hint)) {
                    ChoiceRow(
                        options = listOf<Pair<String?, String>>(null to stringResource(R.string.settings_lang_auto)) + languageOptions(),
                        selected = state.settings.audioLanguage,
                        onSelect = { onEvent(SettingsEvent.SetAudioLanguage(it)) }
                    )
                }
                Setting(stringResource(R.string.settings_subtitle_language)) {
                    ChoiceRow(
                        options = listOf<Pair<String?, String>>(null to stringResource(R.string.settings_off)) + languageOptions(),
                        selected = state.settings.subtitleLanguage,
                        onSelect = { onEvent(SettingsEvent.SetSubtitleLanguage(it)) }
                    )
                }
                Setting(stringResource(R.string.settings_subtitle_size)) {
                    ChoiceRow(
                        options = listOf(
                            SubtitleSize.SMALL to stringResource(R.string.settings_size_small),
                            SubtitleSize.MEDIUM to stringResource(R.string.settings_size_medium),
                            SubtitleSize.LARGE to stringResource(R.string.settings_size_large),
                            SubtitleSize.EXTRA_LARGE to stringResource(R.string.settings_size_extra_large)
                        ),
                        selected = state.settings.subtitleStyle.size,
                        onSelect = { onEvent(SettingsEvent.SetSubtitleSize(it)) }
                    )
                }
                Setting(stringResource(R.string.settings_subtitle_color)) {
                    ChoiceRow(
                        options = listOf(
                            SubtitleColor.WHITE to stringResource(R.string.settings_color_white),
                            SubtitleColor.YELLOW to stringResource(R.string.settings_color_yellow),
                            SubtitleColor.CYAN to stringResource(R.string.settings_color_cyan),
                            SubtitleColor.GREEN to stringResource(R.string.settings_color_green)
                        ),
                        selected = state.settings.subtitleStyle.textColor,
                        onSelect = { onEvent(SettingsEvent.SetSubtitleColor(it)) }
                    )
                }
                Setting(stringResource(R.string.settings_subtitle_background)) {
                    ChoiceRow(
                        options = listOf(
                            SubtitleBackground.NONE to stringResource(R.string.settings_background_none),
                            SubtitleBackground.TRANSLUCENT to stringResource(R.string.settings_background_translucent),
                            SubtitleBackground.BLACK to stringResource(R.string.settings_background_black),
                            SubtitleBackground.GRAY to stringResource(R.string.settings_background_gray),
                            SubtitleBackground.BLUE to stringResource(R.string.settings_background_blue)
                        ),
                        selected = state.settings.subtitleStyle.background,
                        onSelect = { onEvent(SettingsEvent.SetSubtitleBackground(it)) }
                    )
                }

                SectionTitle(stringResource(R.string.settings_section_account))
                state.account?.let { account ->
                    InfoLine(stringResource(R.string.settings_account_server), account.serverUrl)
                    InfoLine(stringResource(R.string.settings_account_username), account.username)
                }
                state.status?.let { status ->
                    InfoLine(
                        stringResource(R.string.settings_account_expires),
                        status.expiresAt?.atZone(ZoneId.systemDefault())?.toLocalDate()?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                            ?: stringResource(R.string.settings_account_unlimited)
                    )
                    InfoLine(stringResource(R.string.settings_account_connections), "${status.activeConnections} / ${status.maxConnections}")
                    InfoLine(stringResource(R.string.settings_account_formats), status.allowedOutputFormats.joinToString { it.extension }.ifEmpty { "-" })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
                    OutlinedButton(onClick = {
                        onEvent(SettingsEvent.RefreshStatus)
                    }, enabled = !state.isRefreshingStatus) {
                        Text(stringResource(if (state.isRefreshingStatus) R.string.settings_account_refreshing else R.string.settings_account_refresh))
                    }
                    if (state.confirmSignOut) {
                        Text(
                            stringResource(R.string.settings_sign_out_confirm),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = Dimens.SpaceS)
                        )
                        TilfazButton(onClick = { onEvent(SettingsEvent.SignOutConfirmed) }) { Text(stringResource(R.string.settings_sign_out_yes)) }
                        OutlinedButton(
                            onClick = {
                                restoreSignOutFocus = true
                                onEvent(SettingsEvent.SignOutCancelled)
                            },
                            modifier = Modifier.focusRequester(cancelFocus)
                        ) { Text(stringResource(R.string.settings_cancel)) }
                    } else {
                        OutlinedButton(onClick = { onEvent(SettingsEvent.SignOutRequested) }, modifier = Modifier.focusRequester(signOutFocus)) {
                            Text(stringResource(R.string.settings_sign_out))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun languageOptions(): List<Pair<String?, String>> = listOf(
    "ar" to stringResource(R.string.settings_lang_arabic),
    "en" to stringResource(R.string.settings_lang_english),
    "fr" to stringResource(R.string.settings_lang_french)
)

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = TilfazColors.Accent)
}

@Composable
private fun Setting(title: String, hint: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodyMedium, color = TilfazColors.OnSurfaceMuted)
        content()
    }
}

@Composable
private fun <T> ChoiceRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        options.forEach { (value, label) ->
            FocusableSurface(onClick = { onSelect(value) }, selected = value == selected, corner = Dimens.CornerS, focusedScale = 1.0f) {
                Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceS))
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = TilfazColors.OnSurfaceMuted, modifier = Modifier.width(260.dp))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
