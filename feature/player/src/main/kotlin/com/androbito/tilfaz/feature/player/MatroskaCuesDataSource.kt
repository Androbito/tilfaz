package com.androbito.tilfaz.feature.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException
import kotlin.math.min

/**
 * Wraps a data source so a Matroska file whose seek index is only reachable through a seek table at the end of the file
 * (see [MatroskaSeekHead]) reaches the player with a pointer it understands, and becomes seekable. Everything else, from
 * other formats to reads that do not start at the beginning, goes through unchanged; any trouble finding the index falls
 * back to the original bytes.
 */
@UnstableApi
internal class MatroskaCuesDataSource(private val upstreamFactory: DataSource.Factory) : DataSource {
    private var upstream: DataSource? = null
    private var head: ByteArray? = null
    private var headPosition = 0
    private val listeners = mutableListOf<TransferListener>()

    override fun addTransferListener(transferListener: TransferListener) {
        listeners += transferListener
        upstream?.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val source = upstreamFactory.createDataSource().also { s -> listeners.forEach(s::addTransferListener) }
        upstream = source
        head = null
        headPosition = 0
        val length = source.open(dataSpec)
        if (dataSpec.position == 0L) head = readHeadFixingSeekTable(source, dataSpec, length)
        return length
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val head = head
        if (head != null && headPosition < head.size) {
            val count = min(length, head.size - headPosition)
            head.copyInto(buffer, offset, headPosition, headPosition + count)
            headPosition += count
            return count
        }
        return checkNotNull(upstream).read(buffer, offset, length)
    }

    override fun getUri(): Uri? = upstream?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream?.responseHeaders ?: emptyMap()

    override fun close() {
        upstream?.close()
        upstream = null
        head = null
    }

    /** The first bytes of the file, with the seek table corrected when it needs to be. Already consumed from [source]. */
    private fun readHeadFixingSeekTable(source: DataSource, spec: DataSpec, length: Long): ByteArray {
        val wanted = if (length == C.LENGTH_UNSET.toLong()) HEAD_SIZE else min(HEAD_SIZE.toLong(), length).toInt()
        val head = ByteArray(wanted)
        var filled = 0
        while (filled < wanted) {
            val read = source.read(head, filled, wanted - filled)
            if (read == C.RESULT_END_OF_INPUT) break
            filled += read
        }
        val header = head.copyOf(filled)
        return fixed(header, spec.uri) ?: header
    }

    private fun fixed(header: ByteArray, uri: Uri): ByteArray? {
        val chain = MatroskaSeekHead.findChain(header) ?: return null
        val tableStart = chain.segmentDataStart + chain.chainedPosition
        return try {
            val cues = MatroskaSeekHead.cuesPosition(readTable(uri, tableStart)) ?: return null
            MatroskaSeekHead.patched(header, chain, cues)
        } catch (_: IOException) {
            null // the file stays as it is: unseekable, but playable
        }
    }

    private fun readTable(uri: Uri, start: Long): ByteArray {
        val source = upstreamFactory.createDataSource()
        try {
            source.open(DataSpec.Builder().setUri(uri).setPosition(start).build())
            val table = ByteArray(TABLE_SIZE)
            var filled = 0
            while (filled < TABLE_SIZE) {
                val read = source.read(table, filled, TABLE_SIZE - filled)
                if (read == C.RESULT_END_OF_INPUT) break
                filled += read
            }
            return table.copyOf(filled)
        } finally {
            source.close()
        }
    }

    private companion object {
        /** The seek tables and tracks of a Matroska file sit in its first bytes; 64 KB is far more than needed. */
        const val HEAD_SIZE = 64 * 1024
        const val TABLE_SIZE = 4 * 1024
    }
}
