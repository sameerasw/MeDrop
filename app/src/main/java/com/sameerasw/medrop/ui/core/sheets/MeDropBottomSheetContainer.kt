package com.sameerasw.medrop.ui.core.sheets

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.sameerasw.medrop.R
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeDropBottomSheetContainer(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    properties: ModalBottomSheetProperties = ModalBottomSheetDefaults.properties,
    rippleTrigger: Int = 0,
    rippleOrigin: Offset = Offset.Zero,
    content: @Composable ColumnScope.() -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val animTime = remember { Animatable(0f) }

    LaunchedEffect(rippleTrigger) {
        if (rippleTrigger > 0) {
            animTime.snapTo(0f)
            animTime.animateTo(
                targetValue = 3.2f,
                animationSpec = tween(durationMillis = 3200, easing = LinearEasing)
            )
            animTime.snapTo(0f)
        }
    }

    val shader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val code = context.resources.openRawResource(R.raw.nfc_ripple).use {
                BufferedReader(InputStreamReader(it)).readText()
            }
            RuntimeShader(code)
        } else null
    }

    var containerWindowPos by remember { mutableStateOf(Offset.Zero) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = containerColor,
        scrimColor = scrimColor,
        dragHandle = dragHandle,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        properties = properties,
        modifier = modifier
            .statusBarsPadding()
            .imePadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    containerWindowPos = coords.positionInWindow()
                }
                .graphicsLayer {
                    val currentTime = animTime.value
                    if (currentTime > 0f && currentTime < 3.2f && shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val densityVal = density.density
                        val amplitude = 32f * densityVal
                        val frequency = 12f
                        val decay = 4.5f
                        val speed = 1400f * densityVal

                        val localOriginX = if (rippleOrigin != Offset.Zero) {
                            rippleOrigin.x - containerWindowPos.x
                        } else {
                            size.width / 2f
                        }
                        val localOriginY = 0f

                        shader.setFloatUniform("uResolution", size.width, size.height)
                        shader.setFloatUniform("uOrigin", localOriginX, localOriginY)
                        shader.setFloatUniform("uTime", currentTime)
                        shader.setFloatUniform("uAmplitude", amplitude)
                        shader.setFloatUniform("uFrequency", frequency)
                        shader.setFloatUniform("uDecay", decay)
                        shader.setFloatUniform("uSpeed", speed)

                        renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "inputShader").asComposeRenderEffect()
                    } else {
                        renderEffect = null
                    }
                }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                content()
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}

