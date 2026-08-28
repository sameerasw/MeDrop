package com.sameerasw.medrop.ui.core.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sameerasw.medrop.R
import com.sameerasw.medrop.utils.HapticUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class FieldInputType {
    TEXT,
    PHONE,
    EMAIL,
    URL,
    DATE,
    MULTILINE,
    PERSON_NAME,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFieldBottomSheet(
    title: String,
    initialValue: String,
    defaultValue: String? = null,
    iconRes: Int = 0,
    inputType: FieldInputType = FieldInputType.TEXT,
    onSave: (String) -> Unit,
    onResetToDefault: (() -> Unit)? = null,
    onDismissRequest: () -> Unit,
) {
    val view = LocalView.current

    if (inputType == FieldInputType.DATE) {
        val initialEpochMillis = remember(initialValue) {
            parseDateToEpochMillis(initialValue)
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialEpochMillis
        )

        DatePickerDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val formatted = formatDateFromEpochMillis(selectedMillis)
                            onSave(formatted)
                        }
                        onDismissRequest()
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Text(stringResource(R.string.feat_medrop_action_save))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onResetToDefault != null && defaultValue != null && initialValue != defaultValue) {
                        TextButton(
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                onResetToDefault()
                                onDismissRequest()
                            }
                        ) {
                            Text(stringResource(R.string.feat_medrop_action_reset_default))
                        }
                    }
                    TextButton(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(R.string.feat_medrop_action_cancel))
                    }
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
        return
    }

    var textValue by remember { mutableStateOf(initialValue) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val keyboardOptions = when (inputType) {
        FieldInputType.PHONE -> KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done)
        FieldInputType.EMAIL -> KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done)
        FieldInputType.URL -> KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done)
        FieldInputType.PERSON_NAME -> KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
        )
        FieldInputType.MULTILINE -> KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Default,
        )
        else -> KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
        )
    }

    MeDropBottomSheetContainer(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (iconRes != 0) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
            }

            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                shape = MaterialTheme.shapes.large,
                singleLine = inputType != FieldInputType.MULTILINE,
                minLines = if (inputType == FieldInputType.MULTILINE) 3 else 1,
                keyboardOptions = keyboardOptions,
                keyboardActions = KeyboardActions(
                    onDone = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onSave(textValue.trim())
                        onDismissRequest()
                    }
                ),
                placeholder = if (defaultValue != null) {
                    { Text(defaultValue) }
                } else null,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onResetToDefault != null && defaultValue != null && textValue != defaultValue) {
                    TextButton(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onResetToDefault()
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(R.string.feat_medrop_action_reset_default))
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(R.string.feat_medrop_action_cancel))
                    }

                    Button(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onSave(textValue.trim())
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.buttonColors(),
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Text(stringResource(R.string.feat_medrop_action_save))
                    }
                }
            }
        }
    }
}

private fun parseDateToEpochMillis(dateStr: String): Long? {
    if (dateStr.isBlank()) return null
    val formats = listOf("yyyy-MM-dd", "yyyy/MM/dd", "yyyyMMdd", "MM/dd/yyyy", "dd/MM/yyyy")
    for (pattern in formats) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val parsed = sdf.parse(dateStr)
            if (parsed != null) return parsed.time
        } catch (_: Exception) {}
    }
    return null
}

private fun formatDateFromEpochMillis(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return sdf.format(Date(millis))
}
