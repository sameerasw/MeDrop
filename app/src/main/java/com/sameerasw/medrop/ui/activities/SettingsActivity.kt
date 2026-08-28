package com.sameerasw.medrop.ui.activities

import android.Manifest
import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.medrop.R
import com.sameerasw.medrop.ui.components.MeDropFloatingToolbar
import com.sameerasw.medrop.ui.core.cards.FeatureCard
import com.sameerasw.medrop.ui.core.cards.IconToggleItem
import com.sameerasw.medrop.ui.core.containers.RoundedCardContainer
import com.sameerasw.medrop.ui.core.sheets.PermissionItem
import com.sameerasw.medrop.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.medrop.ui.modifiers.BlurDirection
import com.sameerasw.medrop.ui.modifiers.progressiveBlur
import com.sameerasw.medrop.ui.theme.MeDropTheme
import com.sameerasw.medrop.utils.MeDropContactPickerHelper
import com.sameerasw.medrop.utils.PermissionUtils
import com.sameerasw.medrop.viewmodels.MeDropViewModel
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
            navigationBarStyle =
                SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val isDarkMode =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        window.setBackgroundDrawableResource(if (isDarkMode) android.R.color.black else R.color.app_window_background)

        setContent {
            val context = LocalContext.current
            val viewModel: MeDropViewModel = viewModel()
            val scope = rememberCoroutineScope()

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer =
                    LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.check(context)
                        }
                    }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(Unit) {
                viewModel.check(context)
            }

            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            val isBlurEnabled by viewModel.isBlurEnabled
            val isAllowWhenLocked by viewModel.isMeDropAllowWhenLocked
            val hasContactsPerm by viewModel.hasContactsPermission
            val settings by viewModel.meDropSettings
            val currentContact = settings?.contact
            val density = LocalDensity.current

            var showPermissionsSheet by remember { mutableStateOf(false) }

            val contactPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        scope.launch {
                            val pickedContact = MeDropContactPickerHelper.processResult(uri, context)
                            viewModel.setMeDropContact(context, pickedContact)
                        }
                    }
                }
            }

            val requestPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                viewModel.hasContactsPermission.value = isGranted
                if (isGranted) {
                    showPermissionsSheet = false
                    contactPickerLauncher.launch(MeDropContactPickerHelper.buildPickIntent())
                }
            }

            val onPickContactClick = {
                if (PermissionUtils.hasContactsPermission(context)) {
                    contactPickerLauncher.launch(MeDropContactPickerHelper.buildPickIntent())
                } else {
                    showPermissionsSheet = true
                }
            }

            MeDropTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) { _ ->
                    val statusBarHeightPx =
                        with(density) {
                            WindowInsets.statusBars
                                .asPaddingValues()
                                .calculateTopPadding()
                                .toPx()
                        }

                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .progressiveBlur(
                                    blurRadius = if (isBlurEnabled) 40f else 0f,
                                    height = statusBarHeightPx * 1.15f,
                                    direction = BlurDirection.TOP,
                                ),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .progressiveBlur(
                                        blurRadius = if (isBlurEnabled) 40f else 0f,
                                        height = with(density) { 150.dp.toPx() },
                                        direction = BlurDirection.BOTTOM,
                                    )
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Spacer(
                                modifier =
                                    Modifier.height(
                                        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                                    ),
                            )

                            // Contact Source Card
                            FeatureCard(
                                title = if (currentContact != null) {
                                    stringResource(R.string.feat_medrop_change_contact)
                                } else {
                                    stringResource(R.string.feat_medrop_select_contact)
                                },
                                description = currentContact?.displayName ?: stringResource(R.string.feat_medrop_no_contact_desc),
                                iconRes = R.drawable.rounded_contacts_product_24,
                                onClick = onPickContactClick,
                            )

                            Text(
                                text = stringResource(R.string.settings_section_general),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp),
                            )

                            RoundedCardContainer {
                                IconToggleItem(
                                    iconRes = R.drawable.rounded_lock_24,
                                    title = stringResource(R.string.feat_medrop_allow_when_locked),
                                    description = stringResource(R.string.feat_medrop_allow_when_locked_desc),
                                    isChecked = isAllowWhenLocked,
                                    onCheckedChange = { viewModel.setMeDropAllowWhenLocked(context, it) },
                                )
                            }

                            Text(
                                text = stringResource(R.string.settings_section_appearance),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp),
                            )

                            RoundedCardContainer {
                                IconToggleItem(
                                    iconRes = R.drawable.rounded_palette_24,
                                    title = stringResource(R.string.setting_pitch_black_theme_title),
                                    description = stringResource(R.string.setting_pitch_black_theme_desc),
                                    isChecked = isPitchBlackThemeEnabled,
                                    onCheckedChange = { viewModel.setPitchBlackTheme(context, it) },
                                )
                                IconToggleItem(
                                    iconRes = R.drawable.rounded_blur_on_24,
                                    title = stringResource(R.string.label_use_blur),
                                    description = stringResource(R.string.desc_use_blur),
                                    isChecked = isBlurEnabled,
                                    onCheckedChange = { viewModel.setBlurEnabled(context, it) },
                                )
                            }

                            Text(
                                text = stringResource(R.string.about_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp),
                            )

                            RoundedCardContainer {
                                IconToggleItem(
                                    iconRes = R.drawable.rounded_info_24,
                                    title = stringResource(R.string.app_name),
                                    description = stringResource(R.string.about_app_desc),
                                    showToggle = false,
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        WindowInsets.navigationBars
                                            .asPaddingValues()
                                            .calculateBottomPadding() + 150.dp,
                                    ),
                            )
                        }

                        MeDropFloatingToolbar(
                            title = stringResource(R.string.settings_title),
                            onBackClick = { finish() },
                            modifier =
                                Modifier
                                    .align(androidx.compose.ui.Alignment.BottomCenter)
                                    .zIndex(1f),
                        )
                    }

                    if (showPermissionsSheet) {
                        val permItems = listOf(
                            PermissionItem(
                                iconRes = R.drawable.rounded_contacts_product_24,
                                title = stringResource(R.string.perm_contacts_title),
                                description = stringResource(R.string.perm_contacts_desc),
                                isGranted = hasContactsPerm,
                                actionLabel = stringResource(R.string.perm_action_grant),
                                action = {
                                    requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                            )
                        )

                        PermissionsBottomSheet(
                            onDismissRequest = { showPermissionsSheet = false },
                            featureTitle = stringResource(R.string.feat_medrop_title),
                            permissions = permItems
                        )
                    }
                }
            }
        }
    }
}
