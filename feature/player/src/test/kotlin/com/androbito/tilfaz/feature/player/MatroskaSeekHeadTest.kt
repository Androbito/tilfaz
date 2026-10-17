package com.androbito.tilfaz.feature.player

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import java.io.ByteArrayOutputStream
import org.junit.Test

/** Builds the few EBML elements the seek tables are made of, the way mkvmerge writes them. */
internal object Ebml {
    const val EBML_HEADER = 0x1A45DFA3L
    const val SEGMENT = 0x18538067L
    const val SEEK_HEAD = 0x114D9B74L
    const val SEEK = 0x4DBBL
    const val SEEK_ID = 0x53ABL
    const val SEEK_POSITION = 0x53ACL
    const val CUES = 0x1C53BB6BL
    const val TRACKS = 0x1654AE6BL
    const val VOID = 0xECL

    private fun idBytes(id: Long): ByteArray {
        val length = when {
            id > 0xFFFFFF -> 4
            id > 0xFFFF -> 3
            id > 0xFF -> 2
            else -> 1
        }
        return ByteArray(length) { (id shr (8 * (length - 1 - it))).toByte() }
    }

    private fun sizeBytes(size: Int): ByteArray = byteArrayOf((0x80 or size).toByte()).also { check(size < 0x7F) }

    fun element(id: Long, body: ByteArray = ByteArray(0)): ByteArray = idBytes(id) + sizeBytes(body.size) + body

    fun uint(value: Long, width: Int): ByteArray = ByteArray(width) { (value shr (8 * (width - 1 - it))).toByte() }

    /** A Seek entry: which element, and where it starts relative to the segment's content. */
    fun seek(target: Long, position: Long, positionWidth: Int = 4): ByteArray = element(
        SEEK,
        element(SEEK_ID, idBytes(target)) + element(SEEK_POSITION, uint(position, positionWidth))
    )

    fun seekHead(vararg entries: ByteArray): ByteArray = element(SEEK_HEAD, entries.fold(ByteArray(0)) { a, b -> a + b })

    /** The file start: EBML header, then a Segment of unknown size whose content begins with [content]. */
    fun fileStart(content: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(element(EBML_HEADER, ByteArray(4)))
        out.write(idBytes(SEGMENT))
        out.write(byteArrayOf(0x01, -1, -1, -1, -1, -1, -1, -1)) // unknown size, as live-muxed files write it
        out.write(content)
        return out.toByteArray()
    }
}

class MatroskaSeekHeadTest {
    private val chained = Ebml.fileStart(Ebml.seekHead(Ebml.seek(Ebml.SEEK_HEAD, position = 5_000)) + Ebml.element(Ebml.VOID, ByteArray(20)))

    @Test
    fun `a first seek table that only points to another one at the end is recognised`() {
        val chain = MatroskaSeekHead.findChain(chained)

        assertThat(chain?.chainedPosition).isEqualTo(5_000L)
    }

    @Test
    fun `the end seek table tells where the cues are`() {
        val atEnd = Ebml.seekHead(Ebml.seek(Ebml.TRACKS, 100), Ebml.seek(Ebml.CUES, 4_900))

        assertThat(MatroskaSeekHead.cuesPosition(atEnd)).isEqualTo(4_900L)
    }

    @Test
    fun `patching makes the first seek table point straight at the cues`() {
        val chain = MatroskaSeekHead.findChain(chained)!!

        val patched = MatroskaSeekHead.patched(chained, chain, cuesPosition = 4_900)!!

        assertThat(patched.size).isEqualTo(chained.size)
        assertThat(MatroskaSeekHead.findChain(patched)).isNull() // no longer a bare pointer to another table
        assertThat(MatroskaSeekHead.cuesPosition(patched.copyOfRange(chain.segmentDataStart, patched.size))).isEqualTo(4_900L)
    }

    @Test
    fun `a file that already lists its cues is left alone`() {
        val direct = Ebml.fileStart(Ebml.seekHead(Ebml.seek(Ebml.TRACKS, 100), Ebml.seek(Ebml.CUES, 4_900)))

        assertThat(MatroskaSeekHead.findChain(direct)).isNull()
    }

    @Test
    fun `bytes that are not a matroska file are left alone`() {
        assertThat(MatroskaSeekHead.findChain(ByteArray(2_000) { 7 })).isNull()
        assertThat(MatroskaSeekHead.findChain(ByteArray(0))).isNull()
    }

    @Test
    fun `a header cut short does not crash`() {
        assertThat(MatroskaSeekHead.findChain(chained.copyOf(chained.size - 40))).isNull()
        assertThat(MatroskaSeekHead.findChain(chained.copyOf(10))).isNull()
    }

    @Test
    fun `a cues position too large for the room in the table is not patched`() {
        val narrow = Ebml.fileStart(Ebml.seekHead(Ebml.seek(Ebml.SEEK_HEAD, position = 5_000, positionWidth = 2)))
        val chain = MatroskaSeekHead.findChain(narrow)!!

        assertThat(MatroskaSeekHead.patched(narrow, chain, cuesPosition = 70_000)).isNull()
        assertThat(MatroskaSeekHead.patched(narrow, chain, cuesPosition = 4_900)).isNotNull()
    }
}
