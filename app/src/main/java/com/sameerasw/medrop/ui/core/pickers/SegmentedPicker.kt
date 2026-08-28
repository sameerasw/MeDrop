package com.sameerasw.medrop.ui.core.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.medrop.utils.HapticUtil

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> SegmentedPicker(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    iconProvider: (@Composable (T) -> Unit)? = null,
    textStyleProvider: ((T) -> androidx.compose.ui.text.TextStyle)? = null,
    modifier: Modifier = Modifier,
    cornerShape: CornerSize = MaterialTheme.shapes.extraSmall.bottomEnd,
    containerColor: Color = MaterialTheme.colorScheme.surfaceBright,
    contentPadding: PaddingValues = PaddingValues(10.dp),
) {
    val view = LocalView.current

    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(cornerShape))
                .background(color = containerColor)
                .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        val modifiers = List(items.size) { Modifier.weight(1f) }

        items.forEachIndexed { index, item ->
            val label = labelProvider(item)

            Box(modifier = modifiers[index]) {
                ToggleButton(
                    checked = selectedItem == item,
                    onCheckedChange = {
                        HapticUtil.performUIHaptic(view)
                        onItemSelected(item)
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics { role = Role.RadioButton },
                    shapes =
                        when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            items.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (iconProvider != null) {
                            iconProvider(item)
                        }
                        if (label.isNotEmpty()) {
                            if (iconProvider != null) {
                                Spacer(Modifier.padding(end = 8.dp))
                            }
                            val customStyle = textStyleProvider?.invoke(item) ?: androidx.compose.ui.text.TextStyle.Default
                            Text(
                                label,
                                fontSize = 12.sp,
                                style = customStyle,
                                modifier = Modifier.basicMarquee(),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
