package com.sameerasw.medrop.ui.core.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.sameerasw.medrop.ui.core.containers.RoundedCardContainer
import com.sameerasw.medrop.utils.HapticUtil

val LocalDropdownMenuDismiss = staticCompositionLocalOf<(() -> Unit)?> { null }

@Composable
fun SegmentedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    containerColor: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        containerColor = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        CompositionLocalProvider(LocalDropdownMenuDismiss provides onDismissRequest) {
            RoundedCardContainer(
                cornerRadius = 16.dp,
                spacing = 2.dp,
                content = content,
            )
        }
    }
}

@Composable
fun SegmentedDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    itemContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    colors: MenuItemColors =
        MenuDefaults.itemColors(
            textColor = MaterialTheme.colorScheme.onSurface,
            leadingIconColor = MaterialTheme.colorScheme.onSurface,
            trailingIconColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
) {
    val view = LocalView.current
    val dismiss = LocalDropdownMenuDismiss.current
    DropdownMenuItem(
        text = text,
        onClick = {
            HapticUtil.performUIHaptic(view)
            onClick()
            dismiss?.invoke()
        },
        modifier =
            modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .background(itemContainerColor),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        colors = colors,
    )
}
