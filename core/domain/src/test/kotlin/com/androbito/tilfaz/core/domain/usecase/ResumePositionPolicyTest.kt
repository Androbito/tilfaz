package com.androbito.tilfaz.core.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.WatchProgress
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import org.junit.Test

class ResumePositionPolicyTest {
    private fun progress(positionMs: Long, durationMs: Long = 100 * 60_000L) = WatchProgress(
        playableKey = "vod:501",
        contentType = ContentType.VOD,
        title = "Film",
        posterUrl = null,
        positionMs = positionMs,
        durationMs = durationMs,
        updatedAt = Fixtures.now,
        reference = "501",
    )

    @Test
    fun `resumes from saved position`() {
        assertThat(ResumePositionPolicy.startPositionMs(progress(42 * 60_000L), resumeEnabled = true)).isEqualTo(42 * 60_000L)
    }

    @Test
    fun `starts over when resume is disabled or nothing is saved`() {
        assertThat(ResumePositionPolicy.startPositionMs(progress(42 * 60_000L), resumeEnabled = false)).isEqualTo(0L)
        assertThat(ResumePositionPolicy.startPositionMs(null, resumeEnabled = true)).isEqualTo(0L)
    }

    @Test
    fun `finished items and tiny positions start over`() {
        assertThat(ResumePositionPolicy.startPositionMs(progress(97 * 60_000L), resumeEnabled = true)).isEqualTo(0L)
        assertThat(ResumePositionPolicy.startPositionMs(progress(10_000L), resumeEnabled = true)).isEqualTo(0L)
    }
}
