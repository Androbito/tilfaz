package com.androbito.tilfaz.feature.player

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class OnStopEffectTest {
    @get:Rule
    val compose = createComposeRule()

    private class TestOwner : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry
    }

    @Test
    fun `leaving the screen runs the action, staying on it does not`() {
        val owner = TestOwner().apply { registry.currentState = Lifecycle.State.RESUMED }
        var stops = 0
        compose.setContent { CompositionLocalProvider(LocalLifecycleOwner provides owner) { OnStopEffect { stops++ } } }

        compose.runOnIdle { assertThat(stops).isEqualTo(0) }
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED } // stopped: another app or the launcher is in front
        compose.runOnIdle { assertThat(stops).isEqualTo(1) }
    }
}
