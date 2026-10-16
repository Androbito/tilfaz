package com.androbito.tilfaz.core.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.androbito.tilfaz.core.model.NowNext
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import org.junit.Test

class NowNextResolverTest {
    private val news = Fixtures.epg(101, "News Hour", startOffsetMin = -30, durationMin = 60)
    private val debate = Fixtures.epg(101, "The Debate", startOffsetMin = 30, durationMin = 60)
    private val earlier = Fixtures.epg(101, "Morning Show", startOffsetMin = -180, durationMin = 120)

    @Test
    fun `empty list gives empty now next`() {
        assertThat(NowNextResolver.resolve(emptyList(), Fixtures.now)).isEqualTo(NowNext.Empty)
    }

    @Test
    fun `picks the programme on air and the following one, whatever the input order`() {
        val result = NowNextResolver.resolve(listOf(debate, earlier, news), Fixtures.now)
        assertThat(result.now).isEqualTo(news)
        assertThat(result.next).isEqualTo(debate)
    }

    @Test
    fun `when nothing is on air, next is the first future programme`() {
        val result = NowNextResolver.resolve(listOf(earlier, debate), Fixtures.now)
        assertThat(result.now).isNull()
        assertThat(result.next).isEqualTo(debate)
    }

    @Test
    fun `a programme ending exactly now is not on air`() {
        val justEnded = Fixtures.epg(101, "Just Ended", startOffsetMin = -60, durationMin = 60)
        assertThat(NowNextResolver.resolve(listOf(justEnded), Fixtures.now).now).isNull()
    }

    @Test
    fun `progress is the elapsed fraction`() {
        assertThat(news.progressAt(Fixtures.now)).isEqualTo(0.5f)
    }
}
