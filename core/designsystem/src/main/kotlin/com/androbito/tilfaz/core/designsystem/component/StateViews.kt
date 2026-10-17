package com.androbito.tilfaz.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.androbito.tilfaz.core.designsystem.R
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors

@Composable
fun LoadingState(modifier: Modifier = Modifier, label: String = stringResource(R.string.ds_loading)) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = TilfazColors.OnSurfaceMuted)
    }
}

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.ds_empty_title),
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier.fillMaxSize().padding(Dimens.SpaceXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Inbox, contentDescription = null, tint = TilfazColors.OnSurfaceMuted, modifier = Modifier.size(64.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = Dimens.SpaceM), textAlign = TextAlign.Center)
        if (message != null) {
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = TilfazColors.OnSurfaceMuted,
                modifier = Modifier.padding(top = Dimens.SpaceS),
                textAlign = TextAlign.Center
            )
        }
        if (actionLabel != null && onAction != null) {
            TilfazButton(onClick = onAction, modifier = Modifier.padding(top = Dimens.SpaceL)) { Text(actionLabel) }
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    retryLabel: String = stringResource(R.string.ds_retry)
) {
    Column(
        modifier.fillMaxSize().padding(Dimens.SpaceXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = TilfazColors.Danger, modifier = Modifier.size(64.dp))
        Text(message, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = Dimens.SpaceM), textAlign = TextAlign.Center)
        if (onRetry != null) {
            TilfazButton(onClick = onRetry, modifier = Modifier.padding(top = Dimens.SpaceL)) { Text(retryLabel) }
        }
    }
}
