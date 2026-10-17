package com.androbito.tilfaz.feature.player

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class SeekScrubberTest {
    private val duration = 2 * 60 * 60 * 1_000L // two hours
    private val position = 30 * 60 * 1_000L // half an hour in

    /** Presses a key [count] times, [gapMs] apart, starting from [start]. */
    private fun press(direction: Int, count: Int, gapMs: Long, start: ScrubState? = null, atMs: Long = 0, from: Long = position): ScrubState {
        var state = start
        repeat(count) { i -> state = SeekScrubber.step(state, direction, from, duration, atMs + i * gapMs) }
        return state!!
    }

    @Test
    fun `the first press moves ten seconds from where playback is`() {
        val state = SeekScrubber.step(null, direction = +1, positionMs = position, durationMs = duration, nowMs = 0)

        assertThat(state.targetMs).isEqualTo(position + 10_000)
        assertThat(state.deltaMs).isEqualTo(10_000)
    }

    @Test
    fun `slow presses stay at ten seconds but keep adding up`() {
        val state = press(direction = +1, count = 3, gapMs = 1_000)

        assertThat(state.targetMs).isEqualTo(position + 30_000)
    }

    @Test
    fun `holding the key speeds the cursor up`() {
        val tenFast = press(direction = +1, count = 10, gapMs = 100)

        // 3 steps of 10s, 5 of 30s, 2 of 60s.
        assertThat(tenFast.deltaMs).isEqualTo(3 * 10_000L + 5 * 30_000L + 2 * 60_000L)
    }

    @Test
    fun `a long hold reaches five minute steps`() {
        val longHold = press(direction = +1, count = 20, gapMs = 100)
        val one = SeekScrubber.step(longHold, +1, position, duration, nowMs = 20 * 100L)

        assertThat(one.targetMs - longHold.targetMs).isEqualTo(300_000L)
    }

    @Test
    fun `pausing between presses starts the ladder again`() {
        val first = press(direction = +1, count = 10, gapMs = 100)

        val after = SeekScrubber.step(first, +1, position, duration, nowMs = 10 * 100L + SeekScrubber.STREAK_WINDOW_MS + 1)

        assertThat(after.targetMs - first.targetMs).isEqualTo(10_000L)
    }

    @Test
    fun `changing direction starts the ladder again`() {
        val forward = press(direction = +1, count = 10, gapMs = 100)

        val back = SeekScrubber.step(forward, -1, position, duration, nowMs = 10 * 100L)

        assertThat(back.targetMs).isEqualTo(forward.targetMs - 10_000L)
    }

    @Test
    fun `the cursor never leaves the video`() {
        val atStart = press(direction = -1, count = 5, gapMs = 1_000, from = 15_000)
        val atEnd = press(direction = +1, count = 40, gapMs = 100, from = duration - 20_000)

        assertThat(atStart.targetMs).isEqualTo(0L)
        assertThat(atEnd.targetMs).isEqualTo(duration)
    }

    @Test
    fun `the target is relative to the position the scrub began at, not to the moving playhead`() {
        val first = SeekScrubber.step(null, +1, position, duration, 0)

        // Playback carried on while the cursor was out: the origin must not shift under the user.
        val second = SeekScrubber.step(first, +1, position + 5_000, duration, 1_000)

        assertThat(second.originMs).isEqualTo(position)
        assertThat(second.targetMs).isEqualTo(position + 20_000)
    }

    @Test
    fun `the change reads like a sign and a clock`() {
        assertThat(formatScrubDelta(120_000)).isEqualTo("+2:00")
        assertThat(formatScrubDelta(-30_000)).isEqualTo("-0:30")
        assertThat(formatScrubDelta(3_725_000)).isEqualTo("+1:02:05")
        assertThat(formatScrubDelta(0)).isEqualTo("0:00")
    }
}
