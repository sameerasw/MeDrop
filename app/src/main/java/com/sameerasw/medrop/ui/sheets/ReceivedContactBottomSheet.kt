package com.sameerasw.medrop.ui.sheets

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sameerasw.medrop.R
import com.sameerasw.medrop.ui.core.cards.IconToggleItem
import com.sameerasw.medrop.ui.core.containers.RoundedCardContainer
import com.sameerasw.medrop.ui.core.sheets.MeDropBottomSheetContainer
import com.sameerasw.medrop.utils.HapticUtil
import com.sameerasw.medrop.utils.ReceivedContact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivedContactBottomSheet(
    contact: ReceivedContact,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val photoBitmap = contact.getPhotoBitmap()

    val avatarScale = remember { Animatable(0.4f) }
    val contentAlpha = remember { Animatable(0f) }
    var sheetRippleTrigger by remember { mutableStateOf(0) }
    var avatarCenterOffset by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(Unit) {
        avatarScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(500, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f))
        )
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
        kotlinx.coroutines.delay(100)
        sheetRippleTrigger++
        val center = avatarCenterOffset
        val cx: Float
        val cy: Float
        if (center != null) {
            cx = center.x
            cy = center.y
        } else {
            val loc = IntArray(2)
            view.getLocationInWindow(loc)
            cx = loc[0] + (view.width / 2f)
            cy = loc[1] + (view.height / 3f)
        }
        com.sameerasw.medrop.ui.effects.NfcRippleEffect.triggerOnView(view, cx, cy)
    }

    MeDropBottomSheetContainer(
        onDismissRequest = onDismissRequest,
        rippleTrigger = sheetRippleTrigger,
        rippleOrigin = avatarCenterOffset ?: Offset.Zero
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Avatar & Name Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(avatarScale.value),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .onGloballyPositioned { coordinates ->
                            val pos = coordinates.positionInWindow()
                            val size = coordinates.size
                            avatarCenterOffset = androidx.compose.ui.geometry.Offset(
                                x = pos.x + (size.width / 2f),
                                y = pos.y + (size.height / 2f)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoBitmap != null) {
                        Image(
                            bitmap = photoBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contact.displayName.take(1).uppercase(),
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val subInfo = listOfNotNull(
                    contact.jobTitle,
                    contact.department,
                    contact.organization
                ).filter { it.isNotBlank() }.joinToString(" • ")

                if (subInfo.isNotBlank()) {
                    Text(
                        text = subInfo,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }

            val infoFields = mutableListOf<Triple<Int, String, String>>()

            if (!contact.nickname.isNullOrBlank()) {
                infoFields.add(Triple(R.drawable.rounded_person_24, stringResource(R.string.feat_medrop_field_nickname), contact.nickname))
            }
            if (!contact.pronouns.isNullOrBlank()) {
                infoFields.add(Triple(R.drawable.rounded_person_24, stringResource(R.string.feat_medrop_field_pronouns), contact.pronouns))
            }
            if (!contact.role.isNullOrBlank()) {
                infoFields.add(Triple(R.drawable.rounded_id_card_24, stringResource(R.string.feat_medrop_field_role), contact.role))
            }
            if (!contact.birthday.isNullOrBlank()) {
                infoFields.add(Triple(R.drawable.rounded_calendar_today_24, stringResource(R.string.feat_medrop_field_birthday), contact.birthday))
            }
            contact.phones.forEach {
                infoFields.add(Triple(R.drawable.rounded_call_log_24, stringResource(R.string.feat_medrop_field_phone), it))
            }
            contact.emails.forEach {
                infoFields.add(Triple(R.drawable.rounded_mail_24, stringResource(R.string.feat_medrop_field_email), it))
            }
            contact.addresses.forEach {
                infoFields.add(Triple(R.drawable.rounded_location_on_24, stringResource(R.string.feat_medrop_field_address), it))
            }
            contact.urls.forEach {
                infoFields.add(Triple(R.drawable.rounded_globe_24, stringResource(R.string.feat_medrop_field_url), it))
            }
            if (!contact.note.isNullOrBlank()) {
                infoFields.add(Triple(R.drawable.rounded_edit_24, stringResource(R.string.feat_medrop_field_note), contact.note))
            }

            if (infoFields.isNotEmpty()) {
                RoundedCardContainer {
                    infoFields.forEach { item ->
                        IconToggleItem(
                            iconRes = item.first,
                            title = item.third,
                            description = item.second,
                            showToggle = false,
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    HapticUtil.performHeavyHaptic(view)
                    contact.saveToSystemContacts(context)
                    onDismissRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_person_24),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.feat_medrop_save_contact),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
