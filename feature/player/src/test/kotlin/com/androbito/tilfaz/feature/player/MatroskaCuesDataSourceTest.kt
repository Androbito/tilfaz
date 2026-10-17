package com.androbito.tilfaz.feature.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@UnstableApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MatroskaCuesDataSourceTest {
    private val uri = Uri.parse("http://provider.test/movie.mkv")
    private val cuesAt = 4_900L

    /** A file whose first seek table only points at a second one, 200 bytes into the segment, which names the Cues. */
    private fun chainedFile(): ByteArray {
        val start = Ebml.fileStart(Ebml.seekHead(Ebml.seek(Ebml.SEEK_HEAD, position = 200)))
        val segmentStart = MatroskaSeekHead.findChain(start)!!.segmentDataStart
        val atEnd = Ebml.seekHead(Ebml.seek(Ebml.TRACKS, 100), Ebml.seek(Ebml.CUES, cuesAt))
        val body = ByteArray(segmentStart + 200 + atEnd.size + 120_000)
        start.copyInto(body)
        atEnd.copyInto(body, segmentStart + 200)
        for (i in start.size until segmentStart + 200) body[i] = 0x55
        return body
    }

    private fun source(file: ByteArray) = MatroskaCuesDataSource(DataSource.Factory { ByteArrayDataSource(file) })

    private fun DataSource.readAll(spec: DataSpec): ByteArray {
        val length = open(spec)
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(1_000)
        while (true) {
            val read = read(buffer, 0, buffer.size)
            if (read == C.RESULT_END_OF_INPUT) break
            out.write(buffer, 0, read)
        }
        close()
        if (length != C.LENGTH_UNSET.toLong()) assertThat(out.size().toLong()).isEqualTo(length)
        return out.toByteArray()
    }

    @Test
    fun `reading a chained file from the start gives a first seek table that names the cues`() {
        val file = chainedFile()

        val read = source(file).readAll(DataSpec(uri))

        assertThat(read.size).isEqualTo(file.size)
        assertThat(read).isNotEqualTo(file)
        assertThat(MatroskaSeekHead.findChain(read)).isNull()
        val segmentStart = MatroskaSeekHead.findChain(file)!!.segmentDataStart
        assertThat(MatroskaSeekHead.cuesPosition(read.copyOfRange(segmentStart, read.size))).isEqualTo(cuesAt)
    }

    /** Counts how many sources are open at the same time: the provider's account allows a single connection. */
    private class CountingFactory(private val file: ByteArray) : DataSource.Factory {
        var open = 0
        var mostOpen = 0
        var opened = 0
        val specs = mutableListOf<DataSpec>()

        override fun createDataSource(): DataSource = object : DataSource by ByteArrayDataSource(file) {
            private val inner = ByteArrayDataSource(file)
            override fun open(dataSpec: DataSpec): Long {
                open++
                opened++
                specs += dataSpec
                mostOpen = maxOf(mostOpen, open)
                return inner.open(dataSpec)
            }
            override fun read(buffer: ByteArray, offset: Int, length: Int) = inner.read(buffer, offset, length)
            override fun close() {
                inner.close()
                open--
            }
            override fun addTransferListener(transferListener: androidx.media3.datasource.TransferListener) = inner.addTransferListener(transferListener)
            override fun getUri() = inner.uri
        }
    }

    @Test
    fun `fixing the seek table never holds two connections open at once`() {
        val factory = CountingFactory(chainedFile())

        MatroskaCuesDataSource(factory).readAll(DataSpec(uri))

        assertThat(factory.mostOpen).isEqualTo(1)
        assertThat(factory.open).isEqualTo(0) // everything is closed again
    }

    @Test
    fun `the requests made to look at the file are bounded so each one finishes instead of being cut off`() {
        val factory = CountingFactory(chainedFile())

        MatroskaCuesDataSource(factory).readAll(DataSpec(uri))

        // head, then the end seek table, then the rest of the file from where the head ended
        assertThat(factory.opened).isEqualTo(3)
        assertThat(factory.specs[0].length != C.LENGTH_UNSET.toLong()).isEqualTo(true)
        assertThat(factory.specs[1].length != C.LENGTH_UNSET.toLong()).isEqualTo(true)
        assertThat(factory.specs[2].length).isEqualTo(C.LENGTH_UNSET.toLong())
    }

    @Test
    fun `a file that needs no fixing costs one extra short request and still arrives whole`() {
        val direct = Ebml.fileStart(Ebml.seekHead(Ebml.seek(Ebml.CUES, 300))) + ByteArray(200_000)
        val factory = CountingFactory(direct)

        val read = MatroskaCuesDataSource(factory).readAll(DataSpec(uri))

        assertThat(factory.opened).isEqualTo(2)
        assertThat(factory.mostOpen).isEqualTo(1)
        assertThat(read.toList()).isEqualTo(direct.toList())
    }

    @Test
    fun `a file shorter than the head is read in one request`() {
        val tiny = Ebml.fileStart(Ebml.seekHead(Ebml.seek(Ebml.CUES, 300))) + ByteArray(50)
        val factory = CountingFactory(tiny)

        val read = MatroskaCuesDataSource(factory).readAll(DataSpec(uri))

        assertThat(factory.opened).isEqualTo(1)
        assertThat(read.toList()).isEqualTo(tiny.toList())
    }

    @Test
    fun `only the pointer changes, every other byte is the file's own`() {
        val file = chainedFile()

        val read = source(file).readAll(DataSpec(uri))

        val changed = file.indices.count { file[it] != read[it] }
        assertThat(changed <= 8).isEqualTo(true) // the 4-byte id and the 4-byte position
    }

    @Test
    fun `reading from the middle of the file is passed through untouched`() {
        val file = chainedFile()

        val read = source(file).readAll(DataSpec.Builder().setUri(uri).setPosition(100).build())

        assertThat(read.toList()).isEqualTo(file.copyOfRange(100, file.size).toList())
    }

    @Test
    fun `a file without that layout is passed through untouched`() {
        val direct = Ebml.fileStart(Ebml.seekHead(Ebml.seek(Ebml.CUES, 300))) + ByteArray(500)

        val read = source(direct).readAll(DataSpec(uri))

        assertThat(read.toList()).isEqualTo(direct.toList())
    }

    @Test
    fun `when the second seek table cannot be read the file is passed through untouched`() {
        val start = Ebml.fileStart(Ebml.seekHead(Ebml.seek(Ebml.SEEK_HEAD, position = 90_000)))
        val truncated = start + ByteArray(300)

        val read = source(truncated).readAll(DataSpec(uri))

        assertThat(read.toList()).isEqualTo(truncated.toList())
    }
}
