package com.sameerasw.medrop.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.medrop.R
import com.sameerasw.medrop.ui.core.containers.RoundedCardContainer

data class InstructionSection(
    val title: String,
    val iconRes: Int,
    val description: String? = null,
)

@Composable
fun HelpAndGuidesContent() {
    val sections =
        listOf(
            InstructionSection(
                title = stringResource(R.string.help_section_setup_title),
                iconRes = R.drawable.rounded_contacts_product_24,
                description = stringResource(R.string.help_section_setup_desc),
            ),
            InstructionSection(
                title = stringResource(R.string.help_section_nfc_title),
                iconRes = R.drawable.rounded_nfc_24,
                description = stringResource(R.string.help_section_nfc_desc),
            ),
            InstructionSection(
                title = stringResource(R.string.help_section_lockscreen_title),
                iconRes = R.drawable.rounded_lock_24,
                description = stringResource(R.string.help_section_lockscreen_desc),
            ),
            InstructionSection(
                title = stringResource(R.string.help_section_profiles_title),
                iconRes = R.drawable.rounded_id_card_24,
                description = stringResource(R.string.help_section_profiles_desc),
            ),
        )

    RoundedCardContainer {
        sections.forEach { section ->
            ExpandableGuideSection(section)
        }
    }
}

@Composable
fun ExpandableGuideSection(section: InstructionSection) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "arrow_rotation",
    )

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
                .clickable { expanded = !expanded },
        shape = RoundedCornerShape(2.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = if (expanded) MaterialTheme.colorScheme.surfaceBright else MaterialTheme.colorScheme.surfaceContainerLow,
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = section.iconRes),
                            contentDescription = null,
                            tint = if (expanded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )

                Icon(
                    painter = painterResource(id = R.drawable.rounded_keyboard_arrow_down_24),
                    contentDescription =
                        if (expanded) {
                            stringResource(R.string.action_collapse)
                        } else {
                            stringResource(
                                R.string.action_expand,
                            )
                        },
                    modifier = Modifier.rotate(rotation),
                )
            }

            // Content
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier =
                        Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (section.description != null) {
                        Text(
                            text = section.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(fraction = 0.95f),
                        )
                    }
                }
            }
        }
    }
}
