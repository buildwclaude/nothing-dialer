package com.buildwclaude.dialer.domain

enum class CallDirection { INCOMING, OUTGOING, MISSED, REJECTED, BLOCKED, VOICEMAIL }

data class RecentCall(
    val id: Long,
    val number: String,
    val name: String?,
    val photoUri: String?,
    val direction: CallDirection,
    val date: Long,
    val durationSec: Int,
) {
    val display: String get() = name?.takeIf { it.isNotBlank() } ?: number.ifBlank { "Unknown" }
}

data class Contact(
    val id: Long,
    val name: String,
    val number: String,
    val photoUri: String?,
    val starred: Boolean,
    val lookupKey: String? = null,
) {
    /** First letter for the A–Z index; non-letters bucket under '#'. */
    val sortLetter: Char
        get() = name.trim().firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' } ?: '#'
}
