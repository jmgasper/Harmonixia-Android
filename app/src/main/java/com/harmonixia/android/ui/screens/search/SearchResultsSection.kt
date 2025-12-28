package com.harmonixia.android.ui.screens.search

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.harmonixia.android.R

@Composable
fun SearchResultsSection(
    title: String,
    itemCount: Int,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    content: @Composable () -> Unit
) {
    val windowSizeClass = calculateWindowSizeClass(activity = LocalContext.current as Activity)
    val headerPadding = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 12.dp
        WindowWidthSizeClass.Medium -> 16.dp
        WindowWidthSizeClass.Expanded -> 20.dp
        else -> 12.dp
    }
    val headerSpacing = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 8.dp
        else -> 12.dp
    }
    val headerTextStyle = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> MaterialTheme.typography.titleSmall
        else -> MaterialTheme.typography.titleMedium
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(headerPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(headerSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = headerTextStyle,
                    modifier = Modifier.weight(1f)
                )
                SectionCountBadge(count = itemCount)
                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Outlined.ExpandLess
                        } else {
                            Icons.Outlined.ExpandMore
                        },
                        contentDescription = stringResource(
                            if (isExpanded) {
                                R.string.content_desc_collapse_section
                            } else {
                                R.string.content_desc_expand_section
                            }
                        )
                    )
                }
            }
            if (isExpanded) {
                Spacer(modifier = Modifier.height(headerSpacing))
                content()
            }
        }
    }
}

@Composable
private fun SectionCountBadge(count: Int) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
