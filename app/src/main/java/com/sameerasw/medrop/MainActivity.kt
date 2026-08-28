package com.sameerasw.medrop

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.medrop.domain.model.MeDropProfileType
import com.sameerasw.medrop.ui.components.MeDropFloatingToolbar
import com.sameerasw.medrop.ui.components.ToolbarItem
import com.sameerasw.medrop.ui.features.MeDropSettingsUI
import com.sameerasw.medrop.ui.modifiers.BlurDirection
import com.sameerasw.medrop.ui.modifiers.progressiveBlur
import com.sameerasw.medrop.ui.theme.MeDropTheme
import com.sameerasw.medrop.utils.HapticUtil
import com.sameerasw.medrop.utils.MeDropContactPickerHelper
import com.sameerasw.medrop.viewmodels.MeDropViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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

            remember(context) { viewModel.check(context) }

            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            val isBlurEnabled by viewModel.isBlurEnabled

            val density = LocalDensity.current
            val minHeaderHeight = 200.dp
            val maxHeaderHeight = 400.dp
            var headerHeight by remember { mutableStateOf(minHeaderHeight) }

            val view = LocalView.current
            val scope = rememberCoroutineScope()

            var selectedTab by remember { mutableStateOf(MeDropProfileType.CONTACT) }

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

            val nestedScrollConnection =
                remember {
                    object : NestedScrollConnection {
                        override fun onPreScroll(
                            available: Offset,
                            source: NestedScrollSource,
                        ): Offset {
                            val delta = available.y
                            if (delta < 0 && headerHeight > minHeaderHeight) {
                                val oldHeight = headerHeight
                                headerHeight =
                                    with(density) {
                                        (oldHeight.toPx() + delta).toDp()
                                    }.coerceAtLeast(minHeaderHeight)
                                val consumed = oldHeight - headerHeight
                                return Offset(0f, with(density) { -consumed.toPx() })
                            }
                            return Offset.Zero
                        }

                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource,
                        ): Offset {
                            val delta = available.y
                            if (delta > 0) {
                                val oldHeight = headerHeight
                                headerHeight =
                                    with(density) {
                                        (oldHeight.toPx() + delta).toDp()
                                    }.coerceAtMost(maxHeaderHeight)

                                if (headerHeight == maxHeaderHeight && oldHeight < maxHeaderHeight) {
                                    HapticUtil.performLightHaptic(view)
                                }

                                val produced = headerHeight - oldHeight
                                return Offset(0f, with(density) { produced.toPx() })
                            }
                            return Offset.Zero
                        }
                    }
                }

            val toolbarItems = listOf(
                ToolbarItem(
                    iconRes = R.drawable.rounded_contacts_product_24,
                    labelRes = R.string.feat_medrop_profile_contact,
                    onClick = { selectedTab = MeDropProfileType.CONTACT }
                ),
                ToolbarItem(
                    iconRes = R.drawable.rounded_work_24,
                    labelRes = R.string.feat_medrop_profile_professional,
                    onClick = { selectedTab = MeDropProfileType.PROFESSIONAL }
                ),
                ToolbarItem(
                    iconRes = R.drawable.rounded_id_card_24,
                    labelRes = R.string.feat_medrop_profile_custom,
                    onClick = { selectedTab = MeDropProfileType.CUSTOM }
                )
            )
            val selectedTabIndex = when (selectedTab) {
                MeDropProfileType.CONTACT -> 0
                MeDropProfileType.PROFESSIONAL -> 1
                MeDropProfileType.CUSTOM -> 2
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
                                    .nestedScroll(nestedScrollConnection)
                                    .verticalScroll(rememberScrollState()),
                        ) {
                            Spacer(
                                modifier =
                                    Modifier.height(
                                        WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                                    ),
                            )

                            MeDropSettingsUI(
                                viewModel = viewModel,
                                headerHeight = headerHeight,
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it },
                                modifier = Modifier.padding(top = 4.dp),
                            )

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
                            items = toolbarItems,
                            selectedIndex = selectedTabIndex,
                            fabIconRes = R.drawable.rounded_contacts_product_24,
                            fabAction = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                contactPickerLauncher.launch(MeDropContactPickerHelper.buildPickIntent())
                            },
                            fabContentDescription = stringResource(R.string.feat_medrop_select_contact),
                            modifier =
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .zIndex(1f),
                        )
                    }
                }
            }
        }
    }
}