package com.sameerasw.medrop.ui.features

import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import coil.compose.AsyncImage
import com.sameerasw.medrop.R
import com.sameerasw.medrop.domain.model.MeDropContact
import com.sameerasw.medrop.domain.model.MeDropProfileType
import com.sameerasw.medrop.domain.model.MeDropSettings
import com.sameerasw.medrop.ui.core.cards.FeatureCard
import com.sameerasw.medrop.ui.core.cards.IconToggleItem
import com.sameerasw.medrop.ui.core.containers.RoundedCardContainer
import com.sameerasw.medrop.ui.core.menus.SegmentedDropdownMenu
import com.sameerasw.medrop.ui.core.menus.SegmentedDropdownMenuItem
import com.sameerasw.medrop.utils.HapticUtil
import com.sameerasw.medrop.utils.MeDropContactPickerHelper
import com.sameerasw.medrop.viewmodels.MeDropViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MeDropSettingsUI(
    viewModel: MeDropViewModel,
    headerHeight: Dp = 200.dp,
    selectedTab: MeDropProfileType = MeDropProfileType.CONTACT,
    onPickContactClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings by viewModel.meDropSettings
    val safeSettings = settings ?: MeDropSettings()
    val contact = safeSettings.contact

    var isPhotoMenuExpanded by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val savedUri = MeDropContactPickerHelper.saveCompressedCustomPhoto(uri, context, selectedTab)
                if (savedUri != null) {
                    viewModel.updateMeDropProfilePhoto(context, selectedTab, savedUri)
                }
            }
        }
    }

    val currentPhotoUri = safeSettings.getEffectivePhotoUri(selectedTab)

    val targetPolygon = when (selectedTab) {
        MeDropProfileType.CONTACT -> MaterialShapes.Cookie12Sided
        MeDropProfileType.PROFESSIONAL -> MaterialShapes.Pill
        MeDropProfileType.CUSTOM -> MaterialShapes.Cookie4Sided
    }

    val previousPolygon = remember { mutableStateOf(targetPolygon) }
    val currentPolygon = remember { mutableStateOf(targetPolygon) }
    val morphProgress = remember { Animatable(1f) }

    LaunchedEffect(targetPolygon) {
        if (targetPolygon != currentPolygon.value) {
            previousPolygon.value = currentPolygon.value
            currentPolygon.value = targetPolygon
            morphProgress.snapTo(0f)
            morphProgress.animateTo(1f, animationSpec = tween(400, easing = LinearOutSlowInEasing))
        }
    }

    val morph = remember(previousPolygon.value, currentPolygon.value) {
        Morph(previousPolygon.value, currentPolygon.value)
    }

    val animatedShape = remember(morph, morphProgress.value) {
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val matrix = Matrix().apply {
                    postScale(size.width, size.height)
                }
                val androidPath = morph.toPath(morphProgress.value)
                androidPath.transform(matrix)
                return Outline.Generic(androidPath.asComposePath())
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(headerHeight - 16.dp)
                    .clip(animatedShape)
                    .clickable {
                        HapticUtil.performVirtualKeyHaptic(view)
                        isPhotoMenuExpanded = true
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (!currentPhotoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = currentPhotoUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (contact != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = contact.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_add_photo_alternate_24),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Edit button at bottom-right of photo area
            Box(
                modifier = Modifier
                    .size(headerHeight - 16.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                IconButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        isPhotoMenuExpanded = true
                    },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier
                        .size(44.dp)
                        .offset(x = (-4).dp, y = (-4).dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_edit_24),
                        contentDescription = stringResource(R.string.feat_medrop_choose_custom_photo),
                        modifier = Modifier.size(22.dp),
                    )
                }

                SegmentedDropdownMenu(
                    expanded = isPhotoMenuExpanded,
                    onDismissRequest = { isPhotoMenuExpanded = false },
                ) {
                    SegmentedDropdownMenuItem(
                        text = { Text(stringResource(R.string.feat_medrop_choose_custom_photo)) },
                        onClick = {
                            isPhotoMenuExpanded = false
                            HapticUtil.performVirtualKeyHaptic(view)
                            photoPickerLauncher.launch("image/*")
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.rounded_add_photo_alternate_24),
                                contentDescription = null,
                            )
                        },
                    )

                    val customPhotoOnProfile = safeSettings.getProfile(selectedTab).photoUri
                    if (!customPhotoOnProfile.isNullOrBlank() || (!currentPhotoUri.isNullOrBlank() && selectedTab == MeDropProfileType.CONTACT)) {
                        SegmentedDropdownMenuItem(
                            text = { Text(stringResource(R.string.feat_medrop_remove_custom_photo)) },
                            onClick = {
                                isPhotoMenuExpanded = false
                                HapticUtil.performVirtualKeyHaptic(view)
                                viewModel.updateMeDropProfilePhoto(context, selectedTab, null)
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_delete_24),
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                }
            }
        }

        // Contact Header
        if (contact != null) {
            Text(
                text = contact.displayName,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .basicMarquee(),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = FontFamily(
                        Font(
                            R.font.google_sans_flex,
                            variationSettings = FontVariation.Settings(
                                FontVariation.width(150f),
                                FontVariation.weight(FontWeight.Normal.weight),
                                FontVariation.Setting("ROND", 100f),
                            ),
                        ),
                    ),
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )

            val showNickname = safeSettings.isEntrySelected(selectedTab, "nickname") && !contact.nickname.isNullOrBlank()
            val showPronouns = safeSettings.isEntrySelected(selectedTab, "pronouns") && !contact.pronouns.isNullOrBlank()
            if (showNickname || showPronouns) {
                val nickPart = if (showNickname) "\"${contact.nickname}\"" else null
                val pronounPart = if (showPronouns) "(${contact.pronouns})" else null
                val subName = listOfNotNull(nickPart, pronounPart).joinToString(" ")
                Text(
                    text = subName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.basicMarquee(),
                )
            }
        }

        if (contact == null) {
            FeatureCard(
                title = stringResource(R.string.feat_medrop_select_contact),
                description = stringResource(R.string.feat_medrop_no_contact_desc),
                iconRes = R.drawable.rounded_contacts_product_24,
                onClick = onPickContactClick,
            )
        } else {
            Text(
                text = stringResource(R.string.feat_medrop_section_fields),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 8.dp),
            )
            ProfileFieldsList(
                type = selectedTab,
                contact = contact,
                settings = safeSettings,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun ProfileFieldsList(
    type: MeDropProfileType,
    contact: MeDropContact,
    settings: MeDropSettings,
    viewModel: MeDropViewModel,
) {
    val context = LocalContext.current

    RoundedCardContainer {
        if (!contact.nickname.isNullOrBlank()) {
            val id = "nickname"
            IconToggleItem(
                iconRes = R.drawable.rounded_app_registration_24,
                title = contact.nickname,
                subtitle = stringResource(R.string.feat_medrop_field_nickname),
                isChecked = settings.isEntrySelected(type, id),
                onCheckedChange = {
                    viewModel.toggleMeDropProfileEntry(context, type, id, it)
                },
            )
        }
        if (!contact.pronouns.isNullOrBlank()) {
            val id = "pronouns"
            IconToggleItem(
                iconRes = R.drawable.rounded_heart_smile_24,
                title = contact.pronouns,
                subtitle = stringResource(R.string.feat_medrop_field_pronouns),
                isChecked = settings.isEntrySelected(type, id),
                onCheckedChange = {
                    viewModel.toggleMeDropProfileEntry(context, type, id, it)
                },
            )
        }
        if (!contact.birthday.isNullOrBlank()) {
            val id = "birthday"
            IconToggleItem(
                iconRes = R.drawable.rounded_calendar_today_24,
                title = contact.birthday,
                subtitle = stringResource(R.string.feat_medrop_field_birthday),
                isChecked = settings.isEntrySelected(type, id),
                onCheckedChange = {
                    viewModel.toggleMeDropProfileEntry(context, type, id, it)
                },
            )
        }
        contact.getSafePhones().forEachIndexed { i, phone ->
            val id = "phone_$i"
            IconToggleItem(
                iconRes = R.drawable.rounded_call_log_24,
                title = phone,
                isChecked = settings.isEntrySelected(type, id),
                onCheckedChange = {
                    viewModel.toggleMeDropProfileEntry(context, type, id, it)
                },
            )
        }
        contact.getSafeEmails().forEachIndexed { i, email ->
            val id = "email_$i"
            IconToggleItem(
                iconRes = R.drawable.rounded_mail_24,
                title = email,
                isChecked = settings.isEntrySelected(type, id),
                onCheckedChange = {
                    viewModel.toggleMeDropProfileEntry(context, type, id, id == "email_0")
                },
            )
        }
        if (!contact.organization.isNullOrBlank()) {
            val id = "organization"
            IconToggleItem(
                iconRes = R.drawable.rounded_work_24,
                title = contact.organization,
                subtitle = stringResource(R.string.feat_medrop_field_organization),
                isChecked = settings.isEntrySelected(type, id),
                onCheckedChange = {
                    viewModel.toggleMeDropProfileEntry(context, type, id, it)
                },
            )
        }
        if (!contact.department.isNullOrBlank()) {
            val id = "department"
            IconToggleItem(
                iconRes = R.drawable.rounded_work_24,
                title = contact.department,
                subtitle = stringResource(R.string.feat_medrop_field_department),
                isChecked = settings.isEntrySelected(type, id),
                onCheckedChange = {
                    viewModel.toggleMeDropProfileEntry(context, type, id, it)
                },
            )
        }
        if (!contact.jobTitle.isNullOrBlank()) {
            val id = "jobTitle"
            IconToggleItem(
                iconRes = R.drawable.rounded_work_24,
                title = contact.jobTitle,
                subtitle = stringResource(R.string.feat_medrop_field_job_title),
                isChecked = settings.isEntrySelected(type, id),
                onCheckedChange = {
                    viewModel.toggleMeDropProfileEntry(context, type, id, it)
                },
            )
        }
        if (!contact.role.isNullOrBlank()) {
            val id = "role"
            IconToggleItem(
                iconRes = R.drawable.rounded_work_24,
                title = contact.role,
                subtitle = stringResource(R.string.feat_medrop_field_role),
                isChecked = settings.isEntrySelected(type, id),
                onCheckedChange = {
                    viewModel.toggleMeDropProfileEntry(context, type, id, it)
                },
            )
        }
        contact.getSafeAddresses().forEachIndexed { i, addr ->
            val id = "address_$i"
            val addrType = contact.getSafeAddressTypes().getOrNull(i)
            val tag = if (addrType == 2) stringResource(R.string.feat_medrop_address_work) else stringResource(R.string.feat_medrop_address_home)
            IconToggleItem(
                iconRes = R.drawable.rounded_location_on_24,
                title = addr.replace("\n", ", "),
                subtitle = tag,
                isChecked = settings.isEntrySelected(type, id),
                onCheckedChange = {
                    viewModel.toggleMeDropProfileEntry(context, type, id, it)
                },
            )
        }
        contact.getSafeUrls().forEachIndexed { i, url ->
            val id = "url_$i"
            IconToggleItem(
                iconRes = R.drawable.rounded_globe_24,
                title = url,
                isChecked = settings.isEntrySelected(type, id),
                onCheckedChange = {
                    viewModel.toggleMeDropProfileEntry(context, type, id, it)
                },
            )
        }
        if (!contact.note.isNullOrBlank()) {
            val id = "note"
            IconToggleItem(
                iconRes = R.drawable.rounded_info_24,
                title = contact.note,
                isChecked = settings.isEntrySelected(type, id),
                onCheckedChange = {
                    viewModel.toggleMeDropProfileEntry(context, type, id, it)
                },
            )
        }
    }
}
