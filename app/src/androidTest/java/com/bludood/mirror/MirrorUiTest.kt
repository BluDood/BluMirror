package com.bludood.mirror

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.bludood.mirror.ui.theme.BluMirrorTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MirrorUiTest {
    @get:Rule val compose = createComposeRule()

    @Before
    fun emulatorOnly() {
        assertTrue("Run only on an emulator", Build.FINGERPRINT.contains("generic") || Build.MODEL.startsWith("sdk_"))
    }

    @Test
    fun stateTransitionsKeepOneActionAndBlockDuplicateApproval() {
        var connected by mutableStateOf(false)
        var streaming by mutableStateOf(false)
        var active by mutableStateOf(false)
        var waiting by mutableStateOf(false)
        var starts = 0
        var stops = 0
        compose.setContent {
            BluMirrorTheme(darkTheme = true, dynamicColor = false) {
                MirrorContent(streaming, active, connected, waiting, { starts++ }, { stops++ })
            }
        }
        compose.onNodeWithText("Connect to Android Auto to start mirroring.").assertIsDisplayed()
        compose.onNodeWithText("Start mirroring").assertDoesNotExist()
        capture("dark-disconnected")
        compose.runOnIdle { connected = true }
        compose.onNodeWithText("Ready to mirror").assertIsDisplayed()
        compose.onNodeWithText("Start mirroring").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, starts) }
        assertTrue("Fitting content should not accept scroll input", compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isEmpty())
        capture("dark-ready")
        compose.onNodeWithText("Start mirroring").performTouchInput { down(center) }
        capture("dark-pressed")
        compose.onNodeWithText("Start mirroring").performTouchInput { up() }
        compose.runOnIdle { waiting = true }
        compose.onNodeWithText("Approve screen sharing on your phone.").assertIsDisplayed()
        compose.onNodeWithText("Start mirroring").assertIsNotEnabled()
        capture("dark-waiting")
        compose.runOnIdle { waiting = false }
        compose.onNodeWithText("Start mirroring").assertIsEnabled()
        compose.runOnIdle { streaming = true }
        compose.onNodeWithText("Open BluMirror on your car display.").assertIsDisplayed()
        compose.onNodeWithText("Start mirroring").assertDoesNotExist()
        compose.onNodeWithText("Stop mirroring").assertIsEnabled()
        capture("dark-open-on-car")
        compose.runOnIdle { active = true }
        compose.onNodeWithText("Mirroring active").assertIsDisplayed()
        compose.onNodeWithText("Stop mirroring").performClick()
        compose.runOnIdle { assertEquals(1, stops) }
        capture("dark-active")
    }

    @Test
    fun lightDynamicAndRtlKeepReadableActions() {
        var dark by mutableStateOf(false)
        var dynamic by mutableStateOf(false)
        var rtl by mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                BluMirrorTheme(darkTheme = dark, dynamicColor = dynamic) {
                    MirrorContent(false, false, true, false, {}, {})
                }
            }
        }
        compose.onNodeWithText("Start mirroring").assertIsDisplayed()
        capture("light-ready")
        compose.runOnIdle { dark = true; dynamic = true }
        compose.onNodeWithText("Start mirroring").assertIsDisplayed()
        capture("dynamic-dark-ready")
        compose.runOnIdle { dark = false }
        compose.onNodeWithText("Start mirroring").assertIsDisplayed()
        capture("dynamic-light-ready")
        compose.runOnIdle { dark = true; dynamic = false; rtl = true }
        compose.onNodeWithText("Start mirroring").assertIsDisplayed()
        capture("dark-rtl")
    }

    @Test
    fun compactLargeTextCanScrollToAndActivateStop() {
        var stops = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                BluMirrorTheme(darkTheme = true, dynamicColor = false) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.width(320.dp).height(240.dp)) {
                            MirrorContent(true, false, true, false, {}, { stops++ })
                        }
                    }
                }
            }
        }
        assertTrue("Overflow must remain scrollable", compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithText("Stop mirroring").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, stops) }
        capture("dark-320dp-2x-landscape-scrolled")
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "ui-evidence").apply { mkdirs() }
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
