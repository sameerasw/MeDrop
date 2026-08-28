package com.sameerasw.medrop.ui.core.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.medrop.R
import com.sameerasw.medrop.utils.HapticUtil

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PermissionCard(
    iconRes: Int,
    title: String,
    dependentFeatures: List<String> = emptyList(),
    actionLabel: String = stringResource(R.string.perm_action_grant),
    isGranted: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val grantedGreen = Color(0xFF4CAF50)
    val view = LocalView.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright,
            ),
    ) {
        Column(
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp, end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ListItem(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                colors =
                    ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                    ),
                leadingContent = {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = if (isGranted) grantedGreen else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                    )
                },
                supportingContent = {
                    Column {
                        if (description != null) {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (dependentFeatures.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.perm_required_for),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            dependentFeatures.forEach { f ->
                                Text(
                                    text = "• $f",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                },
                content = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                if (isGranted) {
                    OutlinedButton(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onActionClick()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(actionLabel)
                    }
                } else {
                    Button(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onActionClick()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}
