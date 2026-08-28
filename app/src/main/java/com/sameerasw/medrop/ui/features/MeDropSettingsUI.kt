package com.sameerasw.medrop.ui.features

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sameerasw.medrop.R
import com.sameerasw.medrop.domain.model.MeDropContact
import com.sameerasw.medrop.domain.model.MeDropProfileType
import com.sameerasw.medrop.domain.model.MeDropSettings
import com.sameerasw.medrop.ui.core.cards.IconToggleItem
import com.sameerasw.medrop.ui.core.containers.RoundedCardContainer
import com.sameerasw.medrop.ui.core.pickers.SegmentedPicker
import com.sameerasw.medrop.utils.HapticUtil
import com.sameerasw.medrop.utils.MeDropContactPickerHelper
import com.sameerasw.medrop.viewmodels.MeDropViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeDropSettingsUI(
    viewModel: MeDropViewModel,
    headerHeight: Dp = 200.dp,
    selectedTab: MeDropProfileType = MeDropProfileType.CONTACT,
    onTabSelected: (MeDropProfileType) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings by viewModel.meDropSettings
    val safeSettings = settings ?: MeDropSettings()
    val contact = safeSettings.contact

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
                    .fillMaxSize()
                    .clip(CircleShape)
                    .clickable {
                        HapticUtil.performVirtualKeyHaptic(view)
                        photoPickerLauncher.launch("image/*")
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
        }

        val profileTabs = listOf(MeDropProfileType.CONTACT, MeDropProfileType.PROFESSIONAL, MeDropProfileType.CUSTOM)
        RoundedCardContainer {
            SegmentedPicker(
                items = profileTabs,
                selectedItem = selectedTab,
                onItemSelected = onTabSelected,
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
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (contact == null) {
            RoundedCardContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_contacts_product_24),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.feat_medrop_no_contact),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.feat_medrop_no_contact_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            when (selectedTab) {
                MeDropProfileType.CONTACT -> {
                    ContactTabContent(
                        contact = contact,
                        settings = safeSettings,
                        viewModel = viewModel,
                        onPickPhoto = {
                            photoPickerLauncher.launch("image/*")
                        },
                    )
                }
                MeDropProfileType.PROFESSIONAL -> {
                    NonDefaultProfileTabContent(
                        type = MeDropProfileType.PROFESSIONAL,
                        contact = contact,
                        settings = safeSettings,
                        viewModel = viewModel,
                        onPickPhoto = {
                            photoPickerLauncher.launch("image/*")
                        },
                    )
                }
                MeDropProfileType.CUSTOM -> {
                    NonDefaultProfileTabContent(
                        type = MeDropProfileType.CUSTOM,
                        contact = contact,
                        settings = safeSettings,
                        viewModel = viewModel,
                        onPickPhoto = {
                            photoPickerLauncher.launch("image/*")
                        },
                    )
                }
            }
        }

        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_lock_24,
                title = stringResource(R.string.feat_medrop_allow_when_locked),
                description = stringResource(R.string.feat_medrop_allow_when_locked_desc),
                isChecked = safeSettings.allowWhenLocked,
                onCheckedChange = { viewModel.setMeDropAllowWhenLocked(context, it) },
            )
        }
    }
}

@Composable
private fun ContactTabContent(
    contact: MeDropContact,
    settings: MeDropSettings,
    viewModel: MeDropViewModel,
    onPickPhoto: () -> Unit,
) {
    val context = LocalContext.current

    Text(
        text = stringResource(R.string.feat_medrop_section_photo),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 8.dp),
    )
    RoundedCardContainer {
        IconToggleItem(
            iconRes = R.drawable.rounded_contacts_product_24,
            title = stringResource(R.string.feat_medrop_include_photo),
            isChecked = settings.isEntrySelected(MeDropProfileType.CONTACT, "photo"),
            onCheckedChange = {
                viewModel.toggleMeDropProfileEntry(context, MeDropProfileType.CONTACT, "photo", it)
            },
        )
        IconToggleItem(
            iconRes = R.drawable.rounded_share_24,
            title = stringResource(R.string.feat_medrop_use_photo_for_all),
            description = stringResource(R.string.feat_medrop_use_photo_for_all_desc),
            isChecked = settings.usePhotoForAll,
            onCheckedChange = {
                viewModel.setMeDropUsePhotoForAll(context, it)
            },
        )
        IconToggleItem(
            iconRes = R.drawable.rounded_add_photo_alternate_24,
            title = stringResource(R.string.feat_medrop_choose_custom_photo),
            showToggle = false,
            onClick = onPickPhoto,
        )
        val photoUri = settings.contactProfile.photoUri
        if (!photoUri.isNullOrBlank()) {
            IconToggleItem(
                iconRes = R.drawable.rounded_delete_24,
                title = stringResource(R.string.feat_medrop_remove_custom_photo),
                showToggle = false,
                onClick = {
                    viewModel.updateMeDropProfilePhoto(context, MeDropProfileType.CONTACT, null)
                },
            )
        }
    }

    Text(
        text = stringResource(R.string.feat_medrop_section_fields),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 8.dp),
    )
    ProfileFieldsList(
        type = MeDropProfileType.CONTACT,
        contact = contact,
        settings = settings,
        viewModel = viewModel,
    )
}

@Composable
private fun NonDefaultProfileTabContent(
    type: MeDropProfileType,
    contact: MeDropContact,
    settings: MeDropSettings,
    viewModel: MeDropViewModel,
    onPickPhoto: () -> Unit,
) {
    val context = LocalContext.current
    val profile = settings.getProfile(type)

    RoundedCardContainer {
        IconToggleItem(
            iconRes = R.drawable.rounded_person_24,
            title = stringResource(R.string.feat_medrop_profile_enabled),
            description = stringResource(R.string.feat_medrop_profile_enabled_desc),
            isChecked = profile.enabled,
            onCheckedChange = {
                viewModel.setMeDropProfileEnabled(context, type, it)
            },
        )
    }

    AnimatedVisibility(visible = profile.enabled) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!settings.usePhotoForAll) {
                Text(
                    text = stringResource(R.string.feat_medrop_section_photo),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp),
                )
                RoundedCardContainer {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_contacts_product_24,
                        title = stringResource(R.string.feat_medrop_include_photo),
                        isChecked = settings.isEntrySelected(type, "photo"),
                        onCheckedChange = {
                            viewModel.toggleMeDropProfileEntry(context, type, "photo", it)
                        },
                    )
                    IconToggleItem(
                        iconRes = R.drawable.rounded_add_photo_alternate_24,
                        title = stringResource(R.string.feat_medrop_choose_custom_photo),
                        showToggle = false,
                        onClick = onPickPhoto,
                    )
                    if (!profile.photoUri.isNullOrBlank()) {
                        IconToggleItem(
                            iconRes = R.drawable.rounded_delete_24,
                            title = stringResource(R.string.feat_medrop_remove_custom_photo),
                            showToggle = false,
                            onClick = {
                                viewModel.updateMeDropProfilePhoto(context, type, null)
                            },
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.feat_medrop_section_fields),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 8.dp),
            )
            ProfileFieldsList(
                type = type,
                contact = contact,
                settings = settings,
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
                    viewModel.toggleMeDropProfileEntry(context, type, id, it)
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
