package com.bludood.mirror

import android.os.Build
import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.MessageTemplate
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.testing.TestCarContext
import androidx.test.platform.app.InstrumentationRegistry
import com.bludood.mirror.car.MirrorScreen
import com.bludood.mirror.car.WelcomeScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CarTemplateTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before
    fun emulatorOnly() {
        assertTrue("Run only on an emulator", Build.FINGERPRINT.contains("generic") || Build.MODEL.startsWith("sdk_"))
    }

    @Test
    fun readyHasOneBrandedStartWithOlderHostFallback() = instrumentation.runOnMainSync {
        MirrorState.setWaitingForPermission(false)
        for (api in listOf(1, 4, 7)) {
            val template = WelcomeScreen(context(api)).onGetTemplate() as MessageTemplate
            assertEquals("Welcome to BluMirror!", template.message.toString())
            assertNull(template.icon)
            assertNull(template.title)
            assertEquals(1, template.actions.size)
            val start = template.actions.single()
            assertEquals("Start mirroring", start.title.toString())
            assertNotNull(start.icon)
            assertEquals(CarColor.PRIMARY, start.backgroundColor)
            assertEquals(if (api >= 4) Action.FLAG_PRIMARY else 0, start.flags)
        }
    }

    @Test
    fun waitingExplainsPhoneApprovalWithoutCompetingIconOrAction() = instrumentation.runOnMainSync {
        MirrorState.setWaitingForPermission(true)
        try {
            for (api in listOf(1, 2, 7)) {
                val template = WelcomeScreen(context(api)).onGetTemplate() as MessageTemplate
                assertEquals("Approve screen sharing on your phone.", template.message.toString())
                assertFalse(template.isLoading)
                assertNull(template.title)
                assertNull(template.icon)
                assertTrue(template.actions.isEmpty())
            }
        } finally {
            MirrorState.setWaitingForPermission(false)
        }
    }

    @Test
    fun mirrorKeepsOneStopWithTextAndIcon() = instrumentation.runOnMainSync {
        val template = MirrorScreen(context(7)).onGetTemplate() as NavigationTemplate
        val stop = template.actionStrip!!.actions.single()
        assertEquals("Stop mirroring", stop.title.toString())
        assertNotNull(stop.icon)
        assertFalse(stop.flags and Action.FLAG_PRIMARY != 0)
    }

    private fun context(api: Int): TestCarContext {
        val context = TestCarContext.createCarContext(instrumentation.targetContext)
        // Car App Library 1.7.0's test context has no public host API-level setter.
        CarContext::class.java.getDeclaredField("mCarAppApiLevel").apply {
            isAccessible = true
            setInt(context, api)
        }
        return context
    }
}
