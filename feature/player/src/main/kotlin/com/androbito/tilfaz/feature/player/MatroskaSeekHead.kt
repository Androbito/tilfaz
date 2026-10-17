package com.androbito.tilfaz.feature.player

/**
 * Some Matroska files list their seek index (the "Cues") only in a second seek table at the very end of the file, and the
 * table at the start merely points to that one. ExoPlayer follows direct pointers to the Cues only, so it takes such a file
 * for unseekable. This finds that layout and rewrites the pointer in the first table so it names the Cues directly, without
 * changing the file's size or any other byte.
 */
internal object MatroskaSeekHead {
    private const val EBML_HEADER = 0x1A45DFA3L
    private const val SEGMENT = 0x18538067L
    private const val SEEK_HEAD = 0x114D9B74L
    private const val SEEK = 0x4DBBL
    private const val SEEK_ID = 0x53ABL
    private const val SEEK_POSITION = 0x53ACL
    private const val CUES = 0x1C53BB6BL
    private const val VOID = 0xECL
    private const val CUES_ID_WIDTH = 4
    private const val MAX_ELEMENTS_BEFORE_SEEK_HEAD = 8

    /** Where in the first bytes the pointer to another seek table sits, and where that table starts. */
    class Chain(
        /** Where the segment's content starts in the file; every position in the seek tables is relative to it. */
        val segmentDataStart: Int,
        /** Where the second seek table starts, relative to [segmentDataStart]. */
        val chainedPosition: Long,
        val idOffset: Int,
        val positionOffset: Int,
        val positionWidth: Int
    )

    private class Entry(val id: Long, val position: Long, val idOffset: Int, val idWidth: Int, val positionOffset: Int, val positionWidth: Int)

    private class Element(val id: Long, val contentStart: Int, val contentSize: Long)

    /** The chain when [header] (a file's first bytes) has the layout described above, else null. */
    fun findChain(header: ByteArray): Chain? {
        val ebml = readElement(header, 0)?.takeIf { it.id == EBML_HEADER } ?: return null
        val segment = readElement(header, (ebml.contentStart + ebml.contentSize).toIntOrNull() ?: return null)
            ?.takeIf { it.id == SEGMENT } ?: return null
        var position = segment.contentStart
        repeat(MAX_ELEMENTS_BEFORE_SEEK_HEAD) {
            val element = readElement(header, position) ?: return null
            when (element.id) {
                VOID -> position = (element.contentStart + element.contentSize).toIntOrNull() ?: return null
                SEEK_HEAD -> {
                    val entries = entriesOf(header, element) ?: return null
                    if (entries.any { it.id == CUES }) return null
                    val pointer = entries.firstOrNull { it.id == SEEK_HEAD && it.idWidth == CUES_ID_WIDTH } ?: return null
                    return Chain(segment.contentStart, pointer.position, pointer.idOffset, pointer.positionOffset, pointer.positionWidth)
                }
                else -> return null
            }
        }
        return null
    }

    /** Where the Cues start, relative to the segment's content, read from a seek table that begins [seekHead]. */
    fun cuesPosition(seekHead: ByteArray): Long? {
        val element = readElement(seekHead, 0)?.takeIf { it.id == SEEK_HEAD } ?: return null
        return entriesOf(seekHead, element)?.firstOrNull { it.id == CUES }?.position
    }

    /** A copy of [header] whose first seek table names the Cues at [cuesPosition]; null if that number does not fit. */
    fun patched(header: ByteArray, chain: Chain, cuesPosition: Long): ByteArray? {
        if (cuesPosition < 0 || (chain.positionWidth < Long.SIZE_BYTES && cuesPosition >= 1L shl (8 * chain.positionWidth))) return null
        val copy = header.copyOf()
        writeUnsigned(copy, chain.idOffset, CUES, CUES_ID_WIDTH)
        writeUnsigned(copy, chain.positionOffset, cuesPosition, chain.positionWidth)
        return copy
    }

    private fun entriesOf(bytes: ByteArray, seekHead: Element): List<Entry>? {
        val end = (seekHead.contentStart + seekHead.contentSize).toIntOrNull()?.takeIf { it <= bytes.size } ?: return null
        val entries = mutableListOf<Entry>()
        var position = seekHead.contentStart
        while (position < end) {
            val seek = readElement(bytes, position) ?: return null
            val seekEnd = (seek.contentStart + seek.contentSize).toIntOrNull()?.takeIf { it <= end } ?: return null
            if (seek.id == SEEK) entryOf(bytes, seek, seekEnd)?.let(entries::add)
            position = seekEnd
        }
        return entries
    }

    private fun entryOf(bytes: ByteArray, seek: Element, end: Int): Entry? {
        var id: Long? = null
        var idOffset = 0
        var idWidth = 0
        var target: Long? = null
        var targetOffset = 0
        var targetWidth = 0
        var position = seek.contentStart
        while (position < end) {
            val child = readElement(bytes, position) ?: return null
            val childEnd = (child.contentStart + child.contentSize).toIntOrNull()?.takeIf { it <= end } ?: return null
            val width = childEnd - child.contentStart
            when (child.id) {
                SEEK_ID -> {
                    id = readUnsigned(bytes, child.contentStart, width)
                    idOffset = child.contentStart
                    idWidth = width
                }
                SEEK_POSITION -> {
                    target = readUnsigned(bytes, child.contentStart, width)
                    targetOffset = child.contentStart
                    targetWidth = width
                }
            }
            position = childEnd
        }
        return if (id != null && target != null) Entry(id, target, idOffset, idWidth, targetOffset, targetWidth) else null
    }

    /** An element's id and the extent of its content; null when it does not fit in [bytes] or is malformed. */
    private fun readElement(bytes: ByteArray, at: Int): Element? {
        if (at < 0 || at >= bytes.size) return null
        val idWidth = leadingWidth(bytes[at]).takeIf { it in 1..4 } ?: return null
        if (at + idWidth >= bytes.size) return null
        val id = readUnsigned(bytes, at, idWidth)
        val sizeAt = at + idWidth
        val sizeWidth = leadingWidth(bytes[sizeAt]).takeIf { it in 1..8 } ?: return null
        if (sizeAt + sizeWidth > bytes.size) return null
        var size = (bytes[sizeAt].toLong() and 0xFF) and ((1L shl (8 - sizeWidth)) - 1)
        for (k in 1 until sizeWidth) size = (size shl 8) or (bytes[sizeAt + k].toLong() and 0xFF)
        val unknown = size == (1L shl (7 * sizeWidth)) - 1
        // Only the segment may have an unknown size; its content simply runs to the end of the file.
        return Element(id, sizeAt + sizeWidth, if (unknown) Long.MAX_VALUE / 2 else size)
    }

    /** How many bytes a variable length number starting with [first] takes: 1 for 1xxxxxxx up to 8 for 00000001. */
    private fun leadingWidth(first: Byte): Int = Integer.numberOfLeadingZeros(first.toInt() and 0xFF) - 23

    private fun readUnsigned(bytes: ByteArray, at: Int, width: Int): Long {
        var value = 0L
        for (k in 0 until width) value = (value shl 8) or (bytes[at + k].toLong() and 0xFF)
        return value
    }

    private fun writeUnsigned(bytes: ByteArray, at: Int, value: Long, width: Int) {
        for (k in 0 until width) bytes[at + k] = (value shr (8 * (width - 1 - k))).toByte()
    }

    private fun Long.toIntOrNull(): Int? = takeIf { it in 0..Int.MAX_VALUE }?.toInt()
}
