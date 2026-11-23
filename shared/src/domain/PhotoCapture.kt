package com.zerotwodev.recall.domain

sealed interface PhotoPickResult {
    /** Saved in the app's media folder as a JPEG, at most 2048 px on its long side. */
    data class Saved(val fileName: String) : PhotoPickResult
    data object Cancelled : PhotoPickResult
    data object Failed : PhotoPickResult
}

interface PhotoPicker {
    suspend fun pick(): PhotoPickResult

    /** Removes a saved photo whose memory could not be created. */
    suspend fun delete(fileName: String)
}
