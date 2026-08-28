package com.sameerasw.medrop.ui.sheets

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.sameerasw.medrop.MainActivity
import com.sameerasw.medrop.R
import com.sameerasw.medrop.domain.model.MeDropProfileType
import com.sameerasw.medrop.domain.model.MeDropSettings
import com.sameerasw.medrop.ui.core.cards.IconToggleItem
import com.sameerasw.medrop.ui.core.containers.RoundedCardContainer
import com.sameerasw.medrop.ui.core.pickers.SegmentedPicker
import com.sameerasw.medrop.ui.core.sheets.MeDropBottomSheetContainer
import com.sameerasw.medrop.ui.theme.Shapes
import com.sameerasw.medrop.utils.HapticUtil
import com.sameerasw.medrop.utils.MeDropNfcManager
import com.sameerasw.medrop.utils.QrCodeGenerator
import com.sameerasw.medrop.viewmodels.MeDropViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MeDropBottomSheet(
    viewModel: MeDropViewModel,
    onDismissRequest: () -> Unit,
) {
    val settings by viewModel.meDropSettings
    val context = LocalContext.current
    val view = LocalView.current

    LaunchedEffect(Unit) {
        viewModel.loadMeDropSettings(context)
    }

    val safeSettings = settings ?: MeDropSettings()
    val contact = safeSettings.contact
    val activeProfileType = safeSettings.activeProfileType

    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(activity, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadMeDropSettings(context)
            } else if (event == Lifecycle.Event.ON_STOP) {
                if (activity != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        MeDropNfcManager.stopBroadcast(activity)
                    }
                }
                onDismissRequest()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (activity != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    MeDropNfcManager.stopBroadcast(activity)
                }
            }
        }
    }

    LaunchedEffect(safeSettings, activity) {
        if (contact != null && activity != null) {
            MeDropNfcManager.startBroadcast(activity, safeSettings)
        }
    }

    LaunchedEffect(contact) {
        if (contact != null) {
            while (true) {
                delay(500L)
                withContext(Dispatchers.Default) {
                    HapticUtil.performLightHaptic(view)
                }
            }
        }
    }

    val availableProfiles = remember(safeSettings) {
        val list = mutableListOf(MeDropProfileType.CONTACT)
        if (safeSettings.professionalProfile.enabled) {
            list.add(MeDropProfileType.PROFESSIONAL)
        }
        if (safeSettings.customProfile.enabled) {
            list.add(MeDropProfileType.CUSTOM)
        }
        list
    }

    var isQrModeActive by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val window = activity?.window
    val originalBrightness = remember(window) {
        window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }

    val targetBrightness = if (isQrModeActive) 1f else originalBrightness
    val animatedBrightness by animateFloatAsState(
        targetValue = if (targetBrightness < 0f) 0.5f else targetBrightness,
        animationSpec = tween(400, easing = LinearOutSlowInEasing),
        label = "screen_brightness"
    )

    LaunchedEffect(isQrModeActive, animatedBrightness) {
        window?.let { win ->
            val lp = win.attributes
            if (isQrModeActive) {
                lp.screenBrightness = animatedBrightness
                win.attributes = lp
            } else {
                lp.screenBrightness = originalBrightness
                win.attributes = lp
            }
        }
    }

    DisposableEffect(window) {
        onDispose {
            window?.let { win ->
                val lp = win.attributes
                lp.screenBrightness = originalBrightness
                win.attributes = lp
            }
        }
    }

    LaunchedEffect(isQrModeActive, safeSettings, activeProfileType, contact) {
        if (isQrModeActive && contact != null) {
            withContext(Dispatchers.IO) {
                val vcardString = contact.toVCard(
                    context = context,
                    activeEntryIds = safeSettings.getEffectiveEntryIds(activeProfileType),
                    customPhotoUri = null
                )
                val generated = QrCodeGenerator.generateQrBitmap(
                    content = vcardString,
                    size = 1000,
                    foregroundColor = AndroidColor.BLACK,
                    backgroundColor = AndroidColor.WHITE,
                    logo = null
                )
                withContext(Dispatchers.Main) {
                    qrBitmap = generated
                }
            }
        }
    }

    MeDropBottomSheetContainer(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (contact != null) {
                val effectivePhoto = safeSettings.getEffectivePhotoUri(activeProfileType)

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

                val qrContainerSize by animateDpAsState(
                    targetValue = if (isQrModeActive) 340.dp else 200.dp,
                    animationSpec = tween(400, easing = LinearOutSlowInEasing),
                    label = "qr_container_size"
                )
                val photoAvatarSize by animateDpAsState(
                    targetValue = if (isQrModeActive) 80.dp else 200.dp,
                    animationSpec = tween(400, easing = LinearOutSlowInEasing),
                    label = "photo_avatar_size"
                )
                val qrAlpha by animateFloatAsState(
                    targetValue = if (isQrModeActive) 1f else 0f,
                    animationSpec = tween(350, easing = LinearOutSlowInEasing),
                    label = "qr_alpha"
                )

                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .size(qrContainerSize)
                        .clip(Shapes.large)
                        .background(if (isQrModeActive) MaterialTheme.colorScheme.surfaceBright else Color.Transparent)
                        .padding(if (isQrModeActive) 12.dp else 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isQrModeActive) {
                        val bmp = qrBitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = stringResource(R.string.feat_medrop_action_qr),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(Shapes.medium)
                                    .alpha(qrAlpha)
                            )
                        } else {
                            LoadingIndicator()
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(photoAvatarSize)
                            .then(
                                if (isQrModeActive) {
                                    Modifier
                                        .background(Color.White, CircleShape)
                                        .padding(4.dp)
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!effectivePhoto.isNullOrBlank()) {
                            AsyncImage(
                                model = effectivePhoto,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(animatedShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(animatedShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contact.displayName.take(1).uppercase(),
                                    style = if (isQrModeActive) MaterialTheme.typography.titleLarge else MaterialTheme.typography.displayLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = contact.displayName,
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
                        textAlign = TextAlign.Center
                    )

                    val showNickname = safeSettings.isEntrySelected(activeProfileType, "nickname") && !contact.nickname.isNullOrBlank()
                    val showPronouns = safeSettings.isEntrySelected(activeProfileType, "pronouns") && !contact.pronouns.isNullOrBlank()
                    if (showNickname || showPronouns) {
                        val nickPart = if (showNickname) "\"${contact.nickname}\"" else null
                        val pronounPart = if (showPronouns) "(${contact.pronouns})" else null
                        val subName = listOfNotNull(nickPart, pronounPart).joinToString(" ")
                        Text(
                            text = subName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }

                if (availableProfiles.size > 1) {
                    RoundedCardContainer {
                        SegmentedPicker(
                            items = availableProfiles,
                            selectedItem = if (availableProfiles.contains(activeProfileType)) activeProfileType else MeDropProfileType.CONTACT,
                            onItemSelected = { selectedType ->
                                viewModel.setMeDropActiveProfile(context, selectedType)
                            },
                            labelProvider = { type ->
                                when (type) {
                                    MeDropProfileType.CONTACT -> context.getString(R.string.feat_medrop_profile_contact)
                                    MeDropProfileType.PROFESSIONAL -> context.getString(R.string.feat_medrop_profile_professional)
                                    MeDropProfileType.CUSTOM -> context.getString(R.string.feat_medrop_profile_custom)
                                }
                            },
                            iconProvider = { type ->
                                val iconRes = when (type) {
                                    MeDropProfileType.CONTACT -> R.drawable.rounded_contacts_product_24
                                    MeDropProfileType.PROFESSIONAL -> R.drawable.rounded_work_24
                                    MeDropProfileType.CUSTOM -> R.drawable.rounded_id_card_24
                                }
                                Icon(
                                    painter = painterResource(iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                RoundedCardContainer {
                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceBright)
                            .padding(16.dp)
                            .fillMaxWidth()
                            .animateContentSize(animationSpec = tween(300, easing = LinearOutSlowInEasing)),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val showOrg = safeSettings.isEntrySelected(activeProfileType, "organization") && !contact.organization.isNullOrBlank()
                        val showDept = safeSettings.isEntrySelected(activeProfileType, "department") && !contact.department.isNullOrBlank()
                        val showTitle = safeSettings.isEntrySelected(activeProfileType, "jobTitle") && !contact.jobTitle.isNullOrBlank()
                        val showRole = safeSettings.isEntrySelected(activeProfileType, "role") && !contact.role.isNullOrBlank()
                        if (showOrg || showDept || showTitle || showRole) {
                            val roleOrTitle = listOfNotNull(
                                if (showTitle) contact.jobTitle else null,
                                if (showRole) contact.role else null
                            ).filter { it.isNotBlank() }.joinToString(", ")
                            val orgOrDept = listOfNotNull(
                                if (showOrg) contact.organization else null,
                                if (showDept) contact.department else null
                            ).filter { it.isNotBlank() }.joinToString(" - ")
                            val orgText = listOfNotNull(
                                roleOrTitle.ifBlank { null },
                                orgOrDept.ifBlank { null }
                            ).joinToString(" • ")
                            if (orgText.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_work_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = orgText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (safeSettings.isEntrySelected(activeProfileType, "birthday") && !contact.birthday.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_calendar_today_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = contact.birthday,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            contact.getSafePhones().forEachIndexed { i, phone ->
                                if (safeSettings.isEntrySelected(activeProfileType, "phone_$i")) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(R.drawable.rounded_call_log_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = phone,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            contact.getSafeEmails().forEachIndexed { i, email ->
                                if (safeSettings.isEntrySelected(activeProfileType, "email_$i")) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(R.drawable.rounded_mail_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = email,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            contact.getSafeAddresses().forEachIndexed { i, addr ->
                                if (safeSettings.isEntrySelected(activeProfileType, "address_$i")) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(R.drawable.rounded_location_on_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = addr.replace("\n", ", "),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            contact.getSafeUrls().forEachIndexed { i, url ->
                                if (safeSettings.isEntrySelected(activeProfileType, "url_$i")) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(R.drawable.rounded_globe_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = url,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (safeSettings.isEntrySelected(activeProfileType, "note") && !contact.note.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_info_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = contact.note,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = activeProfileType == MeDropProfileType.CUSTOM) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.feat_medrop_share_fields_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                        )
                        RoundedCardContainer {
                            if (!contact.nickname.isNullOrBlank()) {
                                val id = "nickname"
                                IconToggleItem(
                                    iconRes = R.drawable.rounded_app_registration_24,
                                    title = contact.nickname,
                                    subtitle = stringResource(R.string.feat_medrop_field_nickname),
                                    isChecked = safeSettings.isEntrySelected(MeDropProfileType.CUSTOM, id),
                                    onCheckedChange = {
                                        viewModel.toggleMeDropProfileEntry(context, MeDropProfileType.CUSTOM, id, it)
                                    }
                                )
                            }
                            if (!contact.pronouns.isNullOrBlank()) {
                                val id = "pronouns"
                                IconToggleItem(
                                    iconRes = R.drawable.rounded_heart_smile_24,
                                    title = contact.pronouns,
                                    subtitle = stringResource(R.string.feat_medrop_field_pronouns),
                                    isChecked = safeSettings.isEntrySelected(MeDropProfileType.CUSTOM, id),
                                    onCheckedChange = {
                                        viewModel.toggleMeDropProfileEntry(context, MeDropProfileType.CUSTOM, id, it)
                                    }
                                )
                            }
                            if (!contact.birthday.isNullOrBlank()) {
                                val id = "birthday"
                                IconToggleItem(
                                    iconRes = R.drawable.rounded_calendar_today_24,
                                    title = contact.birthday,
                                    subtitle = stringResource(R.string.feat_medrop_field_birthday),
                                    isChecked = safeSettings.isEntrySelected(MeDropProfileType.CUSTOM, id),
                                    onCheckedChange = {
                                        viewModel.toggleMeDropProfileEntry(context, MeDropProfileType.CUSTOM, id, it)
                                    }
                                )
                            }
                            contact.getSafePhones().forEachIndexed { i, phone ->
                                val id = "phone_$i"
                                IconToggleItem(
                                    iconRes = R.drawable.rounded_call_log_24,
                                    title = phone,
                                    isChecked = safeSettings.isEntrySelected(MeDropProfileType.CUSTOM, id),
                                    onCheckedChange = {
                                        viewModel.toggleMeDropProfileEntry(context, MeDropProfileType.CUSTOM, id, it)
                                    }
                                )
                            }
                            contact.getSafeEmails().forEachIndexed { i, email ->
                                val id = "email_$i"
                                IconToggleItem(
                                    iconRes = R.drawable.rounded_mail_24,
                                    title = email,
                                    isChecked = safeSettings.isEntrySelected(MeDropProfileType.CUSTOM, id),
                                    onCheckedChange = {
                                        viewModel.toggleMeDropProfileEntry(context, MeDropProfileType.CUSTOM, id, it)
                                    }
                                )
                            }
                            if (!contact.organization.isNullOrBlank()) {
                                val id = "organization"
                                IconToggleItem(
                                    iconRes = R.drawable.rounded_work_24,
                                    title = contact.organization,
                                    subtitle = stringResource(R.string.feat_medrop_field_organization),
                                    isChecked = safeSettings.isEntrySelected(MeDropProfileType.CUSTOM, id),
                                    onCheckedChange = {
                                        viewModel.toggleMeDropProfileEntry(context, MeDropProfileType.CUSTOM, id, it)
                                    }
                                )
                            }
                            if (!contact.department.isNullOrBlank()) {
                                val id = "department"
                                IconToggleItem(
                                    iconRes = R.drawable.rounded_work_24,
                                    title = contact.department,
                                    subtitle = stringResource(R.string.feat_medrop_field_department),
                                    isChecked = safeSettings.isEntrySelected(MeDropProfileType.CUSTOM, id),
                                    onCheckedChange = {
                                        viewModel.toggleMeDropProfileEntry(context, MeDropProfileType.CUSTOM, id, it)
                                    }
                                )
                            }
                            if (!contact.jobTitle.isNullOrBlank()) {
                                val id = "jobTitle"
                                IconToggleItem(
                                    iconRes = R.drawable.rounded_work_24,
                                    title = contact.jobTitle,
                                    subtitle = stringResource(R.string.feat_medrop_field_job_title),
                                    isChecked = safeSettings.isEntrySelected(MeDropProfileType.CUSTOM, id),
                                    onCheckedChange = {
                                        viewModel.toggleMeDropProfileEntry(context, MeDropProfileType.CUSTOM, id, it)
                                    }
                                )
                            }
                            if (!contact.role.isNullOrBlank()) {
                                val id = "role"
                                IconToggleItem(
                                    iconRes = R.drawable.rounded_work_24,
                                    title = contact.role,
                                    subtitle = stringResource(R.string.feat_medrop_field_role),
                                    isChecked = safeSettings.isEntrySelected(MeDropProfileType.CUSTOM, id),
                                    onCheckedChange = {
                                        viewModel.toggleMeDropProfileEntry(context, MeDropProfileType.CUSTOM, id, it)
                                    }
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
                                    isChecked = safeSettings.isEntrySelected(MeDropProfileType.CUSTOM, id),
                                    onCheckedChange = {
                                        viewModel.toggleMeDropProfileEntry(context, MeDropProfileType.CUSTOM, id, it)
                                    }
                                )
                            }
                            contact.getSafeUrls().forEachIndexed { i, url ->
                                val id = "url_$i"
                                IconToggleItem(
                                    iconRes = R.drawable.rounded_globe_24,
                                    title = url,
                                    isChecked = safeSettings.isEntrySelected(MeDropProfileType.CUSTOM, id),
                                    onCheckedChange = {
                                        viewModel.toggleMeDropProfileEntry(context, MeDropProfileType.CUSTOM, id, it)
                                    }
                                )
                            }
                            if (!contact.note.isNullOrBlank()) {
                                val id = "note"
                                IconToggleItem(
                                    iconRes = R.drawable.rounded_info_24,
                                    title = contact.note,
                                    isChecked = safeSettings.isEntrySelected(MeDropProfileType.CUSTOM, id),
                                    onCheckedChange = {
                                        viewModel.toggleMeDropProfileEntry(context, MeDropProfileType.CUSTOM, id, it)
                                    }
                                )
                            }
                        }
                    }
                }

                NfcBroadcastIndicator()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    val shapesLeading = ButtonGroupDefaults.connectedLeadingButtonShapes()
                    val shapesMiddle = ButtonGroupDefaults.connectedMiddleButtonShapes()
                    val shapesTrailing = ButtonGroupDefaults.connectedTrailingButtonShapes()

                    Button(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            val vcardString = contact.toVCard(
                                context = context,
                                activeEntryIds = safeSettings.getEffectiveEntryIds(activeProfileType),
                                customPhotoUri = safeSettings.getEffectivePhotoUri(activeProfileType)
                            )
                            try {
                                val cleanName = contact.displayName.replace(Regex("[^a-zA-Z0-9.-]"), "_").ifBlank { "contact" }
                                val vcardFile = File(context.cacheDir, "$cleanName.vcf").apply {
                                    writeText(vcardString)
                                }
                                val contentUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    vcardFile
                                )
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/x-vcard"
                                    putExtra(Intent.EXTRA_STREAM, contentUri)
                                    putExtra(Intent.EXTRA_SUBJECT, contact.displayName)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.feat_medrop_action_share))
                                context.startActivity(shareIntent)
                            } catch (_: Exception) {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/x-vcard"
                                    putExtra(Intent.EXTRA_TEXT, vcardString)
                                    putExtra(Intent.EXTRA_SUBJECT, contact.displayName)
                                }
                                val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.feat_medrop_action_share))
                                context.startActivity(shareIntent)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = shapesLeading.shape,
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_share_24),
                            contentDescription = stringResource(R.string.feat_medrop_action_share),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Button(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            isQrModeActive = !isQrModeActive
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = shapesMiddle.shape,
                        colors = if (isQrModeActive) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_qr_code_24),
                            contentDescription = stringResource(R.string.feat_medrop_action_qr),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Button(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onDismissRequest()
                            val intent = Intent(context, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = shapesTrailing.shape,
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_settings_24),
                            contentDescription = stringResource(R.string.feat_medrop_title),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_contacts_product_24),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.feat_medrop_no_contact),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.feat_medrop_no_contact_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onDismissRequest()
                            val intent = Intent(context, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        shape = Shapes.medium
                    ) {
                        Text(text = stringResource(R.string.feat_medrop_set_up))
                    }
                }
            }
        }
    }
}

@Composable
private fun NfcBroadcastIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.rounded_contactless_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.feat_medrop_hold_near),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
