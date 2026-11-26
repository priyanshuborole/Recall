package com.zerotwodev.recall.domain

/** Locks the app behind the phone's own fingerprint, face or PIN. Recall never stores a PIN of its own. */
interface AppLock {
    /** False when the phone has no screen lock, so there is nothing to unlock with. */
    fun canLock(): Boolean

    /** Shows the system prompt. Returns true once the person is confirmed. */
    suspend fun unlock(): Boolean
}
