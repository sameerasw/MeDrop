package com.sameerasw.medrop

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.sameerasw.medrop.data.repository.MeDropRepository
import com.sameerasw.medrop.domain.model.MeDropProfileType
import com.sameerasw.medrop.domain.model.MeDropSettings
import com.sameerasw.medrop.ui.activities.SettingsActivity
import com.sameerasw.medrop.ui.components.MeDropFloatingToolbar
import com.sameerasw.medrop.ui.components.ToolbarItem
import com.sameerasw.medrop.ui.core.sheets.PermissionItem
import com.sameerasw.medrop.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.medrop.ui.features.MeDropHeaderUI
import com.sameerasw.medrop.ui.features.MeDropProfileFieldsUI
import com.sameerasw.medrop.ui.modifiers.BlurDirection
import com.sameerasw.medrop.ui.modifiers.progressiveBlur
import com.sameerasw.medrop.ui.theme.MeDropTheme
import com.sameerasw.medrop.utils.HapticUtil
import com.sameerasw.medrop.utils.MeDropContactPickerHelper
import com.sameerasw.medrop.utils.PermissionUtils
import com.sameerasw.medrop.viewmodels.MeDropViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
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

        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            try {
                val splashScreenView = splashScreenViewProvider.view
                val fadeOut =
                    android.animation.ObjectAnimator.ofFloat(splashScreenView, "alpha", 1f, 0f).apply {
                        interpolator = androidx.interpolator.view.animation.FastOutSlowInInterpolator()
                        duration = 400
                    }
                fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        splashScreenViewProvider.remove()
                    }
                })
                fadeOut.start()
            } catch (e: Exception) {
                splashScreenViewProvider.remove()
            }
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
            val hasContactsPerm by viewModel.hasContactsPermission
            val settings by viewModel.meDropSettings
            val safeSettings = settings ?: MeDropSettings()

            val entranceProgress = remember { androidx.compose.animation.core.Animatable(0f) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(250)
                entranceProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 1200,
                        easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.0f, 0f, 1.0f)
                    )
                )
            }

            val density = LocalDensity.current
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.dp
            val screenHeight = configuration.screenHeightDp.dp
            val minHeaderHeight = 200.dp
            val maxHeaderHeight = minOf(screenWidth, screenHeight * 0.6f).coerceAtLeast(minHeaderHeight)
            var headerHeight by remember { mutableStateOf(minHeaderHeight) }

            val view = LocalView.current
            val scope = rememberCoroutineScope()

            val enabledTabs = remember(safeSettings) {
                val list = mutableListOf(MeDropProfileType.CONTACT)
                if (safeSettings.professionalProfile.enabled) {
                    list.add(MeDropProfileType.PROFESSIONAL)
                }
                if (safeSettings.customProfile.enabled) {
                    list.add(MeDropProfileType.CUSTOM)
                }
                list
            }

            val pagerState = rememberPagerState(
                initialPage = 0,
                pageCount = { enabledTabs.size }
            )

            // Adjust pager page if tabs count changes
            LaunchedEffect(enabledTabs) {
                if (pagerState.currentPage >= enabledTabs.size) {
                    pagerState.scrollToPage(0)
                }
            }

            // Haptics on tab switch
            LaunchedEffect(pagerState) {
                var isFirst = true
                snapshotFlow { pagerState.currentPage }.collect {
                    if (isFirst) {
                        isFirst = false
                    } else {
                        HapticUtil.performHeavyHaptic(view)
                    }
                }
            }

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

            val toolbarItems = enabledTabs.mapIndexed { index, type ->
                when (type) {
                    MeDropProfileType.CONTACT -> ToolbarItem(
                        iconRes = R.drawable.medrop_logo,
                        labelRes = R.string.feat_medrop_profile_contact,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index, animationSpec = tween(300))
                            }
                        }
                    )
                    MeDropProfileType.PROFESSIONAL -> ToolbarItem(
                        iconRes = R.drawable.rounded_work_24,
                        labelRes = R.string.feat_medrop_profile_professional,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index, animationSpec = tween(300))
                            }
                        }
                    )
                    MeDropProfileType.CUSTOM -> ToolbarItem(
                        iconRes = R.drawable.rounded_id_card_24,
                        labelRes = R.string.feat_medrop_profile_custom,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index, animationSpec = tween(300))
                            }
                        }
                    )
                }
            }

            val activeProfileType = enabledTabs.getOrNull(pagerState.currentPage) ?: MeDropProfileType.CONTACT

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
                            val expansionFraction = ((headerHeight - minHeaderHeight) / (maxHeaderHeight - minHeaderHeight)).coerceIn(0f, 1f)
                            val topSpacerHeight = (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() * (1f - expansionFraction)) - (24.dp * expansionFraction)
                            Spacer(
                                modifier =
                                    Modifier.height(topSpacerHeight.coerceAtLeast(-24.dp)),
                            )

                            // Common Top Header: Photo (with morphing shape) & Contact Name
                            MeDropHeaderUI(
                                viewModel = viewModel,
                                headerHeight = headerHeight,
                                activeProfileType = activeProfileType,
                                onPickContactClick = onPickContactClick,
                                entranceProgress = entranceProgress.value,
                                modifier = Modifier.padding(top = (4.dp * (1f - expansionFraction))),
                            )

                            val contentOffsetY = with(density) { (1f - entranceProgress.value) * 300.dp.toPx() }
                            val contentAlpha = entranceProgress.value.coerceIn(0f, 1f)

                            // Swipeable Fields Area per Tab (smoothly slides up from bottom)
                            if (safeSettings.contact != null) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset { androidx.compose.ui.unit.IntOffset(0, contentOffsetY.toInt()) }
                                        .graphicsLayer { alpha = contentAlpha },
                                    verticalAlignment = Alignment.Top,
                                ) { page ->
                                    val currentProfileType = enabledTabs.getOrNull(page) ?: MeDropProfileType.CONTACT
                                    MeDropProfileFieldsUI(
                                        viewModel = viewModel,
                                        profileType = currentProfileType,
                                    )
                                }
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

                        val toolbarOffsetY = with(density) { (1f - entranceProgress.value) * 150.dp.toPx() }
                        MeDropFloatingToolbar(
                            items = toolbarItems,
                            selectedIndex = pagerState.currentPage.coerceIn(0, toolbarItems.size - 1),
                            fabIconRes = R.drawable.rounded_settings_24,
                            fabAction = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                val intent = Intent(context, SettingsActivity::class.java)
                                context.startActivity(intent)
                            },
                            fabContentDescription = stringResource(R.string.action_settings),
                            modifier =
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset { androidx.compose.ui.unit.IntOffset(0, toolbarOffsetY.toInt()) }
                                    .graphicsLayer { alpha = entranceProgress.value }
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