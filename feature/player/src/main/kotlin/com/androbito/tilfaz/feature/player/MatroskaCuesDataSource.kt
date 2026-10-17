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
 *
 * The provider allows one connection per account and does not like connections being cut off, so the file is looked at
 * with short requests that run one after the other and finish on their own: the head, then (only when needed) the end
 * seek table, and only then the stream the player reads, which starts where the head ended.
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
        head = null
        headPosition = 0
        if (dataSpec.position != 0L) return openUpstream(dataSpec)

        val header = readBounded(dataSpec.buildUpon().setLength(headSize(dataSpec).toLong()).build()) ?: return openUpstream(dataSpec)
        val chain = MatroskaSeekHead.findChain(header)
        head = chain?.let { fixed(header, it, dataSpec.uri) } ?: header

        val complete = header.size < headSize(dataSpec) // the file ended inside the head: there is nothing more to read
        if (complete) return header.size.toLong()
        val rest = dataSpec.buildUpon().setPosition(header.size.toLong())
        if (dataSpec.length != C.LENGTH_UNSET.toLong()) rest.setLength(dataSpec.length - header.size)
        val restLength = openUpstream(rest.build())
        return if (restLength == C.LENGTH_UNSET.toLong()) restLength else header.size + restLength
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
        return upstream?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT
    }

    override fun getUri(): Uri? = upstream?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream?.responseHeaders ?: emptyMap()

    override fun close() {
        upstream?.close()
        upstream = null
        head = null
    }

    private fun headSize(spec: DataSpec): Int =
        if (spec.length == C.LENGTH_UNSET.toLong()) HEAD_SIZE else min(HEAD_SIZE.toLong(), spec.length).toInt()

    private fun openUpstream(spec: DataSpec): Long {
        val source = upstreamFactory.createDataSource()
        listeners.forEach(source::addTransferListener)
        upstream = source
        return source.open(spec)
    }

    /** Reads the whole of [spec], which must be bounded, on its own connection; null if that fails. */
    private fun readBounded(spec: DataSpec): ByteArray? {
        val source = upstreamFactory.createDataSource()
        return try {
            val length = source.open(spec)
            val size = if (length == C.LENGTH_UNSET.toLong()) spec.length.toInt() else min(length, spec.length).toInt()
            val bytes = ByteArray(size)
            var filled = 0
            while (filled < size) {
                val read = source.read(bytes, filled, size - filled)
                if (read == C.RESULT_END_OF_INPUT) break
                filled += read
            }
            bytes.copyOf(filled)
        } catch (_: IOException) {
            null
        } finally {
            source.close()
        }
    }

    private fun fixed(header: ByteArray, chain: MatroskaSeekHead.Chain, uri: Uri): ByteArray? {
        val tableStart = chain.segmentDataStart + chain.chainedPosition
        val table = readBounded(DataSpec.Builder().setUri(uri).setPosition(tableStart).setLength(TABLE_SIZE.toLong()).build()) ?: return null
        return MatroskaSeekHead.cuesPosition(table)?.let { MatroskaSeekHead.patched(header, chain, it) }
    }

    private companion object {
        /** The seek tables and tracks of a Matroska file sit in its first bytes; 64 KB is far more than needed. */
        const val HEAD_SIZE = 64 * 1024
        const val TABLE_SIZE = 4 * 1024
    }
}
