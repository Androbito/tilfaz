package com.androbito.tilfaz.core.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class TitleTextTest {
    @Test
    fun `case and accents do not matter`() {
        assertThat(TitleText.normalize("La Vénus ÉLECTRIQUE")).isEqualTo("la venus electrique")
    }

    @Test
    fun `punctuation becomes single spaces`() {
        assertThat(TitleText.normalize("Spider-Man: No   Way Home!")).isEqualTo("spider man no way home")
    }

    @Test
    fun `the provider's language tag is not part of the title`() {
        assertThat(TitleText.normalize("|FR| Little Lorraine (2026)")).isEqualTo("little lorraine 2026")
        assertThat(TitleText.normalize("| Sub - ARA | Guns Up (2025)")).isEqualTo("guns up 2025")
    }

    @Test
    fun `a title written between bars keeps all its parts`() {
        assertThat(TitleText.normalize("| VISA | 2026 | فيزا |")).isEqualTo("visa 2026 فيزا")
    }

    @Test
    fun `arabic letter variants and marks are folded together`() {
        assertThat(TitleText.normalize("أحمد")).isEqualTo(TitleText.normalize("احمد"))
        assertThat(TitleText.normalize("مدرسة")).isEqualTo(TitleText.normalize("مدرسه"))
        assertThat(TitleText.normalize("علىٰ")).isEqualTo(TitleText.normalize("علي"))
        assertThat(TitleText.normalize("مُحَمَّد")).isEqualTo(TitleText.normalize("محمد"))
        assertThat(TitleText.normalize("إسلام")).isEqualTo(TitleText.normalize("اسلام"))
    }

    @Test
    fun `a title is cleaned for display without losing its parts`() {
        assertThat(TitleText.clean("|FR| Scream 4 (2011)")).isEqualTo("Scream 4 (2011)")
        assertThat(TitleText.clean("| Sub - ARA | Guns Up (2025)")).isEqualTo("Guns Up (2025)")
        assertThat(TitleText.clean("| The Mysterious Sound! | 2026 | ده صوت")).isEqualTo("The Mysterious Sound! · 2026 · ده صوت")
        assertThat(TitleText.clean("Plain Title")).isEqualTo("Plain Title")
    }

    @Test
    fun `a heading made of repeated symbols is a separator, a real title is not`() {
        assertThat(TitleText.isSeparator("-----| BEIN SPORTS |-----")).isEqualTo(true)
        assertThat(TitleText.isSeparator("===== FRANCE =====")).isEqualTo(true)
        assertThat(TitleText.isSeparator("*** SPORT ***")).isEqualTo(true)
        assertThat(TitleText.isSeparator("▬▬▬▬ MOVIES ▬▬▬▬")).isEqualTo(true)
        assertThat(TitleText.isSeparator("=====")).isEqualTo(true)
        assertThat(TitleText.isSeparator("  ")).isEqualTo(true)
        assertThat(TitleText.isSeparator("|FR| TF1 HD")).isEqualTo(false)
        assertThat(TitleText.isSeparator("| VISA | 2026 | فيزا |")).isEqualTo(false)
        assertThat(TitleText.isSeparator("BEIN SPORTS 1 FHD")).isEqualTo(false)
        assertThat(TitleText.isSeparator("-Eli Roth's History of Horror")).isEqualTo(false)
    }

    @Test
    fun `blank text stays blank`() {
        assertThat(TitleText.normalize("   ")).isEqualTo("")
        assertThat(TitleText.clean("  ")).isEqualTo("")
    }
}
