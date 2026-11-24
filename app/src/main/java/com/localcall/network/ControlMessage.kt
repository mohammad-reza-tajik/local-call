package com.localcall.network

object NetworkConfig {
    const val CONTROL_PORT = 47474
    const val AUDIO_PORT = 47475
}

enum class MessageType { HELLO, CALL, ACCEPT, REJECT, BYE }

/**
 * Tiny text protocol for discovery and call signaling: `LC1|TYPE|senderId|senderName`.
 */
data class ControlMessage(
    val type: MessageType,
    val senderId: String,
    val senderName: String,
) {
    fun encode(): ByteArray =
        "$MAGIC$SEP${type.name}$SEP$senderId$SEP${senderName.replace(SEP, ' ')}"
            .toByteArray(Charsets.UTF_8)

    companion object {
        private const val MAGIC = "LC1"
        private const val SEP = '|'

        fun decode(bytes: ByteArray, length: Int): ControlMessage? {
            val parts = String(bytes, 0, length, Charsets.UTF_8).split(SEP, limit = 4)
            if (parts.size != 4 || parts[0] != MAGIC) return null
            val type = MessageType.entries.firstOrNull { it.name == parts[1] } ?: return null
            return ControlMessage(type, senderId = parts[2], senderName = parts[3])
        }
    }
}
