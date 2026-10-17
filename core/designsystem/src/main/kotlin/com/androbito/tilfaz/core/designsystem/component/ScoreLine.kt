package com.androbito.tilfaz.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.androbito.tilfaz.core.designsystem.R
import com.androbito.tilfaz.core.designsystem.theme.Dimens
import com.androbito.tilfaz.core.designsystem.theme.TilfazColors
import com.androbito.tilfaz.core.model.Rating

/** "7.1 / 10" with filled and empty stars, the way the user is used to seeing scores on detail pages. */
@Composable
fun ScoreLine(rating: Rating, modifier: Modifier = Modifier) {
    val filled = rating.outOfFive.toInt().coerceIn(0, 5)
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        Text("★".repeat(filled) + "☆".repeat(5 - filled), style = MaterialTheme.typography.titleLarge, color = TilfazColors.Accent)
        Text(stringResource(R.string.ds_score, rating.outOfTen), style = MaterialTheme.typography.titleMedium, color = TilfazColors.OnSurfaceMuted)
    }
}
