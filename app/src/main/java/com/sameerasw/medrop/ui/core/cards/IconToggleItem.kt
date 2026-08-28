package com.sameerasw.medrop.ui.core.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.medrop.utils.HapticUtil

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IconToggleItem(
    iconRes: Int = 0,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    isChecked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    enabled: Boolean = true,
    onDisabledClick: (() -> Unit)? = null,
    showToggle: Boolean = true,
    onClick: (() -> Unit)? = null,
    subtitle: String? = null,
    icon: Int? = null,
    checked: Boolean? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val view = LocalView.current
    val finalIconRes = icon ?: iconRes
    val finalDescription = subtitle ?: description
    val finalIsChecked = checked ?: isChecked

    val onClickAction: (() -> Unit)? =
        if (onClick != null || showToggle) {
            {
                if (enabled) {
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (onClick != null) {
                        onClick()
                    } else {
                        onCheckedChange(!finalIsChecked)
                    }
                } else if (onDisabledClick != null) {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onDisabledClick()
                }
            }
        } else {
            null
        }

    if (showToggle) {
        if (onClick != null) {
            ListItem(
                onClick = {
                    if (enabled) {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onClick()
                    } else if (onDisabledClick != null) {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onDisabledClick()
                    }
                },
                enabled = enabled,
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                leadingContent =
                    if (finalIconRes != 0) {
                        {
                            Icon(
                                painter = painterResource(id = finalIconRes),
                                contentDescription = title,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        null
                    },
                supportingContent =
                    if (finalDescription != null) {
                        {
                            Text(
                                text = finalDescription,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        null
                    },
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        VerticalDivider(
                            modifier =
                                Modifier
                                    .height(32.dp)
                                    .width(1.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Switch(
                            checked = if (enabled) finalIsChecked else false,
                            onCheckedChange = {
                                if (enabled) {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    onCheckedChange(it)
                                }
                            },
                            enabled = enabled,
                        )
                    }
                },
                colors =
                    ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                    ),
                content = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
            )
        } else {
            ListItem(
                onClick = onClickAction ?: {},
                enabled = enabled,
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                leadingContent =
                    if (finalIconRes != 0) {
                        {
                            Icon(
                                painter = painterResource(id = finalIconRes),
                                contentDescription = title,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        null
                    },
                supportingContent =
                    if (finalDescription != null) {
                        {
                            Text(
                                text = finalDescription,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        null
                    },
                trailingContent = {
                    Switch(
                        checked = if (enabled) finalIsChecked else false,
                        onCheckedChange = null,
                        enabled = enabled,
                    )
                },
                colors =
                    ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                    ),
                content = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
            )
        }
    } else {
        ListItem(
            onClick = onClickAction ?: {},
            enabled = onClickAction != null && enabled,
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            leadingContent =
                if (finalIconRes != 0) {
                    {
                        Icon(
                            painter = painterResource(id = finalIconRes),
                            contentDescription = title,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    null
                },
            supportingContent =
                if (finalDescription != null) {
                    {
                        Text(
                            text = finalDescription,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    null
                },
            trailingContent = trailingIcon,
            colors =
                ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                ),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
        )
    }
}
