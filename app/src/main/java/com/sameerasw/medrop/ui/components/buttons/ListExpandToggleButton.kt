package com.sameerasw.medrop.ui.components.buttons

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.medrop.R
import com.sameerasw.medrop.utils.HapticUtil

@Composable
fun ListExpandToggleButton(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    expandedText: String = "",
    collapsedText: String = "",
    expandedIconRes: Int? = null,
    collapsedIconRes: Int? = null,
    iconRes: Int? = null,
) {
    val view = LocalView.current

    val rotationDegree by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "list_expand_chevron_rotation",
    )

    val currentIconRes = when {
        isExpanded && expandedIconRes != null -> expandedIconRes
        !isExpanded && collapsedIconRes != null -> collapsedIconRes
        iconRes != null -> iconRes
        else -> R.drawable.rounded_keyboard_arrow_down_24
    }

    val shouldRotate = iconRes == null && expandedIconRes == null && collapsedIconRes == null

    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        Button(
            onClick = {
                HapticUtil.performVirtualKeyHaptic(view)
                onToggle()
            },
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Icon(
                painter = painterResource(id = currentIconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier
                        .size(22.dp)
                        .then(
                            if (shouldRotate) {
                                Modifier.graphicsLayer { rotationZ = rotationDegree }
                            } else {
                                Modifier
                            },
                        ),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isExpanded) expandedText else collapsedText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
