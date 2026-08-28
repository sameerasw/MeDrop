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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import com.sameerasw.medrop.services.MeDropHceService
import com.sameerasw.medrop.ui.effects.NfcRippleEffect
import com.sameerasw.medrop.ui.modifiers.BlurDirection
import com.sameerasw.medrop.ui.modifiers.progressiveBlur
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
import androidx.graphics.shapes.RoundedPolygon
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
import com.sameerasw.medrop.ui.core.sheets.EditFieldBottomSheet
import com.sameerasw.medrop.ui.core.sheets.FieldInputType
import com.sameerasw.medrop.utils.HapticUtil
import com.sameerasw.medrop.utils.MeDropContactPickerHelper
import com.sameerasw.medrop.viewmodels.MeDropViewModel
import kotlinx.coroutines.launch

private data class ProfileFieldItem(
    val id: String,
    val iconRes: Int,
    val title: String,
    val subtitle: String? = null,
    val inputType: FieldInputType = FieldInputType.TEXT,
    val defaultValue: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MeDropHeaderUI(
    viewModel: MeDropViewModel,
    headerHeight: Dp = 200.dp,
    activeProfileType: MeDropProfileType = MeDropProfileType.CONTACT,
    onPickContactClick: () -> Unit = {},
    entranceProgress: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings by viewModel.meDropSettings
    val safeSettings = settings ?: MeDropSettings()
    val contact = safeSettings.contact

    var isPhotoMenuExpanded by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }

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

    val initialPolygon = MaterialShapes.Circle
    val previousPolygon = remember { mutableStateOf(initialPolygon) }
    val currentPolygon = remember { mutableStateOf(targetPolygon) }
    val morphProgress = remember { Animatable(0f) }

    LaunchedEffect(targetPolygon) {
        if (targetPolygon != currentPolygon.value) {
            previousPolygon.value = currentPolygon.value
            currentPolygon.value = targetPolygon
            morphProgress.snapTo(0f)
            morphProgress.animateTo(1f, animationSpec = tween(500, easing = LinearOutSlowInEasing))
        } else if (morphProgress.value < 1f) {
            kotlinx.coroutines.delay(250)
            morphProgress.animateTo(
                1f,
                animationSpec = tween(
                    durationMillis = 1200,
                    easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.0f, 0f, 1.0f)
                )
            )
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val statusBarTop = androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerCenterY = statusBarTop + 4.dp + 8.dp + ((headerHeight - 16.dp) / 2f)
    val globalEntranceOffsetY = with(density) { ((screenHeightDp / 2f) - headerCenterY).toPx() }
    val currentGlobalOffsetY = (1f - entranceProgress) * globalEntranceOffsetY
    val currentPhotoScale = 0.84f + (0.16f * entranceProgress)
    val textAlpha = entranceProgress.coerceIn(0f, 1f)

    val minHeaderHeight = 200.dp
    val maxAllowedHeight = minOf(screenWidthDp, screenHeightDp * 0.6f).coerceAtLeast(minHeaderHeight)
    val expansionFraction = if (maxAllowedHeight > minHeaderHeight) {
        ((headerHeight - minHeaderHeight) / (maxAllowedHeight - minHeaderHeight)).coerceIn(0f, 1f)
    } else 0f

    // Morph to square in the final 85%+ threshold
    val morphToSquareFraction = if (expansionFraction > 0.85f) {
        ((expansionFraction - 0.85f) / 0.15f).coerceIn(0f, 1f)
    } else {
        0f
    }

    val baseMorph = remember(previousPolygon.value, currentPolygon.value) {
        Morph(previousPolygon.value, currentPolygon.value)
    }

    val sharpSquarePolygon = remember {
        RoundedPolygon(
            numVertices = 4,
            centerX = 0.5f,
            centerY = 0.5f,
        )
    }

    val squareMorph = remember(currentPolygon.value, sharpSquarePolygon) {
        Morph(currentPolygon.value, sharpSquarePolygon)
    }

    val animatedShape = remember(baseMorph, squareMorph, morphProgress.value, morphToSquareFraction) {
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val matrix = Matrix().apply {
                    postScale(size.width, size.height)
                }
                val androidPath = if (morphToSquareFraction > 0f) {
                    squareMorph.toPath(morphToSquareFraction)
                } else {
                    baseMorph.toPath(morphProgress.value)
                }
                androidPath.transform(matrix)
                return Outline.Generic(androidPath.asComposePath())
            }
        }
    }

    val normalAvatarSize = minHeaderHeight - 16.dp
    val targetExpandedWidth = screenWidthDp
    val currentWidth = normalAvatarSize + (targetExpandedWidth - normalAvatarSize) * expansionFraction
    val currentHeight = headerHeight
    val horizontalPadding = (16.dp * (1f - expansionFraction)).coerceAtLeast(0.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .offset { androidx.compose.ui.unit.IntOffset(0, currentGlobalOffsetY.toInt()) }
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .padding(vertical = (8.dp * (1f - expansionFraction)).coerceAtLeast(0.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = currentWidth, height = currentHeight)
                    .graphicsLayer {
                        scaleX = currentPhotoScale
                        scaleY = currentPhotoScale
                    }
                    .clip(animatedShape)
                    .clipToBounds()
                    .clickable {
                        HapticUtil.performVirtualKeyHaptic(view)
                        isPhotoMenuExpanded = true
                    },
                contentAlignment = Alignment.Center,
            ) {
                val blurBottomPx = with(density) { 180.dp.toPx() }
                val imageBlurModifier = if (morphToSquareFraction > 0.01f) {
                    Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .progressiveBlur(
                            blurRadius = 45f * morphToSquareFraction,
                            height = blurBottomPx,
                            direction = BlurDirection.BOTTOM,
                        )
                } else {
                    Modifier.fillMaxSize()
                }

                if (!currentPhotoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = currentPhotoUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = imageBlurModifier,
                    )
                } else if (contact != null) {
                    Box(
                        modifier = imageBlurModifier
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = safeSettings.getEffectiveDisplayName(activeProfileType).take(1).uppercase(),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Box(
                        modifier = imageBlurModifier
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_contacts_product_24),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Smooth fade gradient to surfaceContainer overlaying above the blurred image
                if (morphToSquareFraction > 0.01f) {
                    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .align(Alignment.BottomCenter)
                            .clipToBounds()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        surfaceColor.copy(alpha = morphToSquareFraction)
                                    )
                                )
                            )
                    )
                }
            }

            // Edit button at bottom-right of photo area
            Box(
                modifier = Modifier
                    .size(headerHeight - 16.dp)
                    .graphicsLayer { alpha = textAlpha },
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = textAlpha },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (contact != null) {
                val displayName = safeSettings.getEffectiveDisplayName(activeProfileType)
                val canEditName = activeProfileType != MeDropProfileType.CONTACT

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = canEditName) {
                            HapticUtil.performVirtualKeyHaptic(view)
                            isEditingName = true
                        }
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = displayName,
                        modifier = Modifier.basicMarquee(),
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
                }

                val hasAnySubName = !contact.nickname.isNullOrBlank() || !contact.pronouns.isNullOrBlank()
                if (hasAnySubName) {
                    val effNickname = safeSettings.getEffectiveFieldValue(activeProfileType, "nickname", contact.nickname)
                    val effPronouns = safeSettings.getEffectiveFieldValue(activeProfileType, "pronouns", contact.pronouns)
                    val showNickname = safeSettings.isEntrySelected(activeProfileType, "nickname") && !effNickname.isNullOrBlank()
                    val showPronouns = safeSettings.isEntrySelected(activeProfileType, "pronouns") && !effPronouns.isNullOrBlank()
                    val subName = if (showNickname || showPronouns) {
                        val nickPart = if (showNickname) "\"$effNickname\"" else null
                        val pronounPart = if (showPronouns) "($effPronouns)" else null
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

                if (isEditingName) {
                    EditFieldBottomSheet(
                        title = stringResource(R.string.feat_medrop_edit_name_title),
                        initialValue = safeSettings.getProfile(activeProfileType).customDisplayName ?: contact.displayName,
                        defaultValue = contact.displayName,
                        iconRes = R.drawable.rounded_contacts_product_24,
                        inputType = FieldInputType.PERSON_NAME,
                        onSave = { newName ->
                            viewModel.updateMeDropProfileDisplayName(context, activeProfileType, newName)
                        },
                        onResetToDefault = {
                            viewModel.updateMeDropProfileDisplayName(context, activeProfileType, null)
                        },
                        onDismissRequest = { isEditingName = false }
                    )
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
    var activeEditingField by remember { mutableStateOf<ProfileFieldItem?>(null) }

    val allFields = remember(contact, safeSettings, profileType) {
        val list = mutableListOf<ProfileFieldItem>()
        
        // Nickname
        val effNickname = safeSettings.getEffectiveFieldValue(profileType, "nickname", contact?.nickname) ?: ""
        list.add(
            ProfileFieldItem(
                id = "nickname",
                iconRes = R.drawable.rounded_app_registration_24,
                title = effNickname.ifBlank { context.getString(R.string.feat_medrop_field_nickname) },
                subtitle = context.getString(R.string.feat_medrop_field_nickname),
                inputType = FieldInputType.TEXT,
                defaultValue = contact?.nickname,
            )
        )

        // Pronouns
        val effPronouns = safeSettings.getEffectiveFieldValue(profileType, "pronouns", contact?.pronouns) ?: ""
        list.add(
            ProfileFieldItem(
                id = "pronouns",
                iconRes = R.drawable.rounded_heart_smile_24,
                title = effPronouns.ifBlank { context.getString(R.string.feat_medrop_field_pronouns) },
                subtitle = context.getString(R.string.feat_medrop_field_pronouns),
                inputType = FieldInputType.TEXT,
                defaultValue = contact?.pronouns,
            )
        )

        // Birthday
        val effBirthday = safeSettings.getEffectiveFieldValue(profileType, "birthday", contact?.birthday) ?: ""
        list.add(
            ProfileFieldItem(
                id = "birthday",
                iconRes = R.drawable.rounded_calendar_today_24,
                title = effBirthday.ifBlank { context.getString(R.string.feat_medrop_field_birthday) },
                subtitle = context.getString(R.string.feat_medrop_field_birthday),
                inputType = FieldInputType.DATE,
                defaultValue = contact?.birthday,
            )
        )

        // Phones
        val phones = contact?.getSafePhones() ?: emptyList()
        if (phones.isNotEmpty()) {
            phones.forEachIndexed { i, phone ->
                val eff = safeSettings.getEffectiveFieldValue(profileType, "phone_$i", phone) ?: ""
                list.add(
                    ProfileFieldItem(
                        id = "phone_$i",
                        iconRes = R.drawable.rounded_call_log_24,
                        title = eff.ifBlank { context.getString(R.string.feat_medrop_field_phone) },
                        subtitle = if (phones.size > 1) "${context.getString(R.string.feat_medrop_field_phone)} ${i + 1}" else context.getString(R.string.feat_medrop_field_phone),
                        inputType = FieldInputType.PHONE,
                        defaultValue = phone,
                    )
                )
            }
        } else {
            val eff = safeSettings.getEffectiveFieldValue(profileType, "phone_0", "") ?: ""
            list.add(
                ProfileFieldItem(
                    id = "phone_0",
                    iconRes = R.drawable.rounded_call_log_24,
                    title = eff.ifBlank { context.getString(R.string.feat_medrop_field_phone) },
                    subtitle = context.getString(R.string.feat_medrop_field_phone),
                    inputType = FieldInputType.PHONE,
                    defaultValue = null,
                )
            )
        }

        // Emails
        val emails = contact?.getSafeEmails() ?: emptyList()
        if (emails.isNotEmpty()) {
            emails.forEachIndexed { i, email ->
                val eff = safeSettings.getEffectiveFieldValue(profileType, "email_$i", email) ?: ""
                list.add(
                    ProfileFieldItem(
                        id = "email_$i",
                        iconRes = R.drawable.rounded_mail_24,
                        title = eff.ifBlank { context.getString(R.string.feat_medrop_field_email) },
                        subtitle = if (emails.size > 1) "${context.getString(R.string.feat_medrop_field_email)} ${i + 1}" else context.getString(R.string.feat_medrop_field_email),
                        inputType = FieldInputType.EMAIL,
                        defaultValue = email,
                    )
                )
            }
        } else {
            val eff = safeSettings.getEffectiveFieldValue(profileType, "email_0", "") ?: ""
            list.add(
                ProfileFieldItem(
                    id = "email_0",
                    iconRes = R.drawable.rounded_mail_24,
                    title = eff.ifBlank { context.getString(R.string.feat_medrop_field_email) },
                    subtitle = context.getString(R.string.feat_medrop_field_email),
                    inputType = FieldInputType.EMAIL,
                    defaultValue = null,
                )
            )
        }

        // Organization
        val effOrg = safeSettings.getEffectiveFieldValue(profileType, "organization", contact?.organization) ?: ""
        list.add(
            ProfileFieldItem(
                id = "organization",
                iconRes = R.drawable.rounded_work_24,
                title = effOrg.ifBlank { context.getString(R.string.feat_medrop_field_organization) },
                subtitle = context.getString(R.string.feat_medrop_field_organization),
                inputType = FieldInputType.TEXT,
                defaultValue = contact?.organization,
            )
        )

        // Department
        val effDept = safeSettings.getEffectiveFieldValue(profileType, "department", contact?.department) ?: ""
        list.add(
            ProfileFieldItem(
                id = "department",
                iconRes = R.drawable.rounded_work_24,
                title = effDept.ifBlank { context.getString(R.string.feat_medrop_field_department) },
                subtitle = context.getString(R.string.feat_medrop_field_department),
                inputType = FieldInputType.TEXT,
                defaultValue = contact?.department,
            )
        )

        // Job Title
        val effTitle = safeSettings.getEffectiveFieldValue(profileType, "jobTitle", contact?.jobTitle) ?: ""
        list.add(
            ProfileFieldItem(
                id = "jobTitle",
                iconRes = R.drawable.rounded_work_24,
                title = effTitle.ifBlank { context.getString(R.string.feat_medrop_field_job_title) },
                subtitle = context.getString(R.string.feat_medrop_field_job_title),
                inputType = FieldInputType.TEXT,
                defaultValue = contact?.jobTitle,
            )
        )

        // Role
        val effRole = safeSettings.getEffectiveFieldValue(profileType, "role", contact?.role) ?: ""
        list.add(
            ProfileFieldItem(
                id = "role",
                iconRes = R.drawable.rounded_work_24,
                title = effRole.ifBlank { context.getString(R.string.feat_medrop_field_role) },
                subtitle = context.getString(R.string.feat_medrop_field_role),
                inputType = FieldInputType.TEXT,
                defaultValue = contact?.role,
            )
        )

        // Addresses
        val addrs = contact?.getSafeAddresses() ?: emptyList()
        if (addrs.isNotEmpty()) {
            addrs.forEachIndexed { i, addr ->
                val addrType = contact?.getSafeAddressTypes()?.getOrNull(i)
                val tag = if (addrType == 2) context.getString(R.string.feat_medrop_address_work) else context.getString(R.string.feat_medrop_address_home)
                val eff = safeSettings.getEffectiveFieldValue(profileType, "address_$i", addr) ?: ""
                list.add(
                    ProfileFieldItem(
                        id = "address_$i",
                        iconRes = R.drawable.rounded_location_on_24,
                        title = eff.replace("\n", ", ").ifBlank { context.getString(R.string.feat_medrop_field_address) },
                        subtitle = tag,
                        inputType = FieldInputType.MULTILINE,
                        defaultValue = addr,
                    )
                )
            }
        } else {
            val eff = safeSettings.getEffectiveFieldValue(profileType, "address_0", "") ?: ""
            list.add(
                ProfileFieldItem(
                    id = "address_0",
                    iconRes = R.drawable.rounded_location_on_24,
                    title = eff.replace("\n", ", ").ifBlank { context.getString(R.string.feat_medrop_field_address) },
                    subtitle = context.getString(R.string.feat_medrop_field_address),
                    inputType = FieldInputType.MULTILINE,
                    defaultValue = null,
                )
            )
        }

        // URLs
        val urls = contact?.getSafeUrls() ?: emptyList()
        if (urls.isNotEmpty()) {
            urls.forEachIndexed { i, url ->
                val eff = safeSettings.getEffectiveFieldValue(profileType, "url_$i", url) ?: ""
                list.add(
                    ProfileFieldItem(
                        id = "url_$i",
                        iconRes = R.drawable.rounded_globe_24,
                        title = eff.ifBlank { context.getString(R.string.feat_medrop_field_url) },
                        subtitle = if (urls.size > 1) "${context.getString(R.string.feat_medrop_field_url)} ${i + 1}" else context.getString(R.string.feat_medrop_field_url),
                        inputType = FieldInputType.URL,
                        defaultValue = url,
                    )
                )
            }
        } else {
            val eff = safeSettings.getEffectiveFieldValue(profileType, "url_0", "") ?: ""
            list.add(
                ProfileFieldItem(
                    id = "url_0",
                    iconRes = R.drawable.rounded_globe_24,
                    title = eff.ifBlank { context.getString(R.string.feat_medrop_field_url) },
                    subtitle = context.getString(R.string.feat_medrop_field_url),
                    inputType = FieldInputType.URL,
                    defaultValue = null,
                )
            )
        }

        // Note
        val effNote = safeSettings.getEffectiveFieldValue(profileType, "note", contact?.note) ?: ""
        list.add(
            ProfileFieldItem(
                id = "note",
                iconRes = R.drawable.rounded_info_24,
                title = effNote.ifBlank { context.getString(R.string.feat_medrop_field_note) },
                subtitle = context.getString(R.string.feat_medrop_field_note),
                inputType = FieldInputType.MULTILINE,
                defaultValue = contact?.note,
            )
        )

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
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            if (isEditVisibilityMode) {
                                viewModel.toggleMeDropProfileEntry(context, profileType, item.id, false)
                            } else {
                                activeEditingField = item
                            }
                        },
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

        // List Expand Toggle Button for Edit/Save Visibility (stays in place right below visible fields)
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

        // Hidden Fields Section (expands below the toggle button in Edit mode)
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

        // Empty area inside the page item to ensure full-width swipe surface down below
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
    }

    val editing = activeEditingField
    if (editing != null) {
        val currentVal = safeSettings.getEffectiveFieldValue(profileType, editing.id, editing.defaultValue) ?: ""
        EditFieldBottomSheet(
            title = stringResource(R.string.feat_medrop_edit_field_title, editing.subtitle ?: editing.id),
            initialValue = currentVal,
            defaultValue = editing.defaultValue,
            iconRes = editing.iconRes,
            inputType = editing.inputType,
            onSave = { newValue ->
                viewModel.updateMeDropProfileFieldValue(context, profileType, editing.id, newValue)
            },
            onResetToDefault = {
                viewModel.updateMeDropProfileFieldValue(context, profileType, editing.id, null)
            },
            onDismissRequest = { activeEditingField = null }
        )
    }
}
