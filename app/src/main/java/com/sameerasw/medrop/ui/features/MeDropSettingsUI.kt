package com.sameerasw.medrop.ui.features

import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import com.sameerasw.medrop.ui.components.buttons.ListExpandToggleButton
import com.sameerasw.medrop.ui.core.cards.FeatureCard
import com.sameerasw.medrop.ui.core.cards.IconToggleItem
import com.sameerasw.medrop.ui.core.containers.RoundedCardContainer
import com.sameerasw.medrop.ui.core.menus.SegmentedDropdownMenu
import com.sameerasw.medrop.ui.core.menus.SegmentedDropdownMenuItem
import com.sameerasw.medrop.utils.HapticUtil
import com.sameerasw.medrop.utils.MeDropContactPickerHelper
import com.sameerasw.medrop.viewmodels.MeDropViewModel
import kotlinx.coroutines.launch

private data class ProfileFieldItem(
    val id: String,
    val iconRes: Int,
    val title: String,
    val subtitle: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MeDropHeaderUI(
    viewModel: MeDropViewModel,
    headerHeight: Dp = 200.dp,
    activeProfileType: MeDropProfileType = MeDropProfileType.CONTACT,
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
                val savedUri = MeDropContactPickerHelper.saveCompressedCustomPhoto(uri, context, activeProfileType)
                if (savedUri != null) {
                    viewModel.updateMeDropProfilePhoto(context, activeProfileType, savedUri)
                }
            }
        }
    }

    val currentPhotoUri = safeSettings.getEffectivePhotoUri(activeProfileType)

    val targetPolygon = when (activeProfileType) {
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

                    val customPhotoOnProfile = safeSettings.getProfile(activeProfileType).photoUri
                    if (!customPhotoOnProfile.isNullOrBlank() || (!currentPhotoUri.isNullOrBlank() && activeProfileType == MeDropProfileType.CONTACT)) {
                        SegmentedDropdownMenuItem(
                            text = { Text(stringResource(R.string.feat_medrop_remove_custom_photo)) },
                            onClick = {
                                isPhotoMenuExpanded = false
                                HapticUtil.performVirtualKeyHaptic(view)
                                viewModel.updateMeDropProfilePhoto(context, activeProfileType, null)
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

            val hasAnySubName = !contact.nickname.isNullOrBlank() || !contact.pronouns.isNullOrBlank()
            if (hasAnySubName) {
                val showNickname = safeSettings.isEntrySelected(activeProfileType, "nickname") && !contact.nickname.isNullOrBlank()
                val showPronouns = safeSettings.isEntrySelected(activeProfileType, "pronouns") && !contact.pronouns.isNullOrBlank()
                val subName = if (showNickname || showPronouns) {
                    val nickPart = if (showNickname) "\"${contact.nickname}\"" else null
                    val pronounPart = if (showPronouns) "(${contact.pronouns})" else null
                    listOfNotNull(nickPart, pronounPart).joinToString(" ")
                } else ""

                AnimatedContent(
                    targetState = subName,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)).togetherWith(fadeOut(animationSpec = tween(150)))
                    },
                    label = "subname_transition",
                ) { targetSubName ->
                    if (targetSubName.isNotBlank()) {
                        Text(
                            text = targetSubName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.basicMarquee(),
                        )
                    } else {
                        Spacer(modifier = Modifier.height(0.dp))
                    }
                }
            }
        } else {
            FeatureCard(
                title = stringResource(R.string.feat_medrop_select_contact),
                description = stringResource(R.string.feat_medrop_no_contact_desc),
                iconRes = R.drawable.rounded_contacts_product_24,
                onClick = onPickContactClick,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MeDropProfileFieldsUI(
    viewModel: MeDropViewModel,
    profileType: MeDropProfileType,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val settings by viewModel.meDropSettings
    val safeSettings = settings ?: MeDropSettings()
    val contact = safeSettings.contact ?: return

    var isEditVisibilityMode by remember { mutableStateOf(false) }

    val allFields = remember(contact) {
        val list = mutableListOf<ProfileFieldItem>()
        if (!contact.nickname.isNullOrBlank()) {
            list.add(
                ProfileFieldItem(
                    id = "nickname",
                    iconRes = R.drawable.rounded_app_registration_24,
                    title = contact.nickname,
                    subtitle = context.getString(R.string.feat_medrop_field_nickname),
                )
            )
        }
        if (!contact.pronouns.isNullOrBlank()) {
            list.add(
                ProfileFieldItem(
                    id = "pronouns",
                    iconRes = R.drawable.rounded_heart_smile_24,
                    title = contact.pronouns,
                    subtitle = context.getString(R.string.feat_medrop_field_pronouns),
                )
            )
        }
        if (!contact.birthday.isNullOrBlank()) {
            list.add(
                ProfileFieldItem(
                    id = "birthday",
                    iconRes = R.drawable.rounded_calendar_today_24,
                    title = contact.birthday,
                    subtitle = context.getString(R.string.feat_medrop_field_birthday),
                )
            )
        }
        contact.getSafePhones().forEachIndexed { i, phone ->
            list.add(
                ProfileFieldItem(
                    id = "phone_$i",
                    iconRes = R.drawable.rounded_call_log_24,
                    title = phone,
                )
            )
        }
        contact.getSafeEmails().forEachIndexed { i, email ->
            list.add(
                ProfileFieldItem(
                    id = "email_$i",
                    iconRes = R.drawable.rounded_mail_24,
                    title = email,
                )
            )
        }
        if (!contact.organization.isNullOrBlank()) {
            list.add(
                ProfileFieldItem(
                    id = "organization",
                    iconRes = R.drawable.rounded_work_24,
                    title = contact.organization,
                    subtitle = context.getString(R.string.feat_medrop_field_organization),
                )
            )
        }
        if (!contact.department.isNullOrBlank()) {
            list.add(
                ProfileFieldItem(
                    id = "department",
                    iconRes = R.drawable.rounded_work_24,
                    title = contact.department,
                    subtitle = context.getString(R.string.feat_medrop_field_department),
                )
            )
        }
        if (!contact.jobTitle.isNullOrBlank()) {
            list.add(
                ProfileFieldItem(
                    id = "jobTitle",
                    iconRes = R.drawable.rounded_work_24,
                    title = contact.jobTitle,
                    subtitle = context.getString(R.string.feat_medrop_field_job_title),
                )
            )
        }
        if (!contact.role.isNullOrBlank()) {
            list.add(
                ProfileFieldItem(
                    id = "role",
                    iconRes = R.drawable.rounded_work_24,
                    title = contact.role,
                    subtitle = context.getString(R.string.feat_medrop_field_role),
                )
            )
        }
        contact.getSafeAddresses().forEachIndexed { i, addr ->
            val addrType = contact.getSafeAddressTypes().getOrNull(i)
            val tag = if (addrType == 2) context.getString(R.string.feat_medrop_address_work) else context.getString(R.string.feat_medrop_address_home)
            list.add(
                ProfileFieldItem(
                    id = "address_$i",
                    iconRes = R.drawable.rounded_location_on_24,
                    title = addr.replace("\n", ", "),
                    subtitle = tag,
                )
            )
        }
        contact.getSafeUrls().forEachIndexed { i, url ->
            list.add(
                ProfileFieldItem(
                    id = "url_$i",
                    iconRes = R.drawable.rounded_globe_24,
                    title = url,
                )
            )
        }
        if (!contact.note.isNullOrBlank()) {
            list.add(
                ProfileFieldItem(
                    id = "note",
                    iconRes = R.drawable.rounded_info_24,
                    title = contact.note,
                )
            )
        }
        list
    }

    val visibleCount = allFields.count { safeSettings.isEntrySelected(profileType, it.id) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .animateContentSize(animationSpec = spring(stiffness = 500f)),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Visible Fields Section
        Text(
            text = if (isEditVisibilityMode) {
                stringResource(R.string.feat_medrop_section_visible_fields)
            } else {
                stringResource(R.string.feat_medrop_section_fields)
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 8.dp),
        )

        RoundedCardContainer(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = spring(stiffness = 500f))
        ) {
            allFields.forEach { item ->
                val isVisible = safeSettings.isEntrySelected(profileType, item.id)
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(250)) + expandVertically(animationSpec = spring(stiffness = 500f)),
                    exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = spring(stiffness = 500f)),
                ) {
                    IconToggleItem(
                        iconRes = item.iconRes,
                        title = item.title,
                        subtitle = item.subtitle,
                        showToggle = false,
                        onClick = if (isEditVisibilityMode) {
                            {
                                HapticUtil.performVirtualKeyHaptic(view)
                                viewModel.toggleMeDropProfileEntry(context, profileType, item.id, false)
                            }
                        } else null,
                        trailingIcon = if (isEditVisibilityMode) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_remove_24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        } else null,
                    )
                }
            }

            if (visibleCount == 0) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_info_24,
                    title = stringResource(R.string.feat_medrop_no_visible_fields),
                    showToggle = false,
                )
            }
        }

        // Hidden Fields Section (in Edit mode)
        AnimatedVisibility(
            visible = isEditVisibilityMode,
            enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = spring(stiffness = 500f)),
            exit = fadeOut(animationSpec = tween(250)) + shrinkVertically(animationSpec = spring(stiffness = 500f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = spring(stiffness = 500f)),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.feat_medrop_section_hidden_fields),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp),
                )

                RoundedCardContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = spring(stiffness = 500f))
                ) {
                    allFields.forEach { item ->
                        val isHidden = !safeSettings.isEntrySelected(profileType, item.id)
                        AnimatedVisibility(
                            visible = isHidden,
                            enter = fadeIn(animationSpec = tween(250)) + expandVertically(animationSpec = spring(stiffness = 500f)),
                            exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = spring(stiffness = 500f)),
                        ) {
                            IconToggleItem(
                                iconRes = item.iconRes,
                                title = item.title,
                                subtitle = item.subtitle,
                                showToggle = false,
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    viewModel.toggleMeDropProfileEntry(context, profileType, item.id, true)
                                },
                                trailingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_add_24),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        // List Expand Toggle Button for Edit/Save Visibility
        ListExpandToggleButton(
            isExpanded = isEditVisibilityMode,
            onToggle = {
                HapticUtil.performVirtualKeyHaptic(view)
                isEditVisibilityMode = !isEditVisibilityMode
            },
            expandedText = stringResource(R.string.feat_medrop_save_visibility),
            collapsedText = stringResource(R.string.feat_medrop_edit_visibility),
            collapsedIconRes = R.drawable.rounded_edit_24,
            expandedIconRes = R.drawable.rounded_check_24,
        )

        // Empty area inside the page item to ensure full-width swipe surface down below
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
    }
}
